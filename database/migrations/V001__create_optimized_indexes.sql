-- Gasolinera JSM - Optimized Database Indexes
-- Version: 1.0
-- Description: Creates optimized indexes for all main tables

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- =====================================================
-- USERS TABLE INDEXES
-- =====================================================

-- Primary authentication indexes
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_unique
ON users(email)
WHERE email IS NOT NULL;

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_users_phone_unique
ON users(phone_number)
WHERE phone_number IS NOT NULL;

-- User status and activity indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_status_created
ON users(status, created_at)
WHERE status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED');

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_last_login
ON users(last_login_at)
WHERE last_login_at IS NOT NULL;

-- =====================================================
-- COUPONS TABLE INDEXES
-- =====================================================

-- Primary coupon lookup index
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_code_unique
ON coupons(code);

-- User-based coupon queries (most common pattern)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_user_status_created
ON coupons(user_id, status, created_at DESC)
WHERE status IN ('ACTIVE', 'USED', 'EXPIRED');

-- Campaign management indexes
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_campaign_status
ON coupons(campaign_id, status)
WHERE campaign_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_campaign_created
ON coupons(campaign_id, created_at DESC)
WHERE campaign_id IS NOT NULL;

-- Expiration management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_expires_status
ON coupons(expires_at, status)
WHERE expires_at IS NOT NULL AND status = 'ACTIVE';

-- QR code and validation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_qr_code
ON coupons(qr_code)
WHERE qr_code IS NOT NULL;

-- =====================================================
-- REDEMPTIONS TABLE INDEXES
-- =====================================================

-- User redemption history (primary pattern)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_user_date
ON redemptions(user_id, redeemed_at DESC);

-- Station analytics and reporting
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_station_date
ON redemptions(station_id, redeemed_at DESC)
WHERE station_id IS NOT NULL;

-- Coupon tracking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_coupon_date
ON redemptions(coupon_id, redeemed_at DESC);

-- Time-based analytics (for partitioning preparation)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_date_only
ON redemptions(DATE(redeemed_at));

-- Status and validation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_redemptions_status_date
ON redemptions(status, redeemed_at DESC)
WHERE status IN ('COMPLETED', 'PENDING', 'FAILED');

-- =====================================================
-- STATIONS TABLE INDEXES
-- =====================================================

-- Geospatial index for location-based queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_location_gist
ON stations USING GIST(location)
WHERE location IS NOT NULL;

-- Active stations by region
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_active_region
ON stations(is_active, region)
WHERE is_active = true;

-- Station management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_owner_active
ON stations(owner_id, is_active)
WHERE owner_id IS NOT NULL;

-- Address-based searches
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_address_gin
ON stations USING GIN(to_tsvector('spanish', address))
WHERE address IS NOT NULL;

-- =====================================================
-- CAMPAIGNS TABLE INDEXES
-- =====================================================

-- Active campaigns
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_campaigns_active_dates
ON campaigns(is_active, start_date, end_date)
WHERE is_active = true;

-- Campaign management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_campaigns_owner_status
ON campaigns(created_by, status)
WHERE created_by IS NOT NULL;

-- Date range queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_campaigns_date_range
ON campaigns(start_date, end_date)
WHERE start_date IS NOT NULL AND end_date IS NOT NULL;

-- =====================================================
-- RAFFLE_TICKETS TABLE INDEXES
-- =====================================================

-- User raffle participation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_user_raffle
ON raffle_tickets(user_id, raffle_id, created_at DESC);

-- Raffle management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_raffle_status
ON raffle_tickets(raffle_id, status)
WHERE status IN ('ACTIVE', 'WINNER', 'EXPIRED');

-- Ticket number for draws
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffle_tickets_number
ON raffle_tickets(raffle_id, ticket_number)
WHERE ticket_number IS NOT NULL;

-- =====================================================
-- RAFFLES TABLE INDEXES
-- =====================================================

-- Active raffles
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffles_status_dates
ON raffles(status, start_date, end_date)
WHERE status IN ('ACTIVE', 'COMPLETED', 'CANCELLED');

-- Draw date management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffles_draw_date
ON raffles(draw_date)
WHERE draw_date IS NOT NULL;

-- =====================================================
-- AUDIT_LOGS TABLE INDEXES (if exists)
-- =====================================================

-- Time-based audit queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_timestamp
ON audit_logs(created_at DESC)
WHERE created_at IS NOT NULL;

-- User activity audit
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_user_action
ON audit_logs(user_id, action, created_at DESC)
WHERE user_id IS NOT NULL;

-- Entity audit tracking
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_audit_logs_entity
ON audit_logs(entity_type, entity_id, created_at DESC)
WHERE entity_type IS NOT NULL AND entity_id IS NOT NULL;

-- =====================================================
-- USER_SESSIONS TABLE INDEXES (if exists)
-- =====================================================

-- Session management
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_token
ON user_sessions(session_token)
WHERE session_token IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_user_sessions_user_active
ON user_sessions(user_id, is_active, expires_at)
WHERE is_active = true;

-- =====================================================
-- NOTIFICATIONS TABLE INDEXES (if exists)
-- =====================================================

-- User notifications
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_user_status
ON notifications(user_id, status, created_at DESC)
WHERE status IN ('PENDING', 'SENT', 'READ');

-- Notification type filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_notifications_type_date
ON notifications(notification_type, created_at DESC)
WHERE notification_type IS NOT NULL;

-- =====================================================
-- PERFORMANCE MONITORING INDEXES
-- =====================================================

-- Create indexes for monitoring slow queries
-- These help pg_stat_statements work more effectively

-- Generic pattern indexes for common WHERE clauses
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_generic_created_at
ON coupons(created_at DESC);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_generic_updated_at
ON coupons(updated_at DESC)
WHERE updated_at IS NOT NULL;

-- =====================================================
-- MAINTENANCE AND STATISTICS
-- =====================================================

-- Update table statistics after index creation
ANALYZE users;
ANALYZE coupons;
ANALYZE redemptions;
ANALYZE stations;
ANALYZE campaigns;
ANALYZE raffle_tickets;
ANALYZE raffles;

-- Create a function to automatically update statistics
CREATE OR REPLACE FUNCTION update_table_statistics()
RETURNS void AS $$
BEGIN
    -- Update statistics for all main tables
    ANALYZE users;
    ANALYZE coupons;
    ANALYZE redemptions;
    ANALYZE stations;
    ANALYZE campaigns;
    ANALYZE raffle_tickets;
    ANALYZE raffles;

    -- Log the update
    RAISE NOTICE 'Table statistics updated at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- INDEX USAGE MONITORING
-- =====================================================

-- Create a view to monitor index usage
CREATE OR REPLACE VIEW v_index_usage_stats AS
SELECT
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) as index_size,
    pg_relation_size(indexrelid) as index_size_bytes,
    CASE
        WHEN idx_scan = 0 THEN 'UNUSED'
        WHEN idx_scan < 100 THEN 'LOW_USAGE'
        WHEN idx_scan < 1000 THEN 'MEDIUM_USAGE'
        ELSE 'HIGH_USAGE'
    END as usage_category
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;

-- Create a view for table statistics
CREATE OR REPLACE VIEW v_table_maintenance_stats AS
SELECT
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    CASE
        WHEN n_live_tup > 0
        THEN ROUND((n_dead_tup::float / n_live_tup * 100)::numeric, 2)
        ELSE 0
    END as dead_tuple_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    CASE
        WHEN n_live_tup > 0 AND (n_dead_tup::float / n_live_tup) > 0.2 THEN 'NEEDS_VACUUM'
        WHEN last_analyze IS NULL OR last_analyze < NOW() - INTERVAL '1 day' THEN 'NEEDS_ANALYZE'
        ELSE 'OK'
    END as maintenance_status
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY n_live_tup DESC;

-- =====================================================
-- COMMENTS AND DOCUMENTATION
-- =====================================================

COMMENT ON INDEX idx_users_email_unique IS 'Unique index for user email authentication';
COMMENT ON INDEX idx_coupons_user_status_created IS 'Primary index for user coupon queries with status filtering';
COMMENT ON INDEX idx_redemptions_user_date IS 'Primary index for user redemption history';
COMMENT ON INDEX idx_stations_location_gist IS 'Geospatial index for location-based station searches';
COMMENT ON INDEX idx_coupons_code_unique IS 'Unique index for coupon code validation and redemption';

-- =====================================================
-- COMPLETION MESSAGE
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Gasolinera JSM Database Optimization Complete';
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Created optimized indexes for:';
    RAISE NOTICE '- Users table (authentication & activity)';
    RAISE NOTICE '- Coupons table (user queries & validation)';
    RAISE NOTICE '- Redemptions table (history & analytics)';
    RAISE NOTICE '- Stations table (geospatial & management)';
    RAISE NOTICE '- Campaigns table (active campaigns)';
    RAISE NOTICE '- Raffle system tables';
    RAISE NOTICE '- Audit and monitoring tables';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Monitor index usage with v_index_usage_stats';
    RAISE NOTICE '2. Check maintenance needs with v_table_maintenance_stats';
    RAISE NOTICE '3. Run ANALYZE on tables after data changes';
    RAISE NOTICE '4. Consider partitioning for large tables';
    RAISE NOTICE '=================================================';
END $$;