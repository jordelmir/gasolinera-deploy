-- Database initialization script for Gasolinera JSM Platform
-- This script creates the necessary databases and users for all services

-- Create databases for each service
CREATE DATABASE IF NOT EXISTS auth_service_db;
CREATE DATABASE IF NOT EXISTS station_service_db;
CREATE DATABASE IF NOT EXISTS coupon_service_db;
CREATE DATABASE IF NOT EXISTS redemption_service_db;
CREATE DATABASE IF NOT EXISTS ad_engine_db;
CREATE DATABASE IF NOT EXISTS raffle_service_db;

-- Create service-specific users with appropriate permissions
DO $$
BEGIN
    -- Auth Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'auth_service_user') THEN
        CREATE USER auth_service_user WITH PASSWORD 'auth_service_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE auth_service_db TO auth_service_user;

    -- Station Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'station_service_user') THEN
        CREATE USER station_service_user WITH PASSWORD 'station_service_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE station_service_db TO station_service_user;

    -- Coupon Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'coupon_service_user') THEN
        CREATE USER coupon_service_user WITH PASSWORD 'coupon_service_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE coupon_service_db TO coupon_service_user;

    -- Redemption Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'redemption_service_user') THEN
        CREATE USER redemption_service_user WITH PASSWORD 'redemption_service_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE redemption_service_db TO redemption_service_user;

    -- Ad Engine User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ad_engine_user') THEN
        CREATE USER ad_engine_user WITH PASSWORD 'ad_engine_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE ad_engine_db TO ad_engine_user;

    -- Raffle Service User
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'raffle_service_user') THEN
        CREATE USER raffle_service_user WITH PASSWORD 'raffle_service_password';
    END IF;
    GRANT ALL PRIVILEGES ON DATABASE raffle_service_db TO raffle_service_user;
END
$$;

-- Create extensions that might be needed
\c auth_service_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c station_service_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis" CASCADE;

\c coupon_service_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

\c redemption_service_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c ad_engine_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c raffle_service_db;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Switch back to main database
\c gasolinera_db;

-- Create monitoring user for database metrics
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'monitoring_user') THEN
        CREATE USER monitoring_user WITH PASSWORD 'monitoring_password';
    END IF;
    GRANT pg_monitor TO monitoring_user;
END
$$;

-- Log successful initialization
INSERT INTO pg_stat_statements_info (dealloc) VALUES (0) ON CONFLICT DO NOTHING;