#!/bin/bash

# Gasolinera JSM Platform Backup and Restore Script
# This script handles backup and restore operations for the platform

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$PROJECT_ROOT/backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
RETENTION_DAYS=30

# Default values
OPERATION=""
BACKUP_TYPE="full"
RESTORE_POINT=""
COMPRESSION=true
ENCRYPTION=false
REMOTE_STORAGE=false
VERBOSE=false

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking backup/restore prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed"
        exit 1
    fi

    # Create backup directory
    mkdir -p "$BACKUP_DIR"

    # Check available disk space
    local available_space=$(df "$BACKUP_DIR" | awk 'NR==2 {print $4}')
    if [ "$available_space" -lt 5242880 ]; then # 5GB in KB
        print_warning "Low disk space for backups. Available: $(($available_space / 1024 / 1024))GB"
    fi

    print_success "Prerequisites check completed"
}

# Function to backup database
backup_database() {
    local backup_file="$1"
    print_status "Backing up PostgreSQL database..."

    # Check if PostgreSQL is running
    if ! docker-compose ps postgres | grep -q "Up"; then
        print_error "PostgreSQL container is not running"
        return 1
    fi

    # Create database backup
    docker-compose exec -T postgres pg_dumpall -U gasolinera_user > "$backup_file"

    if [ $? -eq 0 ]; then
        print_success "Database backup completed: $backup_file"
        return 0
    else
        print_error "Database backup failed"
        return 1
    fi
}

# Function to backup Redis data
backup_redis() {
    local backup_file="$1"
    print_status "Backing up Redis data..."

    # Check if Redis is running
    if ! docker-compose ps redis | grep -q "Up"; then
        print_error "Redis container is not running"
        return 1
    fi

    # Force Redis to save current state
    docker-compose exec -T redis redis-cli BGSAVE
    sleep 5

    # Copy Redis dump file
    docker cp $(docker-compose ps -q redis):/data/dump.rdb "$backup_file"

    if [ $? -eq 0 ]; then
        print_success "Redis backup completed: $backup_file"
        return 0
    else
        print_error "Redis backup failed"
        return 1
    fi
}

# Function to backup configuration files
backup_configuration() {
    local backup_dir="$1"
    print_status "Backing up configuration files..."

    # Create configuration backup directory
    mkdir -p "$backup_dir/config"

    # Backup configuration files
    cp -r "$PROJECT_ROOT/config/" "$backup_dir/"
    cp "$PROJECT_ROOT"/.env.* "$backup_dir/" 2>/dev/null || true
    cp "$PROJECT_ROOT"/docker-compose*.yml "$backup_dir/"

    # Backup scripts
    cp -r "$PROJECT_ROOT/scripts/" "$backup_dir/"

    # Create backup manifest
    cat > "$backup_dir/backup_manifest.json" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "type": "$BACKUP_TYPE",
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')",
  "git_branch": "$(git branch --show-current 2>/dev/null || echo 'unknown')",
  "docker_images": [
$(docker images --format '    "{{.Repository}}:{{.Tag}}",' | grep gasolinera | sed '$ s/,$//')
  ],
  "services_status": [
$(docker-compose ps --format json 2>/dev/null | jq -c . | sed 's/^/    /' | sed '$ ! s/$/,/')
  ]
}
EOF

    print_success "Configuration backup completed"
}

# Function to backup Docker volumes
backup_volumes() {
    local backup_dir="$1"
    print_status "Backing up Docker volumes..."

    # Create volumes backup directory
    mkdir -p "$backup_dir/volumes"

    # Get list of project volumes
    local volumes=$(docker volume ls --filter "name=$(basename $PROJECT_ROOT)" --format "{{.Name}}")

    for volume in $volumes; do
        print_status "Backing up volume: $volume"

        # Create a temporary container to access the volume
        docker run --rm -v "$volume:/source" -v "$backup_dir/volumes:/backup" \
            alpine tar czf "/backup/${volume}.tar.gz" -C /source .

        if [ $? -eq 0 ]; then
            print_success "Volume backup completed: $volume"
        else
            print_warning "Volume backup failed: $volume"
        fi
    done
}

# Function to create full backup
create_full_backup() {
    local backup_name="full_backup_$TIMESTAMP"
    local backup_path="$BACKUP_DIR/$backup_name"

    print_status "Creating full backup: $backup_name"

    # Create backup directory
    mkdir -p "$backup_path"

    # Backup database
    backup_database "$backup_path/database.sql"

    # Backup Redis
    backup_redis "$backup_path/redis.rdb"

    # Backup configuration
    backup_configuration "$backup_path"

    # Backup volumes
    backup_volumes "$backup_path"

    # Compress backup if requested
    if [ "$COMPRESSION" = true ]; then
        print_status "Compressing backup..."
        cd "$BACKUP_DIR"
        tar czf "${backup_name}.tar.gz" "$backup_name"
        rm -rf "$backup_name"
        backup_path="${backup_path}.tar.gz"
        print_success "Backup compressed: ${backup_name}.tar.gz"
    fi

    # Encrypt backup if requested
    if [ "$ENCRYPTION" = true ]; then
        print_status "Encrypting backup..."
        gpg --symmetric --cipher-algo AES256 "$backup_path"
        rm "$backup_path"
        backup_path="${backup_path}.gpg"
        print_success "Backup encrypted: $(basename $backup_path)"
    fi

    # Upload to remote storage if configured
    if [ "$REMOTE_STORAGE" = true ]; then
        upload_to_remote_storage "$backup_path"
    fi

    print_success "Full backup completed: $(basename $backup_path)"
    echo "Backup location: $backup_path"
}

# Function to create incremental backup
create_incremental_backup() {
    local backup_name="incremental_backup_$TIMESTAMP"
    local backup_path="$BACKUP_DIR/$backup_name"

    print_status "Creating incremental backup: $backup_name"

    # Find the last full backup
    local last_full_backup=$(ls -t "$BACKUP_DIR"/full_backup_* 2>/dev/null | head -n1)

    if [ -z "$last_full_backup" ]; then
        print_warning "No full backup found. Creating full backup instead."
        create_full_backup
        return
    fi

    print_status "Base backup: $(basename $last_full_backup)"

    # Create backup directory
    mkdir -p "$backup_path"

    # Backup only changed data (simplified approach)
    backup_database "$backup_path/database.sql"
    backup_configuration "$backup_path"

    # Create incremental manifest
    cat > "$backup_path/incremental_manifest.json" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "type": "incremental",
  "base_backup": "$(basename $last_full_backup)",
  "git_commit": "$(git rev-parse HEAD 2>/dev/null || echo 'unknown')"
}
EOF

    print_success "Incremental backup completed: $backup_name"
}

# Function to list available backups
list_backups() {
    print_status "Available backups:"

    if [ ! -d "$BACKUP_DIR" ] || [ -z "$(ls -A $BACKUP_DIR 2>/dev/null)" ]; then
        print_warning "No backups found"
        return
    fi

    echo ""
    printf "%-30s %-15s %-20s %s\n" "BACKUP NAME" "TYPE" "DATE" "SIZE"
    printf "%-30s %-15s %-20s %s\n" "$(printf '%*s' 30 '' | tr ' ' '-')" "$(printf '%*s' 15 '' | tr ' ' '-')" "$(printf '%*s' 20 '' | tr ' ' '-')" "$(printf '%*s' 10 '' | tr ' ' '-')"

    for backup in "$BACKUP_DIR"/*; do
        if [ -f "$backup" ] || [ -d "$backup" ]; then
            local backup_name=$(basename "$backup")
            local backup_type="unknown"
            local backup_date="unknown"
            local backup_size="unknown"

            # Determine backup type from name
            if [[ "$backup_name" == full_backup_* ]]; then
                backup_type="full"
            elif [[ "$backup_name" == incremental_backup_* ]]; then
                backup_type="incremental"
            fi

            # Extract date from backup name
            if [[ "$backup_name" =~ _([0-9]{8}_[0-9]{6}) ]]; then
                local timestamp="${BASH_REMATCH[1]}"
                backup_date=$(date -d "${timestamp:0:8} ${timestamp:9:2}:${timestamp:11:2}:${timestamp:13:2}" "+%Y-%m-%d %H:%M" 2>/dev/null || echo "$timestamp")
            fi

            # Get backup size
            if [ -f "$backup" ]; then
                backup_size=$(du -h "$backup" | cut -f1)
            elif [ -d "$backup" ]; then
                backup_size=$(du -sh "$backup" | cut -f1)
            fi

            printf "%-30s %-15s %-20s %s\n" "$backup_name" "$backup_type" "$backup_date" "$backup_size"
        fi
    done
    echo ""
}

# Function to restore from backup
restore_from_backup() {
    local restore_point="$1"
    local backup_path="$BACKUP_DIR/$restore_point"

    print_status "Restoring from backup: $restore_point"

    # Check if backup exists
    if [ ! -f "$backup_path" ] && [ ! -d "$backup_path" ]; then
        # Try with common extensions
        if [ -f "${backup_path}.tar.gz" ]; then
            backup_path="${backup_path}.tar.gz"
        elif [ -f "${backup_path}.gpg" ]; then
            backup_path="${backup_path}.gpg"
        else
            print_error "Backup not found: $restore_point"
            return 1
        fi
    fi

    print_warning "This will stop all services and restore data. Continue? (y/N)"
    read -r confirmation
    if [[ ! "$confirmation" =~ ^[Yy]$ ]]; then
        print_status "Restore cancelled"
        return 0
    fi

    # Stop services
    print_status "Stopping services..."
    cd "$PROJECT_ROOT"
    ./scripts/docker-stop.sh

    # Prepare restore directory
    local restore_dir="/tmp/gasolinera_restore_$TIMESTAMP"
    mkdir -p "$restore_dir"

    # Decrypt if needed
    if [[ "$backup_path" == *.gpg ]]; then
        print_status "Decrypting backup..."
        gpg --decrypt "$backup_path" > "${backup_path%.gpg}"
        backup_path="${backup_path%.gpg}"
        print_success "Backup decrypted"
    fi

    # Extract if compressed
    if [[ "$backup_path" == *.tar.gz ]]; then
        print_status "Extracting backup..."
        cd "$(dirname "$backup_path")"
        tar -xzf "$(basename "$backup_path")" -C "$restore_dir"
        backup_path="$restore_dir/$(basename "${backup_path%.tar.gz}")"
        print_success "Backup extracted"
    elif [ -d "$backup_path" ]; then
        cp -r "$backup_path"/* "$restore_dir/"
        backup_path="$restore_dir"
    else
        print_error "Invalid backup format"
        return 1
    fi

    # Restore configuration files
    if [ -d "$backup_path/config" ]; then
        print_status "Restoring configuration files..."
        cp -r "$backup_path/config/" "$PROJECT_ROOT/"
        cp "$backup_path"/.env.* "$PROJECT_ROOT/" 2>/dev/null || true
        cp "$backup_path"/docker-compose*.yml "$PROJECT_ROOT/" 2>/dev/null || true
        print_success "Configuration files restored"
    fi

    # Start infrastructure services
    print_status "Starting infrastructure services..."
    cd "$PROJECT_ROOT"
    docker-compose up -d postgres redis rabbitmq
    sleep 30

    # Restore database
    if [ -f "$backup_path/database.sql" ]; then
        print_status "Restoring database..."
        docker-compose exec -T postgres psql -U gasolinera_user -d postgres -c "DROP DATABASE IF EXISTS gasolinera_db;"
        docker-compose exec -T postgres psql -U gasolinera_user -d postgres -c "CREATE DATABASE gasolinera_db;"
        docker-compose exec -T postgres psql -U gasolinera_user -d gasolinera_db < "$backup_path/database.sql"
        print_success "Database restored"
    fi

    # Restore Redis data
    if [ -f "$backup_path/redis.rdb" ]; then
        print_status "Restoring Redis data..."
        docker-compose stop redis
        docker cp "$backup_path/redis.rdb" $(docker-compose ps -q redis):/data/dump.rdb
        docker-compose start redis
        print_success "Redis data restored"
    fi

    # Restore Docker volumes
    if [ -d "$backup_path/volumes" ]; then
        print_status "Restoring Docker volumes..."
        for volume_backup in "$backup_path/volumes"/*.tar.gz; do
            if [ -f "$volume_backup" ]; then
                local volume_name=$(basename "$volume_backup" .tar.gz)
                print_status "Restoring volume: $volume_name"

                # Create volume if it doesn't exist
                docker volume create "$volume_name" 2>/dev/null || true

                # Restore volume data
                docker run --rm -v "$volume_name:/target" -v "$backup_path/volumes:/backup" \
                    alpine tar xzf "/backup/$(basename "$volume_backup")" -C /target
            fi
        done
        print_success "Docker volumes restored"
    fi

    # Start application services
    print_status "Starting application services..."
    ./scripts/docker-start.sh --dev

    # Cleanup restore directory
    rm -rf "$restore_dir"

    print_success "Restore completed successfully"
}

# Function to upload backup to remote storage
upload_to_remote_storage() {
    local backup_file="$1"
    print_status "Uploading backup to remote storage..."

    # This is a placeholder for remote storage integration
    # Implement based on your cloud provider (AWS S3, Google Cloud, etc.)

    # Example for AWS S3:
    # aws s3 cp "$backup_file" "s3://your-backup-bucket/gasolinera-backups/"

    # Example for rsync to remote server:
    # rsync -avz "$backup_file" user@backup-server:/path/to/backups/

    print_warning "Remote storage upload not configured. Implement based on your requirements."
}

# Function to cleanup old backups
cleanup_old_backups() {
    print_status "Cleaning up old backups (retention: $RETENTION_DAYS days)..."

    if [ ! -d "$BACKUP_DIR" ]; then
        print_warning "Backup directory does not exist"
        return
    fi

    # Find and remove backups older than retention period
    find "$BACKUP_DIR" -name "*backup_*" -type f -mtime +$RETENTION_DAYS -delete
    find "$BACKUP_DIR" -name "*backup_*" -type d -mtime +$RETENTION_DAYS -exec rm -rf {} + 2>/dev/null || true

    print_success "Old backups cleaned up"
}

# Function to verify backup integrity
verify_backup() {
    local backup_path="$1"
    print_status "Verifying backup integrity: $(basename $backup_path)"

    if [ ! -f "$backup_path" ] && [ ! -d "$backup_path" ]; then
        print_error "Backup not found: $backup_path"
        return 1
    fi

    # Check if backup is compressed
    if [[ "$backup_path" == *.tar.gz ]]; then
        if tar -tzf "$backup_path" >/dev/null 2>&1; then
            print_success "Compressed backup integrity verified"
        else
            print_error "Compressed backup is corrupted"
            return 1
        fi
    fi

    # Check if backup is encrypted
    if [[ "$backup_path" == *.gpg ]]; then
        if gpg --list-packets "$backup_path" >/dev/null 2>&1; then
            print_success "Encrypted backup integrity verified"
        else
            print_error "Encrypted backup is corrupted"
            return 1
        fi
    fi

    # For directory backups, check if manifest exists
    if [ -d "$backup_path" ]; then
        if [ -f "$backup_path/backup_manifest.json" ]; then
            print_success "Directory backup integrity verified"
        else
            print_warning "Directory backup missing manifest file"
        fi
    fi

    return 0
}

# Function to schedule automated backups
schedule_backup() {
    local schedule="$1"
    local backup_type="$2"

    print_status "Setting up automated backup schedule..."

    # Create backup script for cron
    local cron_script="$PROJECT_ROOT/scripts/automated-backup.sh"
    cat > "$cron_script" << EOF
#!/bin/bash
cd "$PROJECT_ROOT"
./scripts/backup-restore.sh --operation backup --type $backup_type --compression --cleanup
EOF

    chmod +x "$cron_script"

    # Add to crontab
    (crontab -l 2>/dev/null; echo "$schedule $cron_script") | crontab -

    print_success "Automated backup scheduled: $schedule"
    print_status "Backup type: $backup_type"
    print_status "Script location: $cron_script"
}

# Function to show usage
show_usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Gasolinera JSM Platform Backup and Restore Script

Operations:
  --operation backup           Create a backup
  --operation restore          Restore from backup
  --operation list             List available backups
  --operation verify           Verify backup integrity
  --operation cleanup          Clean up old backups
  --operation schedule         Schedule automated backups

Backup Options:
  --type full                  Create full backup (default)
  --type incremental           Create incremental backup
  --compression                Enable compression
  --encryption                 Enable encryption
  --remote                     Upload to remote storage

Restore Options:
  --restore-point NAME         Specify backup to restore from

General Options:
  --retention-days DAYS        Backup retention period (default: 30)
  --verbose                    Enable verbose output
  --help                       Show this help message

Examples:
  $0 --operation backup --type full --compression
  $0 --operation backup --type incremental
  $0 --operation restore --restore-point full_backup_20240101_120000
  $0 --operation list
  $0 --operation verify --restore-point full_backup_20240101_120000
  $0 --operation cleanup
  $0 --operation schedule --schedule "0 2 * * *" --type full

Schedule Format (for --operation schedule):
  --schedule "CRON_EXPRESSION"  Cron expression for scheduling
  Examples:
    "0 2 * * *"                Daily at 2 AM
    "0 2 * * 0"                Weekly on Sunday at 2 AM
    "0 2 1 * *"                Monthly on 1st at 2 AM

Environment Variables:
  BACKUP_ENCRYPTION_KEY        GPG encryption key ID
  REMOTE_STORAGE_URL           Remote storage URL
  BACKUP_RETENTION_DAYS        Override retention period

EOF
}

# Main function
main() {
    print_status "Gasolinera JSM Platform Backup/Restore Tool"
    print_status "Timestamp: $(date)"

    case "$OPERATION" in
        "backup")
            check_prerequisites
            case "$BACKUP_TYPE" in
                "full")
                    create_full_backup
                    ;;
                "incremental")
                    create_incremental_backup
                    ;;
                *)
                    print_error "Invalid backup type: $BACKUP_TYPE"
                    exit 1
                    ;;
            esac

            if [ "$CLEANUP" = true ]; then
                cleanup_old_backups
            fi
            ;;
        "restore")
            if [ -z "$RESTORE_POINT" ]; then
                print_error "Restore point must be specified with --restore-point"
                exit 1
            fi
            check_prerequisites
            restore_from_backup "$RESTORE_POINT"
            ;;
        "list")
            list_backups
            ;;
        "verify")
            if [ -z "$RESTORE_POINT" ]; then
                print_error "Backup to verify must be specified with --restore-point"
                exit 1
            fi
            verify_backup "$BACKUP_DIR/$RESTORE_POINT"
            ;;
        "cleanup")
            cleanup_old_backups
            ;;
        "schedule")
            if [ -z "$SCHEDULE" ]; then
                print_error "Schedule must be specified with --schedule"
                exit 1
            fi
            schedule_backup "$SCHEDULE" "$BACKUP_TYPE"
            ;;
        "")
            print_error "Operation must be specified"
            show_usage
            exit 1
            ;;
        *)
            print_error "Invalid operation: $OPERATION"
            show_usage
            exit 1
            ;;
    esac
}

# Parse command line arguments
CLEANUP=false
SCHEDULE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --operation)
            OPERATION="$2"
            shift 2
            ;;
        --type)
            BACKUP_TYPE="$2"
            shift 2
            ;;
        --restore-point)
            RESTORE_POINT="$2"
            shift 2
            ;;
        --retention-days)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        --schedule)
            SCHEDULE="$2"
            shift 2
            ;;
        --compression)
            COMPRESSION=true
            shift
            ;;
        --encryption)
            ENCRYPTION=true
            shift
            ;;
        --remote)
            REMOTE_STORAGE=true
            shift
            ;;
        --cleanup)
            CLEANUP=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Override with environment variables if set
RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-$RETENTION_DAYS}

# Execute main function
main "$@"