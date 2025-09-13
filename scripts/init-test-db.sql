-- Test Database Initialization Script
-- Creates necessary schemas and test data for integration tests

-- Create schemas for each service
CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS coupon_service;
CREATE SCHEMA IF NOT EXISTS station_service;
CREATE SCHEMA IF NOT EXISTS redemption_service;
CREATE SCHEMA IF NOT EXISTS ad_engine;
CREATE SCHEMA IF NOT EXISTS raffle_service;

-- Grant permissions to test user
GRANT ALL PRIVILEGES ON SCHEMA auth_service TO test_user;
GRANT ALL PRIVILEGES ON SCHEMA coupon_service TO test_user;
GRANT ALL PRIVILEGES ON SCHEMA station_service TO test_user;
GRANT ALL PRIVILEGES ON SCHEMA redemption_service TO test_user;
GRANT ALL PRIVILEGES ON SCHEMA ad_engine TO test_user;
GRANT ALL PRIVILEGES ON SCHEMA raffle_service TO test_user;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set default search path
ALTER DATABASE gasolinera_jsm_test SET search_path TO auth_service, coupon_service, station_service, redemption_service, ad_engine, raffle_service, public;

-- Create test-specific functions
CREATE OR REPLACE FUNCTION generate_test_uuid() RETURNS UUID AS $$
BEGIN
    RETURN uuid_generate_v4();
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_test_timestamp() RETURNS TIMESTAMP AS $$
BEGIN
    RETURN NOW();
END;
$$ LANGUAGE plpgsql;

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE 'Test database initialization completed successfully';
END $$;