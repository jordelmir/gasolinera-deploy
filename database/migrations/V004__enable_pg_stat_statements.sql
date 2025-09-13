-- V004: Habilitar pg_stat_statements para monitoreo de performance
-- Descripción: Configura pg_stat_statements para análisis de queries y optimización

-- Crear extensión pg_stat_statements si no existe
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Verificar que la extensión esté instalada
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_stat_statements') THEN
        RAISE EXCEPTION 'pg_stat_statements extension is not available. Please install it first.';
    END IF;
END $$;

-- Función para obtener queries lentas
CREATE OR REPLACE FUNCTION get_slow_queries(limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_exec_time DOUBLE PRECISION,
    mean_exec_time DOUBLE PRECISION,
    max_exec_time DOUBLE PRECISION,
    min_exec_time DOUBLE PRECISION,
    stddev_exec_time DOUBLE PRECISION,
    rows_returned BIGINT,
    hit_percent DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.total_exec_time,
        pss.mean_exec_time,
        pss.max_exec_time,
        pss.min_exec_time,
        pss.stddev_exec_time,
        pss.rows,
        CASE
            WHEN (pss.shared_blks_hit + pss.shared_blks_read) > 0
            THEN 100.0 * pss.shared_blks_hit / (pss.shared_blks_hit + pss.shared_blks_read)
            ELSE 0.0
        END AS hit_percent
    FROM pg_stat_statements pss
    WHERE pss.mean_exec_time > 100  -- Queries con tiempo promedio > 100ms
    ORDER BY pss.mean_exec_time DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener queries más frecuentes
CREATE OR REPLACE FUNCTION get_frequent_queries(limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_exec_time DOUBLE PRECISION,
    mean_exec_time DOUBLE PRECISION,
    rows_returned BIGINT,
    hit_percent DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.total_exec_time,
        pss.mean_exec_time,
        pss.rows,
        CASE
            WHEN (pss.shared_blks_hit + pss.shared_blks_read) > 0
            THEN 100.0 * pss.shared_blks_hit / (pss.shared_blks_hit + pss.shared_blks_read)
            ELSE 0.0
        END AS hit_percent
    FROM pg_stat_statements pss
    WHERE pss.calls > 100  -- Queries ejecutadas más de 100 veces
    ORDER BY pss.calls DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener queries más costosas
CREATE OR REPLACE FUNCTION get_expensive_queries(limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_exec_time DOUBLE PRECISION,
    mean_exec_time DOUBLE PRECISION,
    percent_total_time DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.total_exec_time,
        pss.mean_exec_time,
        (pss.total_exec_time / NULLIF(sum(pss.total_exec_time) OVER(), 0)) * 100 AS percent_total_time
    FROM pg_stat_statements pss
    WHERE pss.total_exec_time > 1000  -- Queries con tiempo total > 1 segundo
    ORDER BY pss.total_exec_time DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener queries intensivas en I/O
CREATE OR REPLACE FUNCTION get_io_intensive_queries(limit_count INTEGER DEFAULT 20)
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    shared_blks_read BIGINT,
    shared_blks_hit BIGINT,
    shared_blks_dirtied BIGINT,
    shared_blks_written BIGINT,
    temp_blks_read BIGINT,
    temp_blks_written BIGINT,
    hit_percent DOUBLE PRECISION,
    total_io BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.shared_blks_read,
        pss.shared_blks_hit,
        pss.shared_blks_dirtied,
        pss.shared_blks_written,
        pss.temp_blks_read,
        pss.temp_blks_written,
        CASE
            WHEN (pss.shared_blks_hit + pss.shared_blks_read) > 0
            THEN 100.0 * pss.shared_blks_hit / (pss.shared_blks_hit + pss.shared_blks_read)
            ELSE 0.0
        END AS hit_percent,
        (pss.shared_blks_read + pss.shared_blks_written + pss.temp_blks_read + pss.temp_blks_written) AS total_io
    FROM pg_stat_statements pss
    WHERE (pss.shared_blks_read + pss.shared_blks_written) > 1000  -- Queries con mucho I/O
    ORDER BY (pss.shared_blks_read + pss.shared_blks_written + pss.temp_blks_read + pss.temp_blks_written) DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- Función para obtener estadísticas generales de la base de datos
CREATE OR REPLACE FUNCTION get_database_performance_stats()
RETURNS TABLE (
    total_queries BIGINT,
    total_calls BIGINT,
    total_exec_time DOUBLE PRECISION,
    avg_exec_time DOUBLE PRECISION,
    queries_per_second DOUBLE PRECISION,
    cache_hit_ratio DOUBLE PRECISION
) AS $$
DECLARE
    stats_reset_time TIMESTAMP WITH TIME ZONE;
    time_diff_seconds DOUBLE PRECISION;
BEGIN
    -- Obtener tiempo desde el último reset de estadísticas
    SELECT stats_reset INTO stats_reset_time FROM pg_stat_database WHERE datname = current_database();
    time_diff_seconds := EXTRACT(EPOCH FROM (now() - stats_reset_time));

    RETURN QUERY
    SELECT
        count(*)::BIGINT as total_queries,
        sum(pss.calls)::BIGINT as total_calls,
        sum(pss.total_exec_time) as total_exec_time,
        avg(pss.mean_exec_time) as avg_exec_time,
        CASE
            WHEN time_diff_seconds > 0
            THEN sum(pss.calls) / time_diff_seconds
            ELSE 0.0
        END as queries_per_second,
        CASE
            WHEN sum(pss.shared_blks_hit + pss.shared_blks_read) > 0
            THEN 100.0 * sum(pss.shared_blks_hit) / sum(pss.shared_blks_hit + pss.shared_blks_read)
            ELSE 0.0
        END as cache_hit_ratio
    FROM pg_stat_statements pss;
END;
$$ LANGUAGE plpgsql;

-- Función para análisis de índices no utilizados
CREATE OR REPLACE FUNCTION get_unused_indexes()
RETURNS TABLE (
    schemaname TEXT,
    tablename TEXT,
    indexname TEXT,
    index_size TEXT,
    index_scans BIGINT,
    tuples_read BIGINT,
    tuples_fetched BIGINT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        psi.schemaname::TEXT,
        psi.tablename::TEXT,
        psi.indexname::TEXT,
        pg_size_pretty(pg_relation_size(psi.indexrelid))::TEXT as index_size,
        psi.idx_scan as index_scans,
        psi.idx_tup_read as tuples_read,
        psi.idx_tup_fetch as tuples_fetched
    FROM pg_stat_user_indexes psi
    JOIN pg_index pi ON psi.indexrelid = pi.indexrelid
    WHERE psi.idx_scan < 100  -- Índices con menos de 100 scans
    AND NOT pi.indisunique     -- Excluir índices únicos
    AND NOT pi.indisprimary    -- Excluir claves primarias
    ORDER BY pg_relation_size(psi.indexrelid) DESC;
END;
$$ LANGUAGE plpgsql;

-- Función para detectar queries que podrían beneficiarse de índices
CREATE OR REPLACE FUNCTION suggest_missing_indexes()
RETURNS TABLE (
    query TEXT,
    calls BIGINT,
    total_time DOUBLE PRECISION,
    mean_time DOUBLE PRECISION,
    suggestion TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        pss.query,
        pss.calls,
        pss.total_exec_time,
        pss.mean_exec_time,
        CASE
            WHEN pss.query ILIKE '%WHERE%' AND pss.query ILIKE '%=%'
            THEN 'Consider adding index on WHERE clause columns'
            WHEN pss.query ILIKE '%ORDER BY%'
            THEN 'Consider adding index on ORDER BY columns'
            WHEN pss.query ILIKE '%GROUP BY%'
            THEN 'Consider adding index on GROUP BY columns'
            WHEN pss.query ILIKE '%JOIN%'
            THEN 'Consider adding indexes on JOIN columns'
            ELSE 'Review query for potential index optimization'
        END as suggestion
    FROM pg_stat_statements pss
    WHERE pss.mean_exec_time > 500  -- Queries lentas que podrían beneficiarse de índices
    AND pss.calls > 10              -- Con suficientes ejecuciones
    ORDER BY pss.mean_exec_time DESC
    LIMIT 20;
END;
$$ LANGUAGE plpgsql;

-- Función para monitoreo de conexiones
CREATE OR REPLACE FUNCTION get_connection_stats()
RETURNS TABLE (
    total_connections INTEGER,
    active_connections INTEGER,
    idle_connections INTEGER,
    idle_in_transaction INTEGER,
    max_connections INTEGER,
    connection_usage_percent DOUBLE PRECISION
) AS $$
DECLARE
    max_conn INTEGER;
BEGIN
    SELECT setting::INTEGER INTO max_conn FROM pg_settings WHERE name = 'max_connections';

    RETURN QUERY
    SELECT
        count(*)::INTEGER as total_connections,
        count(*) FILTER (WHERE state = 'active')::INTEGER as active_connections,
        count(*) FILTER (WHERE state = 'idle')::INTEGER as idle_connections,
        count(*) FILTER (WHERE state = 'idle in transaction')::INTEGER as idle_in_transaction,
        max_conn as max_connections,
        (count(*)::DOUBLE PRECISION / max_conn * 100) as connection_usage_percent
    FROM pg_stat_activity
    WHERE pid != pg_backend_pid();
END;
$$ LANGUAGE plpgsql;

-- Función para limpiar queries antiguas de pg_stat_statements
CREATE OR REPLACE FUNCTION cleanup_old_query_stats()
RETURNS VOID AS $$
BEGIN
    -- Esta función puede ser llamada periódicamente para limpiar estadísticas
    -- Por ahora solo registra que fue llamada
    RAISE NOTICE 'Query stats cleanup executed at %', now();
END;
$$ LANGUAGE plpgsql;

-- Crear vista para monitoreo fácil de performance
CREATE OR REPLACE VIEW query_performance_summary AS
SELECT
    'slow_queries' as metric_type,
    count(*) as count,
    avg(mean_exec_time) as avg_value,
    max(mean_exec_time) as max_value
FROM pg_stat_statements
WHERE mean_exec_time > 100

UNION ALL

SELECT
    'frequent_queries' as metric_type,
    count(*) as count,
    avg(calls) as avg_value,
    max(calls) as max_value
FROM pg_stat_statements
WHERE calls > 100

UNION ALL

SELECT
    'total_queries' as metric_type,
    count(*) as count,
    avg(total_exec_time) as avg_value,
    sum(total_exec_time) as max_value
FROM pg_stat_statements;

-- Comentarios para documentación
COMMENT ON FUNCTION get_slow_queries(INTEGER) IS 'Obtiene las queries más lentas basado en tiempo promedio de ejecución';
COMMENT ON FUNCTION get_frequent_queries(INTEGER) IS 'Obtiene las queries más frecuentemente ejecutadas';
COMMENT ON FUNCTION get_expensive_queries(INTEGER) IS 'Obtiene las queries que consumen más tiempo total de CPU';
COMMENT ON FUNCTION get_io_intensive_queries(INTEGER) IS 'Obtiene las queries que realizan más operaciones de I/O';
COMMENT ON FUNCTION get_database_performance_stats() IS 'Obtiene estadísticas generales de performance de la base de datos';
COMMENT ON FUNCTION get_unused_indexes() IS 'Identifica índices que no están siendo utilizados';
COMMENT ON FUNCTION suggest_missing_indexes() IS 'Sugiere índices que podrían mejorar el performance';
COMMENT ON FUNCTION get_connection_stats() IS 'Obtiene estadísticas de conexiones activas';
COMMENT ON VIEW query_performance_summary IS 'Vista resumen de métricas de performance de queries';

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE 'pg_stat_statements configuration completed successfully';
    RAISE NOTICE 'Available functions:';
    RAISE NOTICE '  - get_slow_queries(limit)';
    RAISE NOTICE '  - get_frequent_queries(limit)';
    RAISE NOTICE '  - get_expensive_queries(limit)';
    RAISE NOTICE '  - get_io_intensive_queries(limit)';
    RAISE NOTICE '  - get_database_performance_stats()';
    RAISE NOTICE '  - get_unused_indexes()';
    RAISE NOTICE '  - suggest_missing_indexes()';
    RAISE NOTICE '  - get_connection_stats()';
    RAISE NOTICE 'Available views:';
    RAISE NOTICE '  - query_performance_summary';
END $$;