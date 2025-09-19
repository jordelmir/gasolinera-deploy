-- V2__Insert_seed_data.sql
-- Seed data for Coupon Service development environment

-- Insert test campaigns
INSERT INTO coupon_schema.campaigns (
    name, description, campaign_code, discount_type, discount_value,
    minimum_purchase, maximum_discount, usage_limit_per_user, total_usage_limit,
    raffle_tickets_per_coupon, start_date, end_date, is_active,
    target_audience, applicable_stations, terms_and_conditions, created_by
) VALUES
-- Welcome Campaign
(
    'Bienvenida Nuevos Clientes',
    'Descuento especial para nuevos clientes de Gasolinera JSM',
    'WELCOME2024',
    'PERCENTAGE',
    15.00,
    100.00,
    50.00,
    1,
    1000,
    3,
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    true,
    'NEW_CUSTOMERS',
    'ALL',
    'Válido solo para nuevos clientes. Un uso por cliente. No acumulable con otras promociones.',
    'system'
),

-- Summer Campaign
(
    'Verano JSM 2024',
    'Promoción especial de verano con grandes descuentos',
    'SUMMER2024',
    'FIXED_AMOUNT',
    25.00,
    150.00,
    25.00,
    3,
    5000,
    2,
    CURRENT_TIMESTAMP - INTERVAL '15 days',
    CURRENT_TIMESTAMP + INTERVAL '45 days',
    true,
    'ALL_CUSTOMERS',
    'ALL',
    'Válido durante la temporada de verano. Máximo 3 usos por cliente.',
    'system'
),

-- Premium Fuel Campaign
(
    'Combustible Premium',
    'Descuento especial en combustible premium',
    'PREMIUM2024',
    'PERCENTAGE',
    10.00,
    200.00,
    40.00,
    5,
    2000,
    4,
    CURRENT_TIMESTAMP - INTERVAL '10 days',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    true,
    'PREMIUM_CUSTOMERS',
    '["JSM002", "JSM007"]',
    'Válido solo en estaciones premium. Aplicable únicamente a combustible premium.',
    'system'
),

-- Weekend Special
(
    'Especial Fin de Semana',
    'Descuentos especiales para fines de semana',
    'WEEKEND2024',
    'PERCENTAGE',
    20.00,
    80.00,
    30.00,
    2,
    3000,
    1,
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP + INTERVAL '90 days',
    true,
    'ALL_CUSTOMERS',
    'ALL',
    'Válido solo sábados y domingos. Máximo 2 usos por cliente.',
    'system'
),

-- Loyalty Program
(
    'Programa de Lealtad',
    'Recompensas para clientes frecuentes',
    'LOYALTY2024',
    'PERCENTAGE',
    25.00,
    120.00,
    60.00,
    10,
    NULL,
    5,
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP + INTERVAL '180 days',
    true,
    'LOYAL_CUSTOMERS',
    'ALL',
    'Para clientes con más de 10 transacciones previas. Uso ilimitado.',
    'system'
),

-- Student Discount
(
    'Descuento Estudiantes',
    'Descuento especial para estudiantes',
    'STUDENT2024',
    'FIXED_AMOUNT',
    15.00,
    75.00,
    15.00,
    4,
    1500,
    2,
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    CURRENT_TIMESTAMP + INTERVAL '120 days',
    true,
    'STUDENTS',
    'ALL',
    'Válido con credencial estudiantil vigente. Máximo 4 usos por mes.',
    'system'
),

-- Corporate Campaign
(
    'Empresarial JSM',
    'Descuentos para empresas y flotas',
    'CORPORATE2024',
    'PERCENTAGE',
    12.00,
    500.00,
    100.00,
    20,
    10000,
    1,
    CURRENT_TIMESTAMP - INTERVAL '60 days',
    CURRENT_TIMESTAMP + INTERVAL '365 days',
    true,
    'CORPORATE',
    'ALL',
    'Válido para empresas registradas. Facturación disponible.',
    'system'
),

-- Test campaigns with different statuses
(
    'Campaña Expirada',
    'Campaña de prueba que ya expiró',
    'EXPIRED2024',
    'PERCENTAGE',
    30.00,
    100.00,
    50.00,
    1,
    100,
    2,
    CURRENT_TIMESTAMP - INTERVAL '90 days',
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    false,
    'ALL_CUSTOMERS',
    'ALL',
    'Campaña de prueba expirada.',
    'system'
),

(
    'Campaña Futura',
    'Campaña que iniciará en el futuro',
    'FUTURE2024',
    'FIXED_AMOUNT',
    20.00,
    100.00,
    20.00,
    2,
    1000,
    3,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP + INTERVAL '90 days',
    true,
    'ALL_CUSTOMERS',
    'ALL',
    'Campaña que iniciará próximamente.',
    'system'
),

(
    'Campaña Inactiva',
    'Campaña desactivada para pruebas',
    'INACTIVE2024',
    'PERCENTAGE',
    15.00,
    80.00,
    25.00,
    3,
    500,
    2,
    CURRENT_TIMESTAMP - INTERVAL '10 days',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    false,
    'ALL_CUSTOMERS',
    'ALL',
    'Campaña desactivada temporalmente.',
    'system'
)

ON CONFLICT (campaign_code) DO NOTHING;

-- Generate test coupons for active campaigns
INSERT INTO coupon_schema.coupons (
    coupon_code, campaign_id, qr_code, qr_signature, status, discount_amount,
    raffle_tickets, valid_from, valid_until, usage_count, max_usage,
    assigned_user_id, batch_id, metadata, created_by
) VALUES
-- Welcome Campaign Coupons
('WELCOME001', 1, 'QR_WELCOME001_DATA', 'SIG_WELCOME001', 'ACTIVE', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 0, 1, 11, 'BATCH_WELCOME_001', '{"campaign": "welcome", "user_type": "new"}', 'system'),
('WELCOME002', 1, 'QR_WELCOME002_DATA', 'SIG_WELCOME002', 'ACTIVE', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 0, 1, 12, 'BATCH_WELCOME_001', '{"campaign": "welcome", "user_type": "new"}', 'system'),
('WELCOME003', 1, 'QR_WELCOME003_DATA', 'SIG_WELCOME003', 'ACTIVE', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 0, 1, 13, 'BATCH_WELCOME_001', '{"campaign": "welcome", "user_type": "new"}', 'system'),
('WELCOME004', 1, 'QR_WELCOME004_DATA', 'SIG_WELCOME004', 'USED', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 1, 1, 14, 'BATCH_WELCOME_001', '{"campaign": "welcome", "user_type": "new"}', 'system'),
('WELCOME005', 1, 'QR_WELCOME005_DATA', 'SIG_WELCOME005', 'ACTIVE', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 0, 1, 15, 'BATCH_WELCOME_001', '{"campaign": "welcome", "user_type": "new"}', 'system'),

-- Summer Campaign Coupons
('SUMMER001', 2, 'QR_SUMMER001_DATA', 'SIG_SUMMER001', 'ACTIVE', 25.00, 2, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 0, 3, 11, 'BATCH_SUMMER_001', '{"campaign": "summer", "season": "2024"}', 'system'),
('SUMMER002', 2, 'QR_SUMMER002_DATA', 'SIG_SUMMER002', 'ACTIVE', 25.00, 2, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 1, 3, 12, 'BATCH_SUMMER_001', '{"campaign": "summer", "season": "2024"}', 'system'),
('SUMMER003', 2, 'QR_SUMMER003_DATA', 'SIG_SUMMER003', 'ACTIVE', 25.00, 2, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 0, 3, 13, 'BATCH_SUMMER_001', '{"campaign": "summer", "season": "2024"}', 'system'),
('SUMMER004', 2, 'QR_SUMMER004_DATA', 'SIG_SUMMER004', 'USED', 25.00, 2, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 3, 3, 16, 'BATCH_SUMMER_001', '{"campaign": "summer", "season": "2024"}', 'system'),

-- Premium Fuel Coupons
('PREMIUM001', 3, 'QR_PREMIUM001_DATA', 'SIG_PREMIUM001', 'ACTIVE', 20.00, 4, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '60 days', 0, 5, 21, 'BATCH_PREMIUM_001', '{"campaign": "premium", "fuel_type": "premium"}', 'system'),
('PREMIUM002', 3, 'QR_PREMIUM002_DATA', 'SIG_PREMIUM002', 'ACTIVE', 20.00, 4, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '60 days', 2, 5, 22, 'BATCH_PREMIUM_001', '{"campaign": "premium", "fuel_type": "premium"}', 'system'),
('PREMIUM003', 3, 'QR_PREMIUM003_DATA', 'SIG_PREMIUM003', 'ACTIVE', 20.00, 4, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '60 days', 0, 5, 11, 'BATCH_PREMIUM_001', '{"campaign": "premium", "fuel_type": "premium"}', 'system'),

-- Weekend Special Coupons
('WEEKEND001', 4, 'QR_WEEKEND001_DATA', 'SIG_WEEKEND001', 'ACTIVE', 16.00, 1, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '90 days', 0, 2, 17, 'BATCH_WEEKEND_001', '{"campaign": "weekend", "days": ["saturday", "sunday"]}', 'system'),
('WEEKEND002', 4, 'QR_WEEKEND002_DATA', 'SIG_WEEKEND002', 'ACTIVE', 16.00, 1, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '90 days', 1, 2, 18, 'BATCH_WEEKEND_001', '{"campaign": "weekend", "days": ["saturday", "sunday"]}', 'system'),
('WEEKEND003', 4, 'QR_WEEKEND003_DATA', 'SIG_WEEKEND003', 'ACTIVE', 16.00, 1, CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '90 days', 0, 2, 19, 'BATCH_WEEKEND_001', '{"campaign": "weekend", "days": ["saturday", "sunday"]}', 'system'),

-- Loyalty Program Coupons
('LOYALTY001', 5, 'QR_LOYALTY001_DATA', 'SIG_LOYALTY001', 'ACTIVE', 30.00, 5, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '180 days', 3, 10, 21, 'BATCH_LOYALTY_001', '{"campaign": "loyalty", "tier": "gold"}', 'system'),
('LOYALTY002', 5, 'QR_LOYALTY002_DATA', 'SIG_LOYALTY002', 'ACTIVE', 30.00, 5, CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '180 days', 1, 10, 22, 'BATCH_LOYALTY_001', '{"campaign": "loyalty", "tier": "gold"}', 'system'),

-- Student Discount Coupons
('STUDENT001', 6, 'QR_STUDENT001_DATA', 'SIG_STUDENT001', 'ACTIVE', 15.00, 2, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '120 days', 0, 4, 20, 'BATCH_STUDENT_001', '{"campaign": "student", "verification": "required"}', 'system'),
('STUDENT002', 6, 'QR_STUDENT002_DATA', 'SIG_STUDENT002', 'ACTIVE', 15.00, 2, CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '120 days', 2, 4, 11, 'BATCH_STUDENT_001', '{"campaign": "student", "verification": "required"}', 'system'),

-- Corporate Coupons
('CORP001', 7, 'QR_CORP001_DATA', 'SIG_CORP001', 'ACTIVE', 60.00, 1, CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP + INTERVAL '365 days', 5, 20, 21, 'BATCH_CORP_001', '{"campaign": "corporate", "company": "test_corp"}', 'system'),

-- Test coupons with different statuses
('EXPIRED001', 8, 'QR_EXPIRED001_DATA', 'SIG_EXPIRED001', 'EXPIRED', 30.00, 2, CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '30 days', 0, 1, 11, 'BATCH_EXPIRED_001', '{"campaign": "expired", "status": "test"}', 'system'),
('CANCELLED001', 1, 'QR_CANCELLED001_DATA', 'SIG_CANCELLED001', 'CANCELLED', 15.00, 3, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '30 days', 0, 1, 12, 'BATCH_WELCOME_001', '{"campaign": "welcome", "status": "cancelled"}', 'system'),
('SUSPENDED001', 2, 'QR_SUSPENDED001_DATA', 'SIG_SUSPENDED001', 'SUSPENDED', 25.00, 2, CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days', 0, 3, 13, 'BATCH_SUMMER_001', '{"campaign": "summer", "status": "suspended"}', 'system')

ON CONFLICT (coupon_code) DO NOTHING;

-- Insert coupon usage history for used coupons
INSERT INTO coupon_schema.coupon_usage_history (
    coupon_id, user_id, station_id, employee_id, redemption_id,
    original_amount, discount_amount, final_amount, raffle_tickets_awarded,
    transaction_reference, payment_method, validation_method, used_at,
    session_id, correlation_id
) VALUES
-- Welcome coupon usage
(4, 14, 1, 1, 1001, 150.00, 15.00, 135.00, 3, 'TXN_WELCOME_001', 'CREDIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '2 days', 'session_001', 'corr_001'),

-- Summer coupon usage
(8, 12, 2, 5, 1002, 200.00, 25.00, 175.00, 2, 'TXN_SUMMER_001', 'CASH', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '5 days', 'session_002', 'corr_002'),
(8, 16, 3, 9, 1003, 180.00, 25.00, 155.00, 2, 'TXN_SUMMER_002', 'DEBIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '3 days', 'session_003', 'corr_003'),
(8, 16, 1, 2, 1004, 160.00, 25.00, 135.00, 2, 'TXN_SUMMER_003', 'CREDIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '1 day', 'session_004', 'corr_004'),

-- Premium coupon usage
(11, 22, 2, 6, 1005, 300.00, 20.00, 280.00, 4, 'TXN_PREMIUM_001', 'CREDIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '4 days', 'session_005', 'corr_005'),
(11, 22, 7, 14, 1006, 250.00, 20.00, 230.00, 4, 'TXN_PREMIUM_002', 'DEBIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '1 day', 'session_006', 'corr_006'),

-- Weekend coupon usage
(15, 18, 4, 11, 1007, 120.00, 16.00, 104.00, 1, 'TXN_WEEKEND_001', 'CASH', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '2 days', 'session_007', 'corr_007'),

-- Loyalty coupon usage
(18, 21, 1, 1, 1008, 200.00, 30.00, 170.00, 5, 'TXN_LOYALTY_001', 'CREDIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '10 days', 'session_008', 'corr_008'),
(18, 21, 2, 5, 1009, 180.00, 30.00, 150.00, 5, 'TXN_LOYALTY_002', 'DEBIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '7 days', 'session_009', 'corr_009'),
(18, 21, 3, 9, 1010, 220.00, 30.00, 190.00, 5, 'TXN_LOYALTY_003', 'CREDIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '3 days', 'session_010', 'corr_010'),

-- Student coupon usage
(21, 11, 5, 12, 1011, 100.00, 15.00, 85.00, 2, 'TXN_STUDENT_001', 'DEBIT_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '8 days', 'session_011', 'corr_011'),
(21, 11, 6, 13, 1012, 90.00, 15.00, 75.00, 2, 'TXN_STUDENT_002', 'CASH', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '5 days', 'session_012', 'corr_012'),

-- Corporate coupon usage
(23, 21, 1, 1, 1013, 800.00, 60.00, 740.00, 1, 'TXN_CORP_001', 'CORPORATE_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '15 days', 'session_013', 'corr_013'),
(23, 21, 2, 5, 1014, 600.00, 60.00, 540.00, 1, 'TXN_CORP_002', 'CORPORATE_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '12 days', 'session_014', 'corr_014'),
(23, 21, 7, 14, 1015, 750.00, 60.00, 690.00, 1, 'TXN_CORP_003', 'CORPORATE_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '8 days', 'session_015', 'corr_015'),
(23, 21, 1, 2, 1016, 550.00, 60.00, 490.00, 1, 'TXN_CORP_004', 'CORPORATE_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '5 days', 'session_016', 'corr_016'),
(23, 21, 3, 9, 1017, 650.00, 60.00, 590.00, 1, 'TXN_CORP_005', 'CORPORATE_CARD', 'QR_SCAN', CURRENT_TIMESTAMP - INTERVAL '2 days', 'session_017', 'corr_017');

-- Insert validation attempts (including failed ones for fraud detection testing)
INSERT INTO coupon_schema.coupon_validation_attempts (
    coupon_code, user_id, station_id, employee_id, validation_result,
    failure_reason, ip_address, user_agent, attempted_at, session_id, correlation_id
) VALUES
-- Successful validations
('WELCOME001', 11, 1, 1, 'SUCCESS', NULL, '192.168.1.101', 'Mobile App v1.0', CURRENT_TIMESTAMP - INTERVAL '1 hour', 'session_101', 'corr_101'),
('SUMMER001', 11, 2, 5, 'SUCCESS', NULL, '192.168.1.101', 'Mobile App v1.0', CURRENT_TIMESTAMP - INTERVAL '2 hours', 'session_102', 'corr_102'),

-- Failed validations
('INVALID001', 11, 1, 1, 'INVALID_CODE', 'Coupon code does not exist', '192.168.1.101', 'Mobile App v1.0', CURRENT_TIMESTAMP - INTERVAL '30 minutes', 'session_103', 'corr_103'),
('EXPIRED001', 12, 2, 5, 'EXPIRED', 'Coupon has expired', '192.168.1.102', 'Web Browser', CURRENT_TIMESTAMP - INTERVAL '1 day', 'session_104', 'corr_104'),
('WELCOME004', 15, 3, 9, 'ALREADY_USED', 'Coupon has already been used', '192.168.1.103', 'Mobile App v1.0', CURRENT_TIMESTAMP - INTERVAL '3 hours', 'session_105', 'corr_105'),
('SUSPENDED001', 13, 1, 2, 'SUSPENDED', 'Coupon is temporarily suspended', '192.168.1.104', 'Web Browser', CURRENT_TIMESTAMP - INTERVAL '4 hours', 'session_106', 'corr_106'),

-- Fraud detection attempts
('WELCOME001', 99, 1, 1, 'FRAUD_DETECTED', 'Suspicious user behavior detected', '10.0.0.1', 'Suspicious Bot', CURRENT_TIMESTAMP - INTERVAL '6 hours', 'session_107', 'corr_107'),
('SUMMER001', 11, 99, 1, 'FRAUD_DETECTED', 'Invalid station ID', '192.168.1.101', 'Mobile App v1.0', CURRENT_TIMESTAMP - INTERVAL '8 hours', 'session_108', 'corr_108');

-- Update campaign usage counts based on actual usage
UPDATE coupon_schema.campaigns
SET current_usage_count = (
    SELECT COUNT(*)
    FROM coupon_schema.coupon_usage_history cuh
    JOIN coupon_schema.coupons c ON cuh.coupon_id = c.id
    WHERE c.campaign_id = campaigns.id
);