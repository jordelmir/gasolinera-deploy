-- Gasolinera JSM - Advanced PostgreSQL Optimizations
-- Version: 3.0
-- Description: Advanced PostgreSQL configurations and optimizations

-- =====================================================
-- ENABLE ADVANCED EXTENSIONS
-- =====================================================

-- Enable additional useful extensions
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_buffercache";
CREATE EXTENSION IF NOT EXISTS "pgstattuple";
CREATE EXTENSION IF NOT EXISTS "pg_prewarm";

-- =====================================================
-- QUERY PERFORMANCE MONITORING
-- =====================================================

-- Create a function to get top slow queries
CREATE OR REPLACE FUNCTION get_slow_queries(limit_count INTEGER DEFAULT 10)
RETURNS TABLE(
    query TEXT,
    calls BIGINT,
    total_time DOUBLE PRECISION,
    mean_time DOUBLE PRECISION,
    rows BIGINT,
    hit_percent DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.total_exec_time as total_time,
        pss.mean_exec_time as mean_time,
        pss.rows,
        CASE
            WHEN (pss.shared_blks_hit + pss.shared_blks_read) > 0
            THEN (pss.shared_blks_hit::FLOAT / (pss.shared_blks_hit + pss.shared_blks_read) * 100)
            ELSE 0
        END as hit_percent
    FROM pg_stat_statements pss
    WHERE pss.calls > 1
    ORDER BY pss.total_exec_time DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Create a function to get table bloat information
CREATE OR REPLACE FUNCTION get_table_bloat()
RETURNS TABLE(
    schema_name TEXT,
    table_name TEXT,
    table_size TEXT,
    bloat_size TEXT,
    bloat_ratio NUMERIC
) AS $$
BEGIN
    RETURN QUERY
    WITH table_stats AS (
        SELECT
            schemaname,
            tablename,
            pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
            pg_total_relation_size(schemaname||'.'||tablename) as size_bytes,
            n_dead_tup,
            n_live_tup
        FROM pg_stat_user_tables
        WHERE schemaname = 'public'
    )
    SELECT
        ts.schemaname::TEXT,
        ts.tablename::TEXT,
        ts.size::TEXT,
        pg_size_pretty(
            CASE
                WHEN ts.n_live_tup > 0
                THEN (ts.size_bytes * ts.n_dead_tup / (ts.n_live_tup + ts.n_dead_tup))::BIGINT
                ELSE 0
            END
        )::TEXT as bloat_size,
        CASE
            WHEN ts.n_live_tup > 0
            THEN ROUND((ts.n_dead_tup::NUMERIC / (ts.n_live_tup + ts.n_dead_tup) * 100), 2)
            ELSE 0
        END as bloat_ratio
    FROM table_stats ts
    WHERE ts.n_live_tup + ts.n_dead_tup > 0
    ORDER BY
        CASE
            WHEN ts.n_live_tup > 0
            THEN (ts.n_dead_tup::FLOAT / (ts.n_live_tup + ts.n_dead_tup))
            ELSE 0
        END DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- INDEX OPTIMIZATION FUNCTIONS
-- =====================================================

-- Function to find unused indexes
CREATE OR REPLACE FUNCTION find_unused_indexes()
RETURNS TABLE(
    schema_name TEXT,
    table_name TEXT,
    index_name TEXT,
    index_size TEXT,
    index_scans BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        psi.schemaname::TEXT,
        psi.tablename::TEXT,
        psi.indexrelname::TEXT,
        pg_size_pretty(pg_relation_size(psi.indexrelid))::TEXT,
        psi.idx_scan
    FROM pg_stat_user_indexes psi
    JOIN pg_index pi ON psi.indexrelid = pi.indexrelid
    WHERE psi.schemaname = 'public'
    AND psi.idx_scan < 100  -- Less than 100 scans
    AND NOT pi.indisunique   -- Not a unique index
    AND NOT pi.indisprimary  -- Not a primary key
    ORDER BY psi.idx_scan, pg_relation_size(psi.indexrelid) DESC;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- MAINTENANCE AUTOMATION
-- =====================================================

-- Function to perform intelligent VACUUM and ANALYZE
CREATE OR REPLACE FUNCTION intelligent_maintenance()
RETURNS TEXT AS $$
DECLARE
    table_record RECORD;
    maintenance_log TEXT := '';
BEGIN
    -- Analyze tables that need statistics update
    FOR table_record IN
        SELECT schemaname, tablename, n_dead_tup, n_live_tup, last_analyze
        FROM pg_stat_user_tables
        WHERE schemaname = 'public'
        AND (
            last_analyze IS NULL
            OR last_analyze < NOW() - INTERVAL '1 day'
            OR (n_live_tup > 0 AND n_dead_tup::FLOAT / n_live_tup > 0.1)
        )
    LOOP
        -- ANALYZE if statistics are old
        IF table_record.last_analyze IS NULL OR table_record.last_analyze < NOW() - INTERVAL '1 day' THEN
            EXECUTE format('ANALYZE %I.%I', table_record.schemaname, table_record.tablename);
            maintenance_log := maintenance_log || format('ANALYZED %s.%s' || chr(10), table_record.schemaname, table_record.tablename);
        END IF;

        -- VACUUM if too many dead tuples
        IF table_record.n_live_tup > 0 AND table_record.n_dead_tup::FLOAT / table_record.n_live_tup > 0.2 THEN
            EXECUTE format('VACUUM %I.%I', table_record.schemaname, table_record.tablename);
            maintenance_log := maintenance_log || format('VACUUMED %s.%s' || chr(10), table_record.schemaname, table_record.tablename);
        END IF;
    END LOOP;

    IF maintenance_log = '' THEN
        maintenance_log := 'No maintenance required at this time.';
    END IF;

    RETURN maintenance_log;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- PERFORMANCE MONITORING VIEWS
-- =====================================================

-- View for table size monitoring
CREATE OR REPLACE VIEW v_table_size_stats AS
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as total_size,
    pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename) - pg_relation_size(schemaname||'.'||tablename)) as index_size,
    pg_total_relation_size(schemaname||'.'||tablename) as total_size_bytes,
    n_live_tup as row_count
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- =====================================================
-- HEALTH CHECK FUNCTIONS
-- =====================================================

-- Function for comprehensive database health check
CREATE OR REPLACE FUNCTION database_health_check()
RETURNS TABLE(
    check_name TEXT,
    status TEXT,
    message TEXT,
    recommendation TEXT
) AS $$
BEGIN
    RETURN QUERY
    WITH health_checks AS (
        -- Check connection count
        SELECT
            'Connection Count'::TEXT as check_name,
            CASE
                WHEN COUNT(*) > 80 THEN 'WARNING'
                WHEN COUNT(*) > 100 THEN 'CRITICAL'
                ELSE 'OK'
            END as status,
            format('Current connections: %s', COUNT(*))::TEXT as message,
            CASE
                WHEN COUNT(*) > 80 THEN 'Consider increasing max_connections or optimizing connection pooling'
                ELSE 'Connection count is healthy'
            END::TEXT as recommendation
        FROM pg_stat_activity
        WHERE state = 'active'

        UNION ALL

        -- Check for table bloat
        SELECT
            'Table Bloat'::TEXT,
            CASE
                WHEN COUNT(*) > 5 THEN 'WARNING'
                WHEN COUNT(*) > 10 THEN 'CRITICAL'
                ELSE 'OK'
            END,
            format('Tables with >20%% bloat: %s', COUNT(*))::TEXT,
            CASE
                WHEN COUNT(*) > 5 THEN 'Schedule VACUUM operations for bloated tables'
                ELSE 'Table bloat is under control'
            END::TEXT
        FROM pg_stat_user_tables
        WHERE schemaname = 'public'
        AND n_live_tup > 0
        AND n_dead_tup::FLOAT / n_live_tup > 0.2
    )
    SELECT * FROM health_checks;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- COMPLETION MESSAGE
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Advanced PostgreSQL Optimizations Complete';
    RAISE NOTICE '=================================================';
    RAISE NOTICE 'Created monitoring functions:';
    RAISE NOTICE '- get_slow_queries(limit)';
    RAISE NOTICE '- get_table_bloat()';
    RAISE NOTICE '- find_unused_indexes()';
    RAISE NOTICE '- intelligent_maintenance()';
    RAISE NOTICE '- database_health_check()';
    RAISE NOTICE '';
    RAISE NOTICE 'Created monitoring views:';
    RAISE NOTICE '- v_table_size_stats';
    RAISE NOTICE '';
    RAISE NOTICE 'Usage examples:';
    RAISE NOTICE '- SELECT * FROM get_slow_queries(5);';
    RAISE NOTICE '- SELECT * FROM database_health_check();';
    RAISE NOTICE '- SELECT intelligent_maintenance();';
    RAISE NOTICE '=================================================';
END $$;