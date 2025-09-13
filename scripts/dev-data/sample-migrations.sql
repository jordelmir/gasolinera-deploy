-- Development Sample Data Migrations
-- Creates sample data for development environment

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create development-specific indexes for performance
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_dev ON users(email) WHERE active = true;
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_coupons_status_dev ON coupons(status, created_at);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_stations_location_dev ON stations USING GIST(ST_Point(longitude, latitude));
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_raffles_active_dev ON raffles(active, end_date) WHERE active = true;

-- Insert sample fuel types if not exists
INSERT INTO fuel_types (id, name, description, created_at, updated_at)
VALUES
    (1, 'REGULAR', 'Regular gasoline', NOW(), NOW()),
    (2, 'PREMIUM', 'Premium gasoline', NOW(), NOW()),
    (3, 'DIESEL', 'Diesel fuel', NOW(), NOW()),
    (4, 'ELECTRIC', 'Electric charging', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert sample service types if not exists
INSERT INTO service_types (id, name, description, created_at, updated_at)
VALUES
    (1, 'CONVENIENCE_STORE', 'Convenience store', NOW(), NOW()),
    (2, 'CAR_WASH', 'Car wash service', NOW(), NOW()),
    (3, 'ATM', 'ATM machine', NOW(), NOW()),
    (4, 'TIRE_REPAIR', 'Tire repair service', NOW(), NOW()),
    (5, 'ELECTRIC_CHARGING', 'Electric vehicle charging', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert sample user roles if not exists
INSERT INTO user_roles (id, name, description, permissions, created_at, updated_at)
VALUES
    (1, 'ADMIN', 'System administrator', '["ALL"]', NOW(), NOW()),
    (2, 'USER', 'Regular user', '["READ_PROFILE", "UPDATE_PROFILE", "USE_COUPONS", "PARTICIPATE_RAFFLES"]', NOW(), NOW()),
    (3, 'STATION_OWNER', 'Gas station owner', '["MANAGE_STATION", "VIEW_ANALYTICS", "MANAGE_FUEL_PRICES"]', NOW(), NOW()),
    (4, 'ADVERTISER', 'Advertiser', '["CREATE_ADS", "VIEW_AD_ANALYTICS", "MANAGE_CAMPAIGNS"]', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert sample discount types if not exists
INSERT INTO discount_types (id, name, description, created_at, updated_at)
VALUES
    (1, 'PERCENTAGE', 'Percentage discount', NOW(), NOW()),
    (2, 'FIXED_AMOUNT', 'Fixed amount discount', NOW(), NOW()),
    (3, 'BUY_ONE_GET_ONE', 'Buy one get one free', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert sample campaign statuses if not exists
INSERT INTO campaign_statuses (id, name, description, created_at, updated_at)
VALUES
    (1, 'DRAFT', 'Campaign in draft state', NOW(), NOW()),
    (2, 'ACTIVE', 'Active campaign', NOW(), NOW()),
    (3, 'PAUSED', 'Paused campaign', NOW(), NOW()),
    (4, 'EXPIRED', 'Expired campaign', NOW(), NOW()),
    (5, 'CANCELLED', 'Cancelled campaign', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert sample raffle statuses if not exists
INSERT INTO raffle_statuses (id, name, description, created_at, updated_at)
VALUES
    (1, 'UPCOMING', 'Raffle not yet started', NOW(), NOW()),
    (2, 'ACTIVE', 'Active raffle', NOW(), NOW()),
    (3, 'DRAWING', 'Drawing in progress', NOW(), NOW()),
    (4, 'COMPLETED', 'Raffle completed', NOW(), NOW()),
    (5, 'CANCELLED', 'Cancelled raffle', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Create development-specific functions
CREATE OR REPLACE FUNCTION generate_dev_coupon_code()
RETURNS TEXT AS $$
BEGIN
    RETURN 'DEV-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT) FROM 1 FOR 8));
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_dev_qr_code(coupon_id UUID)
RETURNS TEXT AS $$
BEGIN
    RETURN 'QR-DEV-' || REPLACE(coupon_id::TEXT, '-', '') || '-' || EXTRACT(EPOCH FROM NOW())::BIGINT;
END;
$$ LANGUAGE plpgsql;

-- Create development audit trigger
CREATE OR REPLACE FUNCTION audit_dev_changes()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by, changed_at)
        VALUES (TG_TABLE_NAME, TG_OP, NEW.id, NULL, row_to_json(NEW), 'SYSTEM', NOW());
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by, changed_at)
        VALUES (TG_TABLE_NAME, TG_OP, NEW.id, row_to_json(OLD), row_to_json(NEW), 'SYSTEM', NOW());
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log (table_name, operation, record_id, old_values, new_values, changed_by, changed_at)
        VALUES (TG_TABLE_NAME, TG_OP, OLD.id, row_to_json(OLD), NULL, 'SYSTEM', NOW());
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create audit log table if not exists
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name VARCHAR(255) NOT NULL,
    operation VARCHAR(10) NOT NULL,
    record_id UUID,
    old_values JSONB,
    new_values JSONB,
    changed_by VARCHAR(255),
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create development performance monitoring views
CREATE OR REPLACE VIEW dev_slow_queries AS
SELECT
    query,
    calls,
    total_time,
    mean_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
WHERE mean_time > 100
ORDER BY mean_time DESC;

CREATE OR REPLACE VIEW dev_table_stats AS
SELECT
    schemaname,
    tablename,
    n_tup_ins as inserts,
    n_tup_upd as updates,
    n_tup_del as deletes,
    n_live_tup as live_tuples,
    n_dead_tup as dead_tuples,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC;

-- Insert development configuration
INSERT INTO system_config (key, value, description, created_at, updated_at)
VALUES
    ('dev.environment', 'development', 'Development environment flag', NOW(), NOW()),
    ('dev.debug_mode', 'true', 'Enable debug mode', NOW(), NOW()),
    ('dev.mock_external_services', 'true', 'Use mock external services', NOW(), NOW()),
    ('dev.enable_test_endpoints', 'true', 'Enable test endpoints', NOW(), NOW()),
    ('dev.log_level', 'DEBUG', 'Development log level', NOW(), NOW())
ON CONFLICT (key) DO UPDATE SET
    value = EXCLUDED.value,
    updated_at = NOW();

-- Create development notification preferences
INSERT INTO notification_preferences (user_id, email_enabled, sms_enabled, push_enabled, created_at, updated_at)
SELECT
    id,
    true,
    false,
    true,
    NOW(),
    NOW()
FROM users
WHERE email LIKE '%@test.com' OR email LIKE '%@gasolinera.com'
ON CONFLICT (user_id) DO NOTHING;

-- Grant necessary permissions for development
GRANT SELECT ON ALL TABLES IN SCHEMA public TO gasolinera_dev;
GRANT SELECT ON pg_stat_statements TO gasolinera_dev;
GRANT SELECT ON pg_stat_user_tables TO gasolinera_dev;

-- Create development-specific sequences with higher starting values
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'dev_coupon_sequence') THEN
        CREATE SEQUENCE dev_coupon_sequence START 10000;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'dev_raffle_sequence') THEN
        CREATE SEQUENCE dev_raffle_sequence START 1000;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE sequencename = 'dev_station_sequence') THEN
        CREATE SEQUENCE dev_station_sequence START 100;
    END IF;
END $$;

-- Development data cleanup function
CREATE OR REPLACE FUNCTION cleanup_dev_data()
RETURNS VOID AS $$
BEGIN
    -- Clean up old test data (older than 7 days)
    DELETE FROM audit_log WHERE changed_at < NOW() - INTERVAL '7 days';
    DELETE FROM user_sessions WHERE created_at < NOW() - INTERVAL '1 day';
    DELETE FROM temporary_tokens WHERE expires_at < NOW();

    -- Reset sequences if needed
    PERFORM setval('dev_coupon_sequence', 10000, false);
    PERFORM setval('dev_raffle_sequence', 1000, false);
    PERFORM setval('dev_station_sequence', 100, false);

    -- Vacuum analyze for performance
    VACUUM ANALYZE;
END;
$$ LANGUAGE plpgsql;

-- Schedule cleanup (if pg_cron is available)
-- SELECT cron.schedule('cleanup-dev-data', '0 2 * * *', 'SELECT cleanup_dev_data();');

COMMIT;