-- ==========================================
-- Database Performance Optimization
-- Gasolinera JSM - World-Class Performance
-- ==========================================
-- PostgreSQL Configuration Optimizations
-- Add these to postgresql.conf for production
-- Memory Settings
-- shared_buffers = 2GB                    -- 25% of total RAM
-- effective_cache_size = 6GB              -- 75% of total RAM
-- work_mem = 64MB                         -- Per operation memory
-- maintenance_work_mem = 512MB            -- Maintenance operations
-- wal_buffers = 64MB                      -- WAL buffer size
-- Connection Settings
-- max_connections = 200                   -- Maximum connections
-- superuser_reserved_connections = 3      -- Reserved for superuser
-- Query Planning
-- random_page_cost = 1.1                  -- SSD optimization
-- seq_page_cost = 1.0                     -- Sequential scan cost
-- cpu_tuple_cost = 0.01                   -- CPU processing cost
-- cpu_index_tuple_cost = 0.005            -- Index processing cost
-- cpu_operator_cost = 0.0025              -- Operator processing cost
-- Write-Ahead Logging (WAL)
-- wal_level = replica                     -- Replication support
-- max_wal_size = 4GB                     -- Maximum WAL size
-- min_wal_size = 1GB                     -- Minimum WAL size
-- checkpoint_completion_target = 0.9      -- Checkpoint spread
-- wal_compression = on                    -- Compress WAL records
-- Background Writer
-- bgwriter_delay = 200ms                  -- Background writer delay
-- bgwriter_lru_maxpages = 100            -- Pages to write per round
-- bgwriter_lru_multiplier = 2.0          -- Multiplier for next round
-- Autovacuum
-- autovacuum = on                         -- Enable autovacuum
-- autovacuum_max_workers = 3              -- Number of workers
-- autovacuum_naptime = 1min               -- Sleep between runs
-- autovacuum_vacuum_threshold = 50        -- Minimum updates before vacuum
-- autovacuum_analyze_threshold = 50       -- Minimum updates before analyze
-- Statistics
-- track_activities = on                   -- Track running commands
-- track_counts = on                       -- Track table/index access
-- track_io_timing = on                    -- Track I/O timing
-- track_functions = all                   -- Track function calls
-- ==========================================
-- Index Optimization Queries
-- ==========================================
-- Coupons table indexes (high-performance)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_user_status_active ON coupons(user_id, status)
WHERE status IN ('ACTIVE', 'PARTIALLY_REDEEMED');
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_station_created ON coupons(station_id, created_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_qr_code_hash ON coupons USING hash(qr_code);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_valid_dates ON coupons(valid_from, valid_until)
WHERE status = 'ACTIVE';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_amount_fuel_type ON coupons(amount, fuel_type)
WHERE status = 'ACTIVE';
-- Redemptions table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_coupon_id ON redemptions(coupon_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_user_station_date ON redemptions(user_id, station_id, redeemed_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_location_gist ON redemptions USING GIST(location);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_created_at_brin ON redemptions USING BRIN(created_at);
-- Users table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_active ON users(email)
WHERE is_active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_phone_verified ON users(phone)
WHERE is_phone_verified = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_at ON users(created_at DESC);
-- Stations table indexes (geospatial optimization)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_location_gist ON stations USING GIST(location)
WHERE status = 'ACTIVE';
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_brand_status ON stations(brand, status);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_rating ON stations(rating DESC)
WHERE status = 'ACTIVE'
  AND rating >= 4.0;
-- Raffle tickets table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_user_raffle ON raffle_tickets(user_id, raffle_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_raffle_generated ON raffle_tickets(raffle_id, generated_at DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_source_redemption ON raffle_tickets(source_redemption_id)
WHERE source_redemption_id IS NOT NULL;
-- Raffles table indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffles_status_dates ON raffles(status, start_date, end_date);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffles_draw_date ON raffles(draw_date)
WHERE status IN ('ACTIVE', 'SCHEDULED');
-- ==========================================
-- Partitioning Setup (for large tables)
-- ==========================================
-- Partition redemptions table by month
CREATE TABLE IF NOT EXISTS redemptions_partitioned (LIKE redemptions INCLUDING ALL) PARTITION BY RANGE (redeemed_at);
-- Create monthly partitions for current and next 12 months
DO $$
DECLARE start_date DATE;
end_date DATE;
partition_name TEXT;
BEGIN FOR i IN 0..12 LOOP start_date := DATE_TRUNC('month', CURRENT_DATE) + (i || ' months')::INTERVAL;
end_date := start_date + INTERVAL '1 month';
partition_name := 'redemptions_' || TO_CHAR(start_date, 'YYYY_MM');
EXECUTE format(
  'CREATE TABLE IF NOT EXISTS %I PARTITION OF redemptions_partitioned
                       FOR VALUES FROM (%L) TO (%L)',
  partition_name,
  start_date,
  end_date
);
END LOOP;
END $$;
-- Partition audit logs by month
CREATE TABLE IF NOT EXISTS audit_logs_partitioned (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  table_name VARCHAR(100) NOT NULL,
  operation VARCHAR(10) NOT NULL,
  old_values JSONB,
  new_values JSONB,
  user_id UUID,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);
-- Create audit log partitions
DO $$
DECLARE start_date DATE;
end_date DATE;
partition_name TEXT;
BEGIN FOR i IN 0..12 LOOP start_date := DATE_TRUNC('month', CURRENT_DATE) + (i || ' months')::INTERVAL;
end_date := start_date + INTERVAL '1 month';
partition_name := 'audit_logs_' || TO_CHAR(start_date, 'YYYY_MM');
EXECUTE format(
  'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_logs_partitioned
                       FOR VALUES FROM (%L) TO (%L)',
  partition_name,
  start_date,
  end_date
);
END LOOP;
END $$;
-- ==========================================
-- Query Optimization Views
-- ==========================================
-- High-performance view for active coupons with station info
CREATE OR REPLACE VIEW active_coupons_with_stations AS
SELECT c.id,
  c.user_id,
  c.amount,
  c.fuel_type,
  c.qr_code,
  c.valid_until,
  s.name AS station_name,
  s.address AS station_address,
  s.location AS station_location
FROM coupons c
  JOIN stations s ON c.station_id = s.id
WHERE c.status = 'ACTIVE'
  AND c.valid_until > CURRENT_TIMESTAMP
  AND s.status = 'ACTIVE';
-- User statistics view (materialized for performance)
CREATE MATERIALIZED VIEW IF NOT EXISTS user_statistics AS
SELECT u.id AS user_id,
  u.email,
  COUNT(c.id) AS total_coupons,
  COUNT(
    CASE
      WHEN c.status = 'ACTIVE' THEN 1
    END
  ) AS active_coupons,
  COUNT(r.id) AS total_redemptions,
  COALESCE(SUM(c.amount), 0) AS total_coupon_value,
  COALESCE(
    SUM(
      CASE
        WHEN r.id IS NOT NULL THEN c.amount
      END
    ),
    0
  ) AS redeemed_value,
  COUNT(rt.id) AS total_raffle_tickets,
  MAX(r.redeemed_at) AS last_redemption_date,
  u.created_at AS registration_date
FROM users u
  LEFT JOIN coupons c ON u.id = c.user_id
  LEFT JOIN redemptions r ON c.id = r.coupon_id
  LEFT JOIN raffle_tickets rt ON u.id = rt.user_id
WHERE u.is_active = true
GROUP BY u.id,
  u.email,
  u.created_at;
-- Create unique index on materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_statistics_user_id ON user_statistics(user_id);
-- Refresh materialized view (run periodically)
-- REFRESH MATERIALIZED VIEW CONCURRENTLY user_statistics;
-- Station performance view
CREATE OR REPLACE VIEW station_performance AS
SELECT s.id,
  s.name,
  s.brand,
  s.location,
  COUNT(DISTINCT c.id) AS total_coupons_sold,
  COUNT(DISTINCT r.id) AS total_redemptions,
  COALESCE(AVG(s.rating), 0) AS average_rating,
  COUNT(DISTINCT c.user_id) AS unique_customers,
  COALESCE(SUM(c.amount), 0) AS total_revenue,
  CASE
    WHEN COUNT(c.id) > 0 THEN ROUND((COUNT(r.id)::DECIMAL / COUNT(c.id) * 100), 2)
    ELSE 0
  END AS redemption_rate_percent
FROM stations s
  LEFT JOIN coupons c ON s.id = c.station_id
  LEFT JOIN redemptions r ON c.id = r.coupon_id
WHERE s.status = 'ACTIVE'
GROUP BY s.id,
  s.name,
  s.brand,
  s.location,
  s.rating;
-- ==========================================
-- Performance Monitoring Queries
-- ==========================================
-- Query to find slow queries
CREATE OR REPLACE VIEW slow_queries AS
SELECT query,
  calls,
  total_time,
  mean_time,
  rows,
  100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
WHERE mean_time > 100 -- Queries taking more than 100ms on average
ORDER BY mean_time DESC;
-- Index usage statistics
CREATE OR REPLACE VIEW index_usage_stats AS
SELECT schemaname,
  tablename,
  indexname,
  idx_tup_read,
  idx_tup_fetch,
  idx_scan,
  CASE
    WHEN idx_scan = 0 THEN 'Never Used'
    WHEN idx_scan < 100 THEN 'Rarely Used'
    WHEN idx_scan < 1000 THEN 'Moderately Used'
    ELSE 'Frequently Used'
  END AS usage_level
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;
-- Table size and bloat information
CREATE OR REPLACE VIEW table_sizes AS
SELECT schemaname,
  tablename,
  pg_size_pretty(
    pg_total_relation_size(schemaname || '.' || tablename)
  ) AS total_size,
  pg_size_pretty(pg_relation_size(schemaname || '.' || tablename)) AS table_size,
  pg_size_pretty(
    pg_total_relation_size(schemaname || '.' || tablename) - pg_relation_size(schemaname || '.' || tablename)
  ) AS index_size,
  n_tup_ins AS inserts,
  n_tup_upd AS updates,
  n_tup_del AS deletes,
  n_live_tup AS live_tuples,
  n_dead_tup AS dead_tuples,
  CASE
    WHEN n_live_tup > 0 THEN ROUND((n_dead_tup::DECIMAL / n_live_tup * 100), 2)
    ELSE 0
  END AS bloat_percent
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(schemaname || '.' || tablename) DESC;
-- ==========================================
-- Maintenance Procedures
-- ==========================================
-- Procedure to analyze table statistics
CREATE OR REPLACE FUNCTION analyze_all_tables() RETURNS void AS $$
DECLARE table_record RECORD;
BEGIN FOR table_record IN
SELECT schemaname,
  tablename
FROM pg_tables
WHERE schemaname = 'public' LOOP EXECUTE 'ANALYZE ' || quote_ident(table_record.schemaname) || '.' || quote_ident(table_record.tablename);
RAISE NOTICE 'Analyzed table: %.%',
table_record.schemaname,
table_record.tablename;
END LOOP;
END;
$$ LANGUAGE plpgsql;
-- Procedure to reindex tables with low usage
CREATE OR REPLACE FUNCTION reindex_unused_indexes() RETURNS void AS $$
DECLARE index_record RECORD;
BEGIN FOR index_record IN
SELECT schemaname,
  indexname
FROM pg_stat_user_indexes
WHERE idx_scan < 100 -- Rarely used indexes
  AND schemaname = 'public' LOOP EXECUTE 'REINDEX INDEX ' || quote_ident(index_record.schemaname) || '.' || quote_ident(index_record.indexname);
RAISE NOTICE 'Reindexed: %.%',
index_record.schemaname,
index_record.indexname;
END LOOP;
END;
$$ LANGUAGE plpgsql;
-- ==========================================
-- Performance Monitoring Setup
-- ==========================================
-- Enable pg_stat_statements extension for query monitoring
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
-- Enable auto_explain for slow query logging
-- Add to postgresql.conf:
-- shared_preload_libraries = 'pg_stat_statements,auto_explain'
-- auto_explain.log_min_duration = 1000  -- Log queries > 1 second
-- auto_explain.log_analyze = true
-- auto_explain.log_buffers = true
-- auto_explain.log_timing = true
-- auto_explain.log_triggers = true
-- auto_explain.log_verbose = true
-- Create monitoring user (read-only)
DO $$ BEGIN IF NOT EXISTS (
  SELECT
  FROM pg_catalog.pg_roles
  WHERE rolname = 'monitoring_user'
) THEN CREATE ROLE monitoring_user WITH LOGIN PASSWORD 'secure_monitoring_password';
GRANT CONNECT ON DATABASE gasolinera_jsm TO monitoring_user;
GRANT USAGE ON SCHEMA public TO monitoring_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO monitoring_user;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO monitoring_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT ON TABLES TO monitoring_user;
END IF;
END $$;
-- ==========================================
-- Backup and Recovery Optimization
-- ==========================================
-- Create backup script template
/*
 #!/bin/bash
 # High-performance backup script
 
 BACKUP_DIR="/var/backups/postgresql"
 DB_NAME="gasolinera_jsm"
 TIMESTAMP=$(date +%Y%m%d_%H%M%S)
 
 # Full backup with compression
 pg_dump -h localhost -U postgres -d $DB_NAME \
 --format=custom \
 --compress=9 \
 --verbose \
 --file="$BACKUP_DIR/full_backup_$TIMESTAMP.dump"
 
 # Incremental WAL backup
 pg_receivewal -h localhost -U replication \
 --directory="$BACKUP_DIR/wal" \
 --compress=9 \
 --verbose
 
 # Cleanup old backups (keep last 7 days)
 find $BACKUP_DIR -name "*.dump" -mtime +7 -delete
 find $BACKUP_DIR/wal -name "*.gz" -mtime +7 -delete
 */
-- ==========================================
-- Connection Pooling Configuration
-- ==========================================
-- PgBouncer configuration template
/*
 [databases]
 gasolinera_jsm = host=localhost port=5432 dbname=gasolinera_jsm
 
 [pgbouncer]
 listen_port = 6432
 listen_addr = *
 auth_type = md5
 auth_file = /etc/pgbouncer/userlist.txt
 admin_users = postgres
 stats_users = monitoring_user
 
 pool_mode = transaction
 server_reset_query = DISCARD ALL
 max_client_conn = 1000
 default_pool_size = 20
 min_pool_size = 5
 reserve_pool_size = 5
 reserve_pool_timeout = 5
 max_db_connections = 50
 max_user_connections = 50
 
 server_round_robin = 1
 ignore_startup_parameters = extra_float_digits
 server_check_delay = 30
 server_check_query = select 1
 server_lifetime = 3600
 server_idle_timeout = 600
 */