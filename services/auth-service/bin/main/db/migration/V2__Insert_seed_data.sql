-- V2__Insert_seed_data.sql
-- Seed data for Auth Service development environment

-- Insert test users for different roles
INSERT INTO auth_schema.users (
    phone_number, first_name, last_name, role, is_active, is_phone_verified
) VALUES
-- System Admin
('+525555000001', 'Admin', 'Sistema', 'SYSTEM_ADMIN', true, true),

-- Station Admins
('+525555000002', 'Carlos', 'Rodríguez', 'STATION_ADMIN', true, true),
('+525555000003', 'María', 'González', 'STATION_ADMIN', true, true),
('+525555000004', 'Juan', 'Pérez', 'STATION_ADMIN', true, true),

-- Employees
('+525555000005', 'Ana', 'López', 'EMPLOYEE', true, true),
('+525555000006', 'Pedro', 'Martínez', 'EMPLOYEE', true, true),
('+525555000007', 'Laura', 'Hernández', 'EMPLOYEE', true, true),
('+525555000008', 'Miguel', 'Torres', 'EMPLOYEE', true, true),
('+525555000009', 'Sofia', 'Ramírez', 'EMPLOYEE', true, true),
('+525555000010', 'Diego', 'Morales', 'EMPLOYEE', true, true),

-- Test Customers
('+525555001001', 'Cliente', 'Uno', 'CUSTOMER', true, true),
('+525555001002', 'Cliente', 'Dos', 'CUSTOMER', true, true),
('+525555001003', 'Cliente', 'Tres', 'CUSTOMER', true, true),
('+525555001004', 'Cliente', 'Cuatro', 'CUSTOMER', true, true),
('+525555001005', 'Cliente', 'Cinco', 'CUSTOMER', true, true),
('+525555001006', 'Cliente', 'Seis', 'CUSTOMER', true, true),
('+525555001007', 'Cliente', 'Siete', 'CUSTOMER', true, true),
('+525555001008', 'Cliente', 'Ocho', 'CUSTOMER', true, true),
('+525555001009', 'Cliente', 'Nueve', 'CUSTOMER', true, true),
('+525555001010', 'Cliente', 'Diez', 'CUSTOMER', true, true),

-- VIP Customers (for testing premium features)
('+525555002001', 'VIP', 'Cliente', 'CUSTOMER', true, true),
('+525555002002', 'Premium', 'Usuario', 'CUSTOMER', true, true),

-- Test users with different verification states
('+525555003001', 'No', 'Verificado', 'CUSTOMER', true, false),
('+525555003002', 'Inactivo', 'Usuario', 'CUSTOMER', false, true)

ON CONFLICT (phone_number) DO NOTHING;

-- Insert some test OTP sessions for development
INSERT INTO auth_schema.otp_sessions (
    phone_number, otp_code, session_token, purpose, attempts, max_attempts, expires_at
) VALUES
('+525555999001', '123456', 'test-session-token-1', 'LOGIN', 0, 3, CURRENT_TIMESTAMP + INTERVAL '10 minutes'),
('+525555999002', '654321', 'test-session-token-2', 'REGISTRATION', 1, 3, CURRENT_TIMESTAMP + INTERVAL '10 minutes'),
('+525555999003', '111111', 'test-session-token-3', 'LOGIN', 0, 3, CURRENT_TIMESTAMP + INTERVAL '5 minutes')
ON CONFLICT (session_token) DO NOTHING;

-- Insert some audit log entries for testing
INSERT INTO auth_schema.auth_audit_log (
    user_id, phone_number, event_type, event_details, ip_address, user_agent, success, session_id, correlation_id, created_at
) VALUES
(1, '+525555000001', 'LOGIN_SUCCESS', '{"login_method": "otp"}', '192.168.1.100', 'Mozilla/5.0 Test Browser', true, 'session-1', 'corr-1', CURRENT_TIMESTAMP - INTERVAL '1 hour'),
(2, '+525555000002', 'LOGIN_SUCCESS', '{"login_method": "otp"}', '192.168.1.101', 'Mozilla/5.0 Test Browser', true, 'session-2', 'corr-2', CURRENT_TIMESTAMP - INTERVAL '2 hours'),
(11, '+525555001001', 'REGISTRATION', '{"registration_method": "phone"}', '192.168.1.102', 'Mobile App', true, 'session-3', 'corr-3', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(12, '+525555001002', 'LOGIN_FAILURE', '{"reason": "invalid_otp"}', '192.168.1.103', 'Mobile App', false, 'session-4', 'corr-4', CURRENT_TIMESTAMP - INTERVAL '30 minutes'),
(13, '+525555001003', 'OTP_SENT', '{"phone_number": "+525555001003"}', '192.168.1.104', 'Web Browser', true, 'session-5', 'corr-5', CURRENT_TIMESTAMP - INTERVAL '15 minutes');

-- Update some users with last login times
UPDATE auth_schema.users
SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '1 hour'
WHERE phone_number IN ('+525555000001', '+525555000002', '+525555001001', '+525555001002');

UPDATE auth_schema.users
SET last_login_at = CURRENT_TIMESTAMP - INTERVAL '1 day'
WHERE phone_number IN ('+525555001003', '+525555001004', '+525555001005');

-- Add some failed login attempts for testing lockout functionality
UPDATE auth_schema.users
SET failed_login_attempts = 2
WHERE phone_number = '+525555003001';

UPDATE auth_schema.users
SET failed_login_attempts = 5, account_locked_until = CURRENT_TIMESTAMP + INTERVAL '30 minutes'
WHERE phone_number = '+525555003002';