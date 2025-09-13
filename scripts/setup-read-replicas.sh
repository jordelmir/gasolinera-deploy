#!/bin/bash

# Script para configurar read replicas de PostgreSQL
# Uso: ./setup-read-replicas.sh [primary_host] [replica_host] [database_name]

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuración por defecto
PRIMARY_HOST=${1:-"localhost"}
REPLICA_HOST=${2:-"localhost"}
DATABASE_NAME=${3:-"gasolinera_jsm"}
POSTGRES_USER=${POSTGRES_USER:-"postgres"}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-"postgres"}
REPLICATION_USER=${REPLICATION_USER:-"replicator"}
REPLICATION_PASSWORD=${REPLICATION_PASSWORD:-"replicator_pass"}

echo -e "${BLUE}🔧 Configurando Read Replicas de PostgreSQL${NC}"
echo -e "${BLUE}Primary Host: ${PRIMARY_HOST}${NC}"
echo -e "${BLUE}Replica Host: ${REPLICA_HOST}${NC}"
echo -e "${BLUE}Database: ${DATABASE_NAME}${NC}"
echo ""

# Función para logging
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

error() {
    echo -e "${RED}[ERROR] $1${NC}"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING] $1${NC}"
}

# Verificar dependencias
check_dependencies() {
    log "Verificando dependencias..."

    if ! command -v psql &> /dev/null; then
        error "psql no está instalado. Por favor instala PostgreSQL client."
    fi

    if ! command -v pg_basebackup &> /dev/null; then
        error "pg_basebackup no está disponible. Por favor instala PostgreSQL."
    fi

    log "✅ Dependencias verificadas"
}

# Configurar servidor primario
configure_primary() {
    log "Configurando servidor primario..."

    # Crear usuario de replicación
    log "Creando usuario de replicación..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $PRIMARY_HOST -U $POSTGRES_USER -d postgres -c "
        DO \$\$
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '$REPLICATION_USER') THEN
                CREATE ROLE $REPLICATION_USER WITH REPLICATION LOGIN PASSWORD '$REPLICATION_PASSWORD';
                GRANT CONNECT ON DATABASE $DATABASE_NAME TO $REPLICATION_USER;
            END IF;
        END
        \$\$;
    " || error "Error creando usuario de replicación"

    # Configurar postgresql.conf
    log "Configurando postgresql.conf..."
    cat > /tmp/postgresql_replica.conf << EOF
# Configuración para Read Replicas
wal_level = replica
max_wal_senders = 10
max_replication_slots = 10
synchronous_commit = off
archive_mode = on
archive_command = 'test ! -f /var/lib/postgresql/archive/%f && cp %p /var/lib/postgresql/archive/%f'

# Performance optimizations
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
checkpoint_completion_target = 0.9
wal_buffers = 16MB
default_statistics_target = 100
random_page_cost = 1.1
effective_io_concurrency = 200

# Connection settings
max_connections = 200
listen_addresses = '*'

# Logging
log_destination = 'stderr'
logging_collector = on
log_directory = 'log'
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
log_rotation_age = 1d
log_rotation_size = 100MB
log_min_duration_statement = 1000
log_checkpoints = on
log_connections = on
log_disconnections = on
log_lock_waits = on

# Monitoring
track_activities = on
track_counts = on
track_io_timing = on
track_functions = all
EOF

    warning "⚠️  Configuración de postgresql.conf generada en /tmp/postgresql_replica.conf"
    warning "⚠️  Por favor, aplica esta configuración manualmente al servidor primario"

    # Configurar pg_hba.conf
    log "Generando configuración de pg_hba.conf..."
    cat > /tmp/pg_hba_replica.conf << EOF
# Configuración para replicación
# TYPE  DATABASE        USER            ADDRESS                 METHOD
host    replication     $REPLICATION_USER    $REPLICA_HOST/32         md5
host    replication     $REPLICATION_USER    samenet                  md5
host    $DATABASE_NAME  $REPLICATION_USER    $REPLICA_HOST/32         md5
EOF

    warning "⚠️  Configuración de pg_hba.conf generada en /tmp/pg_hba_replica.conf"
    warning "⚠️  Por favor, añade estas líneas al archivo pg_hba.conf del servidor primario"

    log "✅ Configuración del servidor primario completada"
}

# Crear backup base para replica
create_base_backup() {
    log "Creando backup base para replica..."

    local backup_dir="/tmp/postgres_backup_$(date +%Y%m%d_%H%M%S)"
    mkdir -p $backup_dir

    log "Ejecutando pg_basebackup..."
    PGPASSWORD=$REPLICATION_PASSWORD pg_basebackup \
        -h $PRIMARY_HOST \
        -D $backup_dir \
        -U $REPLICATION_USER \
        -v \
        -P \
        -W \
        -R || error "Error ejecutando pg_basebackup"

    log "✅ Backup base creado en: $backup_dir"
    echo "BACKUP_DIR=$backup_dir" > /tmp/replica_backup_path.env
}

# Configurar servidor replica
configure_replica() {
    log "Configurando servidor replica..."

    # Leer directorio de backup
    if [ -f /tmp/replica_backup_path.env ]; then
        source /tmp/replica_backup_path.env
    else
        error "No se encontró el directorio de backup. Ejecuta create_base_backup primero."
    fi

    # Configurar recovery.conf (PostgreSQL < 12) o postgresql.conf (PostgreSQL >= 12)
    log "Configurando recovery settings..."
    cat > /tmp/recovery_replica.conf << EOF
# Configuración de recovery para replica
standby_mode = 'on'
primary_conninfo = 'host=$PRIMARY_HOST port=5432 user=$REPLICATION_USER password=$REPLICATION_PASSWORD application_name=replica_$(hostname)'
recovery_target_timeline = 'latest'
hot_standby = on
max_standby_streaming_delay = 30s
max_standby_archive_delay = 60s
hot_standby_feedback = on
EOF

    warning "⚠️  Configuración de recovery generada en /tmp/recovery_replica.conf"
    warning "⚠️  Para PostgreSQL >= 12, añade estas configuraciones a postgresql.conf"
    warning "⚠️  Para PostgreSQL < 12, crea un archivo recovery.conf con este contenido"

    log "✅ Configuración del servidor replica completada"
}

# Verificar replicación
verify_replication() {
    log "Verificando estado de replicación..."

    # Verificar en el servidor primario
    log "Verificando replication slots en el servidor primario..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $PRIMARY_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
        SELECT slot_name, active, restart_lsn, confirmed_flush_lsn
        FROM pg_replication_slots;
    " || warning "No se pudo verificar replication slots"

    # Verificar WAL senders
    log "Verificando WAL senders..."
    PGPASSWORD=$POSTGRES_PASSWORD psql -h $PRIMARY_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
        SELECT pid, usename, application_name, client_addr, state, sync_state
        FROM pg_stat_replication;
    " || warning "No se pudo verificar WAL senders"

    log "✅ Verificación de replicación completada"
}

# Crear script de monitoreo
create_monitoring_script() {
    log "Creando script de monitoreo..."

    cat > /tmp/monitor_replication.sh << 'EOF'
#!/bin/bash

# Script de monitoreo de replicación
# Uso: ./monitor_replication.sh [primary_host] [replica_host]

PRIMARY_HOST=${1:-"localhost"}
REPLICA_HOST=${2:-"localhost"}
DATABASE_NAME=${3:-"gasolinera_jsm"}
POSTGRES_USER=${POSTGRES_USER:-"postgres"}
POSTGRES_PASSWORD=${POSTGRES_PASSWORD:-"postgres"}

echo "=== Monitoreo de Replicación PostgreSQL ==="
echo "Primary: $PRIMARY_HOST"
echo "Replica: $REPLICA_HOST"
echo "Database: $DATABASE_NAME"
echo ""

# Verificar lag de replicación
echo "--- Lag de Replicación ---"
PGPASSWORD=$POSTGRES_PASSWORD psql -h $PRIMARY_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
    SELECT
        client_addr,
        application_name,
        state,
        sync_state,
        pg_wal_lsn_diff(pg_current_wal_lsn(), flush_lsn) AS flush_lag_bytes,
        pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) AS replay_lag_bytes
    FROM pg_stat_replication;
"

# Verificar estado de la replica
echo ""
echo "--- Estado de la Replica ---"
PGPASSWORD=$POSTGRES_PASSWORD psql -h $REPLICA_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
    SELECT
        pg_is_in_recovery() AS is_replica,
        pg_last_wal_receive_lsn() AS last_received,
        pg_last_wal_replay_lsn() AS last_replayed,
        pg_wal_lsn_diff(pg_last_wal_receive_lsn(), pg_last_wal_replay_lsn()) AS replay_lag_bytes;
" 2>/dev/null || echo "No se pudo conectar a la replica o no está configurada"

# Verificar conexiones
echo ""
echo "--- Conexiones Activas ---"
echo "Primary:"
PGPASSWORD=$POSTGRES_PASSWORD psql -h $PRIMARY_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
    SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';
"

echo "Replica:"
PGPASSWORD=$POSTGRES_PASSWORD psql -h $REPLICA_HOST -U $POSTGRES_USER -d $DATABASE_NAME -c "
    SELECT count(*) as active_connections FROM pg_stat_activity WHERE state = 'active';
" 2>/dev/null || echo "No disponible"

echo ""
echo "=== Fin del Monitoreo ==="
EOF

    chmod +x /tmp/monitor_replication.sh
    log "✅ Script de monitoreo creado en: /tmp/monitor_replication.sh"
}

# Crear script de failover
create_failover_script() {
    log "Creando script de failover..."

    cat > /tmp/failover_replica.sh << 'EOF'
#!/bin/bash

# Script de failover para promover replica a primario
# ⚠️  USAR SOLO EN EMERGENCIAS ⚠️
# Uso: ./failover_replica.sh [replica_host]

REPLICA_HOST=${1:-"localhost"}
DATABASE_NAME=${2:-"gasolinera_jsm"}
POSTGRES_USER=${POSTGRES_USER:-"postgres"}

echo "⚠️  ADVERTENCIA: Este script promoverá la replica a servidor primario"
echo "⚠️  Esto debe hacerse solo si el servidor primario está completamente inaccesible"
echo ""
read -p "¿Estás seguro de que quieres continuar? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Operación cancelada"
    exit 1
fi

echo "Promoviendo replica a primario..."

# Promover replica
pg_ctl promote -D /var/lib/postgresql/data || {
    echo "Error promoviendo replica. Intenta manualmente:"
    echo "SELECT pg_promote();"
    exit 1
}

echo "✅ Replica promovida exitosamente"
echo ""
echo "Pasos siguientes:"
echo "1. Actualizar configuración de aplicaciones para apuntar al nuevo primario"
echo "2. Configurar nuevo servidor replica si es necesario"
echo "3. Verificar que todas las aplicaciones funcionen correctamente"
EOF

    chmod +x /tmp/failover_replica.sh
    warning "⚠️  Script de failover creado en: /tmp/failover_replica.sh"
    warning "⚠️  USAR SOLO EN EMERGENCIAS"
}

# Función principal
main() {
    echo -e "${BLUE}Iniciando configuración de Read Replicas...${NC}"

    check_dependencies

    case "${4:-all}" in
        "primary")
            configure_primary
            ;;
        "backup")
            create_base_backup
            ;;
        "replica")
            configure_replica
            ;;
        "verify")
            verify_replication
            ;;
        "monitor")
            create_monitoring_script
            ;;
        "failover")
            create_failover_script
            ;;
        "all")
            configure_primary
            create_base_backup
            configure_replica
            verify_replication
            create_monitoring_script
            create_failover_script
            ;;
        *)
            echo "Uso: $0 [primary_host] [replica_host] [database_name] [step]"
            echo "Steps: primary, backup, replica, verify, monitor, failover, all"
            exit 1
            ;;
    esac

    echo ""
    log "🎉 Configuración completada!"
    echo ""
    echo -e "${YELLOW}📋 Próximos pasos:${NC}"
    echo "1. Aplicar configuraciones generadas a los servidores PostgreSQL"
    echo "2. Reiniciar el servidor primario para aplicar cambios"
    echo "3. Configurar el servidor replica con el backup creado"
    echo "4. Verificar replicación con el script de monitoreo"
    echo "5. Actualizar configuración de la aplicación para usar read replicas"
    echo ""
    echo -e "${BLUE}📁 Archivos generados:${NC}"
    echo "- /tmp/postgresql_replica.conf (configuración primario)"
    echo "- /tmp/pg_hba_replica.conf (configuración acceso)"
    echo "- /tmp/recovery_replica.conf (configuración replica)"
    echo "- /tmp/monitor_replication.sh (script monitoreo)"
    echo "- /tmp/failover_replica.sh (script failover)"
    echo ""
    echo -e "${GREEN}✅ Setup de Read Replicas completado!${NC}"
}

# Ejecutar función principal
main "$@"