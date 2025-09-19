-- V1__Create_auth_schema.sql
-- Create the auth schema and initial tables for the authentication service

-- Create the auth schema
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- Set the search path to include the auth schema
SET search_path TO auth_schema, public;

-- Create the users table
CREATE TABLE auth_schema.users (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_phone_verified BOOLEAN NOT NULL DEFAULT false,
    last_login_at TIMESTAMP,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    account_locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'EMPLOYEE', 'STATION_ADMIN', 'SYSTEM_ADMIN')),
    CONSTRAINT chk_users_phone_format CHECK (phone_number ~ '^\+?[1-9]\d{1,14}$'),
    CONSTRAINT chk_users_first_name_length CHECK (LENGTH(first_name) >= 2),
    CONSTRAINT chk_users_last_name_length CHECK (LENGTH(last_name) >= 2),
    CONSTRAINT chk_users_failed_attempts CHECK (failed_login_attempts >= 0)
);

-- Create indexes for performance
CREATE INDEX idx_users_phone_number ON auth_schema.users(phone_number);
CREATE INDEX idx_users_role ON auth_schema.users(role);
CREATE INDEX idx_users_active ON auth_schema.users(is_active);
CREATE INDEX idx_users_created_at ON auth_schema.users(created_at);
CREATE INDEX idx_users_last_login ON auth_schema.users(last_login_at);
CREATE INDEX idx_users_phone_verified ON auth_schema.users(is_phone_verified);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION auth_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON auth_schema.users
    FOR EACH ROW
    EXECUTE FUNCTION auth_schema.update_updated_at_column();

-- Create OTP sessions table for phone verification
CREATE TABLE auth_schema.otp_sessions (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(20) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    session_token VARCHAR(255) NOT NULL UNIQUE,
    purpose VARCHAR(20) NOT NULL DEFAULT 'LOGIN',
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    expires_at TIMESTAMP NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_otp_purpose CHECK (purpose IN ('LOGIN', 'REGISTRATION', 'PASSWORD_RESET')),
    CONSTRAINT chk_otp_code_format CHECK (otp_code ~ '^\d{6}$'),
    CONSTRAINT chk_otp_attempts CHECK (attempts >= 0 AND attempts <= max_attempts)
);

-- Create indexes for OTP sessions
CREATE INDEX idx_otp_sessions_phone ON auth_schema.otp_sessions(phone_number);
CREATE INDEX idx_otp_sessions_token ON auth_schema.otp_sessions(session_token);
CREATE INDEX idx_otp_sessions_expires ON auth_schema.otp_sessions(expires_at);
CREATE INDEX idx_otp_sessions_purpose ON auth_schema.otp_sessions(purpose);

-- Create refresh tokens table for JWT token management
CREATE TABLE auth_schema.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address INET,
    user_agent TEXT,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP
);

-- Create indexes for refresh tokens
CREATE INDEX idx_refresh_tokens_user_id ON auth_schema.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON auth_schema.refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_expires ON auth_schema.refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_revoked ON auth_schema.refresh_tokens(revoked);

-- Create audit log table for authentication events
CREATE TABLE auth_schema.auth_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES auth_schema.users(id) ON DELETE SET NULL,
    phone_number VARCHAR(20),
    event_type VARCHAR(50) NOT NULL,
    event_details JSONB,
    ip_address INET,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    session_id VARCHAR(255),
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_auth_audit_event_type CHECK (
        event_type IN (
            'LOGIN_ATTEMPT', 'LOGIN_SUCCESS', 'LOGIN_FAILURE',
            'LOGOUT', 'REGISTRATION', 'OTP_SENT', 'OTP_VERIFIED',
            'TOKEN_REFRESH', 'TOKEN_REVOKED', 'ACCOUNT_LOCKED',
            'ACCOUNT_UNLOCKED', 'PASSWORD_RESET', 'PROFILE_UPDATE'
        )
    )
);

-- Create indexes for audit log
CREATE INDEX idx_auth_audit_user_id ON auth_schema.auth_audit_log(user_id);
CREATE INDEX idx_auth_audit_phone ON auth_schema.auth_audit_log(phone_number);
CREATE INDEX idx_auth_audit_event_type ON auth_schema.auth_audit_log(event_type);
CREATE INDEX idx_auth_audit_created_at ON auth_schema.auth_audit_log(created_at);
CREATE INDEX idx_auth_audit_success ON auth_schema.auth_audit_log(success);
CREATE INDEX idx_auth_audit_session_id ON auth_schema.auth_audit_log(session_id);
CREATE INDEX idx_auth_audit_correlation_id ON auth_schema.auth_audit_log(correlation_id);

-- Create a view for active users
CREATE VIEW auth_schema.active_users AS
SELECT
    id,
    phone_number,
    first_name,
    last_name,
    role,
    is_phone_verified,
    last_login_at,
    created_at,
    updated_at
FROM auth_schema.users
WHERE is_active = true;

-- Create a view for user statistics
CREATE VIEW auth_schema.user_statistics AS
SELECT
    role,
    COUNT(*) as total_users,
    COUNT(CASE WHEN is_active THEN 1 END) as active_users,
    COUNT(CASE WHEN is_phone_verified THEN 1 END) as verified_users,
    COUNT(CASE WHEN last_login_at > CURRENT_TIMESTAMP - INTERVAL '30 days' THEN 1 END) as active_last_30_days
FROM auth_schema.users
GROUP BY role;

-- Insert default system admin user (password will be set via application)
INSERT INTO auth_schema.users (
    phone_number,
    first_name,
    last_name,
    role,
    is_active,
    is_phone_verified
) VALUES (
    '+1234567890',
    'System',
    'Administrator',
    'SYSTEM_ADMIN',
    true,
    true
) ON CONFLICT (phone_number) DO NOTHING;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA auth_schema TO auth_service_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA auth_schema TO auth_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA auth_schema TO auth_service_user;