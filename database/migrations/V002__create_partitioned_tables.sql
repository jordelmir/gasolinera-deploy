-- Gasolinera JSM - Partitioned Tables Setup
-- Version: 2.0
-- Description: Creates partitioned versions of high-volume tables

-- =====================================================
-- REDEMPTIONS TABLE PARTITIONING (TIME-BASED)
-- =====================================================

-- Create partitioned redemptions table
CREATE TABLE redemptions_partitioned (
    LIKE redemptions INCLUDING ALL
) PARTITION BY RANGE (redeemed_at);

-- Create monthly partitions for current and future months
-- Current year partitions
CREATE TABLE redemptions_2024_01 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE redemptions_2024_02 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE redemptions_2024_03 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE redemptions_2024_04 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE redemptions_2024_05 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE redemptions_2024_06 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE redemptions_2024_07 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE redemptions_2024_08 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE redemptions_2024_09 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE redemptions_2024_10 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE redemptions_2024_11 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE redemptions_2024_12 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- Next year partitions
CREATE TABLE redemptions_2025_01 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE redemptions_2025_02 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

CREATE TABLE redemptions_2025_03 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-03-01') TO ('2025-04-01');

CREATE TABLE redemptions_2025_04 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

CREATE TABLE redemptions_2025_05 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-05-01') TO ('2025-06-01');

CREATE TABLE redemptions_2025_06 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-06-01') TO ('2025-07-01');

CREATE TABLE redemptions_2025_07 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-07-01') TO ('2025-08-01');

CREATE TABLE redemptions_2025_08 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-08-01') TO ('2025-09-01');

CREATE TABLE redemptions_2025_09 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');

CREATE TABLE redemptions_2025_10 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');

CREATE TABLE redemptions_2025_11 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-11-01') TO ('2025-12-01');

CREATE TABLE redemptions_2025_12 PARTITION OF redemptions_partitioned
    FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- =====================================================
-- AUDIT_LOGS TABLE PARTITIONING (TIME-BASED)
-- =====================================================

-- Create audit_logs table if it doesn't exist
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create partitioned audit_logs table
CREATE TABLE audit_logs_partitioned (
    LIKE audit_logs INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Create monthly partitions for audit logs
CREATE TABLE audit_logs_2024_01 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE audit_logs_2024_02 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE audit_logs_2024_03 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');

CREATE TABLE audit_logs_2024_04 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');

CREATE TABLE audit_logs_2024_05 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-05-01') TO ('2024-06-01');

CREATE TABLE audit_logs_2024_06 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-06-01') TO ('2024-07-01');

CREATE TABLE audit_logs_2024_07 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-08-01');

CREATE TABLE audit_logs_2024_08 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-08-01') TO ('2024-09-01');

CREATE TABLE audit_logs_2024_09 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-09-01') TO ('2024-10-01');

CREATE TABLE audit_logs_2024_10 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2024-11-01');

CREATE TABLE audit_logs_2024_11 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-11-01') TO ('2024-12-01');

CREATE TABLE audit_logs_2024_12 PARTITION OF audit_logs_partitioned
    FOR VALUES FROM ('2024-12-01') TO ('2025-01-01');

-- =====================================================
-- COUPONS TABLE PARTITIONING (HASH-BASED)
-- =====================================================

-- Create hash-partitioned coupons table for better distribution
CREATE TABLE coupons_partitioned (
    LIKE coupons INCLUDING ALL
) PARTITION BY HASH (user_id);

-- Create 4 hash partitions for coupons
CREATE TABLE coupons_hash_0 PARTITION OF coupons_partitioned
    FOR VALUES WITH (modulus 4, remainder 0);

CREATE TABLE coupons_hash_1 PARTITION OF coupons_partitioned
    FOR VALUES WITH (modulus 4, remainder 1);

CREATE TABLE coupons_hash_2 PARTITION OF coupons_partitioned
    FOR VALUES WITH (modulus 4, remainder 2);

CREATE TABLE coupons_hash_3 PARTITION OF coupons_partitioned
    FOR VALUES WITH (modulus 4, remainder 3);

-- =====================================================
-- RAFFLE_TICKETS TABLE PARTITIONING (TIME-BASED)
-- =====================================================

-- Create partitioned raffle_tickets table
CREATE TABLE raffle_tickets_partitioned (
    LIKE raffle_tickets INCLUDING ALL
) PARTITION BY RANGE (created_at);

-- Create quarterly partitions for raffle tickets
CREATE TABLE raffle_tickets_2024_q1 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');

CREATE TABLE raffle_tickets_2024_q2 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');

CREATE TABLE raffle_tickets_2024_q3 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');

CREATE TABLE raffle_tickets_2024_q4 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE raffle_tickets_2025_q1 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');

CREATE TABLE raffle_tickets_2025_q2 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');

CREATE TABLE raffle_tickets_2025_q3 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');

CREATE TABLE raffle_tickets_2025_q4 PARTITION OF raffle_tickets_partitioned
    FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');

-- =====================================================
-- PARTITION INDEXES
-- =====================================================

-- Create indexes on each partition for optimal performance

-- Redemptions partition indexes
CREATE INDEX CONCURRENTLY idx_redemptions_2024_01_user_id ON redemptions_2024_01(user_id);
CREATE INDEX CONCURRENTLY idx_redemptions_2024_01_station_id ON redemptions_2024_01(station_id);
CREATE INDEX CONCURRENTLY idx_redemptions_2024_01_coupon_id ON redemptions_2024_01(coupon_id);

CREATE INDEX CONCURRENTLY idx_redemptions_2024_02_user_id ON redemptions_2024_02(user_id);
CREATE INDEX CONCURRENTLY idx_redemptions_2024_02_station_id ON redemptions_2024_02(station_id);
CREATE INDEX CONCURRENTLY idx_redemptions_2024_02_coupon_id ON redemptions_2024_02(coupon_id);

-- Continue pattern for other months (abbreviated for space)
-- In production, you would create these for all partitions

-- Audit logs partition indexes
CREATE INDEX CONCURRENTLY idx_audit_logs_2024_01_user_id ON audit_logs_2024_01(user_id);
CREATE INDEX CONCURRENTLY idx_audit_logs_2024_01_action ON audit_logs_2024_01(action);
CREATE INDEX CONCURRENTLY idx_audit_logs_2024_01_entity ON audit_logs_2024_01(entity_type, entity_id);

-- Coupons hash partition indexes
CREATE INDEX CONCURRENTLY idx_coupons_hash_0_status_created ON coupons_hash_0(status, created_at);
CREATE INDEX CONCURRENTLY idx_coupons_hash_0_code ON coupons_hash_0(code);
CREATE INDEX CONCURRENTLY idx_coupons_hash_0_campaign ON coupons_hash_0(campaign_id);

CREATE INDEX CONCURRENTLY idx_coupons_hash_1_status_created ON coupons_hash_1(status, created_at);
CREATE INDEX CONCURRENTLY idx_coupons_hash_1_code ON coupons_hash_1(code);
CREATE INDEX CONCURRENTLY idx_coupons_hash_1_campaign ON coupons_hash_1(campaign_id);

CREATE INDEX CONCURRENTLY idx_coupons_hash_2_status_created ON coupons_hash_2(status, created_at);
CREATE INDEX CONCURRENTLY idx_coupons_hash_2_code ON coupons_hash_2(code);
CREATE INDEX CONCURRENTLY idx_coupons_hash_2_campaign ON coupons_hash_2(campaign_id);

CREATE INDEX CONCURRENTLY idx_coupons_hash_3_status_created ON coupons_hash_3(status, created_at);
CREATE INDEX CONCURRENTLY idx_coupons_hash_3_code ON coupons_hash_3(code);
CREATE INDEX CONCURRENTLY idx_coupons_hash_3_campaign ON coupons_hash_3(campaign_id);

-- Raffle tickets partition indexes
CREATE INDEX CONCURRENTLY idx_raffle_tickets_2024_q1_user_raffle ON raffle_tickets_2024_q1(user_id, raffle_id);
CREATE INDEX CONCURRENTLY idx_raffle_tickets_2024_q1_raffle_status ON raffle_tickets_2024_q1(raffle_id, status);

CREATE INDEX CONCURRENTLY idx_raffle_tickets_2024_q2_user_raffle ON raffle_tickets_2024_q2(user_id, raffle_id);
CREATE INDEX CONCURRENTLY idx_raffle_tickets_2024_q2_raffle_status ON raffle_tickets_2024_q2(raffle_id, status);

-- =====================================================
-- PARTITION MANAGEMENT FUNCTIONS
-- =====================================================

-- Function to create future redemption partitions
CREATE OR REPLACE FUNCTION create_redemption_partition(partition_date DATE)
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
BEGIN
    -- Calculate partition boundaries
    start_date := DATE_TRUNC('month', partition_date)::DATE;
    end_date := (start_date + INTERVAL '1 month')::DATE;

    -- Generate partition name
    partition_name := 'redemptions_' || TO_CHAR(start_date, 'YYYY_MM');

    -- Create partition
    EXECUTE format('CREATE TABLE %I PARTITION OF redemptions_partitioned FOR VALUES FROM (%L) TO (%L)',
                   partition_name, start_date, end_date);

    -- Create indexes on new partition
    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_user_id ON %I(user_id)', partition_name, partition_name);
    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_station_id ON %I(station_id)', partition_name, partition_name);
    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_coupon_id ON %I(coupon_id)', partition_name, partition_name);

    RAISE NOTICE 'Created partition % for date range % to %', partition_name, start_date, end_date;
END;
$$ LANGUAGE plpgsql;

-- Function to create future audit log partitions
CREATE OR REPLACE FUNCTION create_audit_partition(partition_date DATE)
RETURNS void AS $$
DECLARE
    partition_name TEXT;
    start_date DATE;
    end_date DATE;
BEGIN
    start_date := DATE_TRUNC('month', partition_date)::DATE;
    end_date := (start_date + INTERVAL '1 month')::DATE;
    partition_name := 'audit_logs_' || TO_CHAR(start_date, 'YYYY_MM');

    EXECUTE format('CREATE TABLE %I PARTITION OF audit_logs_partitioned FOR VALUES FROM (%L) TO (%L)',
                   partition_name, start_date, end_date);

    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_user_id ON %I(user_id)', partition_name, partition_name);
    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_action ON %I(action)', partition_name, partition_name);
    EXECUTE format('CREATE INDEX CONCURRENTLY idx_%s_entity ON %I(entity_type, entity_id)', partition_name, partition_name);

    RAISE NOTICE 'Created audit partition % for date range % to %', partition_name, start_date, end_date;
END;
$$ LANGUAGE plpgsql;

-- Function to drop old partitions (for data retention)
CREATE OR REPLACE FUNCTION drop_old_partitions(table_prefix TEXT, retention_months INTEGER)
RETURNS void AS $$
DECLARE
    partition_record RECORD;
    cutoff_date DATE;
BEGIN
    cutoff_date := (CURRENT_DATE - (retention_months || ' months')::INTERVAL)::DATE;

    FOR partition_record IN
        SELECT schemaname, tablename
        FROM pg_tables
        WHERE tablename LIKE table_prefix || '%'
        AND schemaname = 'public'
    LOOP
        -- Extract date from partition name and check if it's old enough
        -- This is a simplified version - in production you'd want more robust date parsing
        IF partition_record.tablename ~ '\d{4}_\d{2}$' THEN
            DECLARE
                partition_date_str TEXT;
                partition_date DATE;
            BEGIN
                partition_date_str := RIGHT(partition_record.tablename, 7);
                partition_date := TO_DATE(partition_date_str, 'YYYY_MM');

                IF partition_date < cutoff_date THEN
                    EXECUTE format('DROP TABLE IF EXISTS %I.%I', partition_record.schemaname, partition_record.tablename);
                    RAISE NOTICE 'Dropped old partition: %', partition_record.tablename;
                END IF;
            EXCEPTION
                WHEN OTHERS THEN
                    RAISE NOTICE 'Could not process partition: %', partition_record.tablename;
            END;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- AUTOMATED PARTITION MANAGEMENT
-- =====================================================

-- Function to automatically create next month's partitions
CREATE OR REPLACE FUNCTION maintain_partitions()
RETURNS void AS $$
DECLARE
    next_month DATE;
    month_after DATE;
BEGIN
    next_month := (DATE_TRUNC('month', CURRENT_DATE) + INTERVAL '1 month')::DATE;
    month_after := (next_month + INTERVAL '1 month')::DATE;

    -- Create redemption partitions for next 2 months if they don't exist
    BEGIN
        PERFORM create_redemption_partition(next_month);
    EXCEPTION
        WHEN duplicate_table THEN
            RAISE NOTICE 'Redemption partition for % already exists', next_month;
    END;

    BEGIN
        PERFORM create_redemption_partition(month_after);
    EXCEPTION
        WHEN duplicate_table THEN
            RAISE NOTICE 'Redemption partition for % already exists', month_after;
    END;

    -- Create audit log partitions
    BEGIN
        PERFORM create_audit_partition(next_month);
    EXCEPTION
        WHEN duplicate_table THEN
            RAISE NOTICE 'Audit partition for % already exists', next_month;
    END;

    BEGIN
        PERFORM create_audit_partition(month_after);
    EXCEPTION
        WHEN duplicate_table THEN
            RAISE NOTICE 'Audit partition for % already exists', month_after;
    END;

    RAISE NOTICE 'Partition maintenance completed at %', NOW();
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PARTITION MONITORING VIEWS
-- =====================================================

-- View to monitor partition sizes
CREATE OR REPLACE VIEW v_partition_sizes AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
    pg_total_relation_size(schemaname||'.'||tablename) as size_bytes,
    CASE
        WHEN tablename LIKE '%_2024_%' THEN '2024'
        WHEN tablename LIKE '%_2025_%' THEN '2025'
        ELSE 'OTHER'
    END as year_group,
    CASE
        WHEN tablename LIKE 'redemptions_%' THEN 'REDEMPTIONS'
        WHEN tablename LIKE 'audit_logs_%' THEN 'AUDIT_LOGS'
        WHEN tablename LIKE 'coupons_hash_%' THEN 'COUPONS'
        WHEN tablename LIKE 'raffle_tickets_%' THEN 'RAFFLE_TICKETS'
        ELSE 'OTHER'
    END as table_group
FROM pg_tables
WHERE schemaname = 'public'
AND (tablename LIKE '%_2024_%' OR tablename LIKE '%_2025_%' OR tablename LIKE '%_hash_%')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- View to monitor partition row counts
CREATE OR REPLACE VIEW v_partition_stats AS
SELECT
    schemaname,
    tablename,
    n_live_tup as row_count,
    n_dead_tup as dead_rows,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
WHERE schemaname = 'public'
AND (tablename LIKE '%_2024_%' OR tablename LIKE '%_2025_%' OR tablename LIKE '%_hash_%')
ORDER BY n_live_tup DESC;

-- =====================================================
-- CONSTRAINT EXCLUSION SETUP
-- =====================================================

-- Enable constraint exclusion for better query performance
-- This should be set in postgresql.conf, but we can check current setting
DO $$
BEGIN
    IF (SELECT setting FROM pg_settings WHERE name = 'constraint_exclusion') != 'partition' THEN
        RAISE NOTICE 'RECOMMENDATION: Set constraint_exclusion = partition in postgresql.conf for better partition performance';
    END IF;
END $$;

-- =====================================================
-- MIGRATION HELPER FUNCTIONS
-- =====================================================

-- Function to migrate data from original table to partitioned table
CREATE OR REPLACE FUNCTION migrate_to_partitioned_table(
    source_table TEXT,
    target_table TEXT,
    batch_size INTEGER DEFAULT 10000
)
RETURNS void AS $$
DECLARE
    total_rows BIGINT;
    processed_rows BIGINT := 0;
    batch_count INTEGER := 0;
BEGIN
    -- Get total row count
    EXECUTE format('SELECT COUNT(*) FROM %I', source_table) INTO total_rows;

    RAISE NOTICE 'Starting migration of % rows from % to %', total_rows, source_table, target_table;

    -- Migrate in batches
    LOOP
        EXECUTE format('
            INSERT INTO %I
            SELECT * FROM %I
            ORDER BY id
            LIMIT %s OFFSET %s',
            target_table, source_table, batch_size, processed_rows);

        GET DIAGNOSTICS batch_count = ROW_COUNT;
        processed_rows := processed_rows + batch_count;

        RAISE NOTICE 'Migrated % of % rows (%.1f%%)',
                     processed_rows, total_rows,
                     (processed_rows::float / total_rows * 100);

        EXIT WHEN batch_count = 0;

        -- Commit every batch to avoid long transactions
        COMMIT;
    END LOOP;

    RAISE NOTICE 'Migration completed: % rows migrated', processed_rows;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMPLETION AND NEXT STEPS
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Gasolinera JSM Partitioning Setup Complete';
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Created partitioned tables:';
    RAISE NOTICE '- redemptions_partitioned (monthly time-based)';
    RAISE NOTICE '- audit_logs_partitioned (monthly time-based)';
    RAISE NOTICE '- coupons_partitioned (4-way hash-based)';
    RAISE NOTICE '- raffle_tickets_partitioned (quarterly time-based)';
    RAISE NOTICE '';
    RAISE NOTICE 'Management functions created:';
    RAISE NOTICE '- create_redemption_partition(date)';
    RAISE NOTICE '- create_audit_partition(date)';
    RAISE NOTICE '- drop_old_partitions(prefix, months)';
    RAISE NOTICE '- maintain_partitions()';
    RAISE NOTICE '- migrate_to_partitioned_table(source, target)';
    RAISE NOTICE '';
    RAISE NOTICE 'Monitoring views created:';
    RAISE NOTICE '- v_partition_sizes';
    RAISE NOTICE '- v_partition_stats';
    RAISE NOTICE '';
    RAISE NOTICE 'Next steps:';
    RAISE NOTICE '1. Test partitioned tables with sample data';
    RAISE NOTICE '2. Migrate existing data using migrate_to_partitioned_table()';
    RAISE NOTICE '3. Update application to use partitioned tables';
    RAISE NOTICE '4. Schedule maintain_partitions() to run monthly';
    RAISE NOTICE '5. Monitor partition performance and sizes';
    RAISE NOTICE '=================================================';
END $$;