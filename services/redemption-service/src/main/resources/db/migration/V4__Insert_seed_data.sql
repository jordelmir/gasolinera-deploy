-- V4__Insert_seed_data.sql
-- Seed data for Redemption Service development environment

-- Insert test redemptions
INSERT INTO redemption_schema.redemptions (
    user_id, station_id, employee_id, coupon_id, campaign_id,
    transaction_reference, status, qr_code, coupon_code,
    fuel_type, fuel_quantity, fuel_price_per_unit,
    purchase_amount, discount_amount, final_amount, raffle_tickets_earned,
    payment_method, payment_reference, validation_timestamp,
    redemption_timestamp, completion_timestamp, created_by
) VALUES
-- Completed redemptions
(14, 1, 1, 4, 1, 'TXN_WELCOME_001', 'COMPLETED', 'QR_WELCOME004_DATA', 'WELCOME004',
 'Premium', 25.50, 22.50, 150.00, 15.00, 135.00, 3,
 'CREDIT_CARD', 'CC_REF_001', CURRENT_TIMESTAMP - INTERVAL '2 days',
 CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', 'system'),

(12, 2, 5, 8, 2, 'TXN_SUMMER_001', 'COMPLETED', 'QR_SUMMER002_DATA', 'SUMMER002',
 'Magna', 35.20, 20.80, 200.00, 25.00, 175.00, 2,
 'CASH', NULL, CURRENT_TIMESTAMP - INTERVAL '5 days',
 CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', 'system'),

(16, 3, 9, 8, 2, 'TXN_SUMMER_002', 'COMPLETED', 'QR_SUMMER002_DATA', 'SUMMER002',
 'Diesel', 28.75, 21.20, 180.00, 25.00, 155.00, 2,
 'DEBIT_CARD', 'DB_REF_001', CURRENT_TIMESTAMP - INTERVAL '3 days',
 CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days', 'system')-- More c
ompleted redemptions
,(16, 1, 2, 8, 2, 'TXN_SUMMER_003', 'COMPLETED', 'QR_SUMMER002_DATA', 'SUMMER002',
 'Premium', 22.40, 22.50, 160.00, 25.00, 135.00, 2,
 'CREDIT_CARD', 'CC_REF_002', CURRENT_TIMESTAMP - INTERVAL '1 day',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', 'system'),

(22, 2, 6, 11, 3, 'TXN_PREMIUM_001', 'COMPLETED', 'QR_PREMIUM002_DATA', 'PREMIUM002',
 'Premium', 42.80, 22.50, 300.00, 20.00, 280.00, 4,
 'CREDIT_CARD', 'CC_REF_003', CURRENT_TIMESTAMP - INTERVAL '4 days',
 CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days', 'system'),

(22, 7, 14, 11, 3, 'TXN_PREMIUM_002', 'COMPLETED', 'QR_PREMIUM002_DATA', 'PREMIUM002',
 'Premium', 38.20, 22.50, 250.00, 20.00, 230.00, 4,
 'DEBIT_CARD', 'DB_REF_002', CURRENT_TIMESTAMP - INTERVAL '1 day',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', 'system'),

-- Pending redemptions
(11, 1, 1, 1, 1, 'TXN_PENDING_001', 'PENDING', 'QR_WELCOME001_DATA', 'WELCOME001',
 'Magna', 30.00, 20.80, 120.00, 15.00, 105.00, 3,
 'CREDIT_CARD', 'CC_REF_004', CURRENT_TIMESTAMP - INTERVAL '1 hour',
 NULL, NULL, 'system'),

(13, 2, 5, 3, 2, 'TXN_PENDING_002', 'PENDING', 'QR_SUMMER003_DATA', 'SUMMER003',
 'Diesel', 25.00, 21.20, 140.00, 25.00, 115.00, 2,
 'DEBIT_CARD', 'DB_REF_003', CURRENT_TIMESTAMP - INTERVAL '30 minutes',
 NULL, NULL, 'system'),

-- In progress redemptions
(15, 3, 9, 5, 1, 'TXN_PROGRESS_001', 'IN_PROGRESS', 'QR_WELCOME005_DATA', 'WELCOME005',
 'Premium', 20.00, 22.50, 110.00, 15.00, 95.00, 3,
 'CASH', NULL, CURRENT_TIMESTAMP - INTERVAL '2 hours',
 CURRENT_TIMESTAMP - INTERVAL '10 minutes', NULL, 'system'),

-- Failed redemptions
(17, 4, 11, 99, 1, 'TXN_FAILED_001', 'FAILED', 'QR_INVALID_DATA', 'INVALID001',
 'Magna', 25.00, 20.80, 100.00, 0.00, 100.00, 0,
 'CREDIT_CARD', 'CC_REF_005', CURRENT_TIMESTAMP - INTERVAL '6 hours',
 CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours', 'system'),

-- Cancelled redemptions
(18, 5, 12, 25, 1, 'TXN_CANCELLED_001', 'CANCELLED', 'QR_CANCELLED001_DATA', 'CANCELLED001',
 'Diesel', 30.00, 21.20, 130.00, 15.00, 115.00, 3,
 'DEBIT_CARD', 'DB_REF_004', CURRENT_TIMESTAMP - INTERVAL '8 hours',
 CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP - INTERVAL '8 hours', 'system')

ON CONFLICT (transaction_reference) DO NOTHING;

-- Insert fraud detection logs
INSERT INTO redemption_schema.fraud_detection_log (
    redemption_id, user_id, station_id, coupon_code, fraud_type,
    risk_score, detection_rules, additional_data, action_taken, created_at
) VALUES
(11, 17, 4, 'INVALID001', 'DUPLICATE_REDEMPTION', 85,
 ARRAY['duplicate_transaction', 'suspicious_timing'],
 '{"previous_attempt": "2024-01-01T10:00:00Z", "time_diff_minutes": 5}',
 'BLOCKED', CURRENT_TIMESTAMP - INTERVAL '6 hours'),

(12, 18, 5, 'CANCELLED001', 'SUSPICIOUS_PATTERN', 70,
 ARRAY['unusual_location', 'high_frequency'],
 '{"usual_stations": [1, 2, 3], "attempts_last_hour": 5}',
 'FLAGGED', CURRENT_TIMESTAMP - INTERVAL '8 hours'),

(NULL, 99, 1, 'WELCOME001', 'VELOCITY_FRAUD', 95,
 ARRAY['too_many_attempts', 'multiple_devices'],
 '{"attempts_count": 10, "devices": ["mobile", "web", "unknown"]}',
 'BLOCKED', CURRENT_TIMESTAMP - INTERVAL '12 hours');

-- Insert QR validations (legacy support)
INSERT INTO redemption_schema.qr_validations (
    qr_code_hash, station_id, dispenser_code, nonce, expires_at,
    signature_algorithm, signature, validation_data, created_at
) VALUES
('HASH_WELCOME001', 1, 'DISP_001', 'NONCE_001', CURRENT_TIMESTAMP + INTERVAL '1 hour',
 'RS256', 'SIG_WELCOME001_VALIDATION', '{"coupon_code": "WELCOME001", "user_id": 11}', CURRENT_TIMESTAMP - INTERVAL '2 hours'),

('HASH_SUMMER001', 2, 'DISP_002', 'NONCE_002', CURRENT_TIMESTAMP + INTERVAL '30 minutes',
 'RS256', 'SIG_SUMMER001_VALIDATION', '{"coupon_code": "SUMMER001", "user_id": 12}', CURRENT_TIMESTAMP - INTERVAL '1 hour'),

('HASH_PREMIUM001', 2, 'DISP_003', 'NONCE_003', CURRENT_TIMESTAMP - INTERVAL '30 minutes',
 'RS256', 'SIG_PREMIUM001_VALIDATION', '{"coupon_code": "PREMIUM001", "user_id": 22}', CURRENT_TIMESTAMP - INTERVAL '4 hours'),

('HASH_EXPIRED001', 3, 'DISP_004', 'NONCE_004', CURRENT_TIMESTAMP - INTERVAL '2 hours',
 'RS256', 'SIG_EXPIRED001_VALIDATION', '{"coupon_code": "EXPIRED001", "user_id": 13}', CURRENT_TIMESTAMP - INTERVAL '6 hours');

-- Update QR validation with used_at for completed redemptions
UPDATE redemption_schema.qr_validations
SET used_at = CURRENT_TIMESTAMP - INTERVAL '4 hours'
WHERE nonce = 'NONCE_003';

-- Insert points ledger entries (legacy support)
INSERT INTO redemption_schema.points_ledger (
    user_id, amount, transaction_type, source_transaction_id, source_transaction_type,
    description, balance_after, expires_at, created_at
) VALUES
-- Points earned from redemptions
(14, 150, 'EARNED', 1, 'REDEMPTION', 'Points from coupon redemption TXN_WELCOME_001', 150, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(12, 200, 'EARNED', 2, 'REDEMPTION', 'Points from coupon redemption TXN_SUMMER_001', 200, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(16, 180, 'EARNED', 3, 'REDEMPTION', 'Points from coupon redemption TXN_SUMMER_002', 380, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(16, 160, 'EARNED', 4, 'REDEMPTION', 'Points from coupon redemption TXN_SUMMER_003', 540, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(22, 300, 'EARNED', 5, 'REDEMPTION', 'Points from coupon redemption TXN_PREMIUM_001', 300, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
(22, 250, 'EARNED', 6, 'REDEMPTION', 'Points from coupon redemption TXN_PREMIUM_002', 550, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Bonus points
(14, 50, 'BONUS', NULL, 'WELCOME_BONUS', 'Welcome bonus for new customer', 200, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(22, 100, 'BONUS', NULL, 'LOYALTY_BONUS', 'Loyalty bonus for premium customer', 650, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Points redeemed
(16, -100, 'REDEEMED', NULL, 'RAFFLE_ENTRY', 'Points used for raffle entry', 440, NULL, CURRENT_TIMESTAMP - INTERVAL '12 hours'),
(22, -150, 'REDEEMED', NULL, 'DISCOUNT_COUPON', 'Points redeemed for discount coupon', 500, NULL, CURRENT_TIMESTAMP - INTERVAL '6 hours'),

-- Expired points
(12, -50, 'EXPIRED', NULL, 'EXPIRATION', 'Points expired after 1 year', 150, NULL, CURRENT_TIMESTAMP - INTERVAL '1 day'),

-- Adjusted points (admin correction)
(14, 25, 'ADJUSTED', NULL, 'ADMIN_CORRECTION', 'Points adjustment by admin', 225, CURRENT_TIMESTAMP + INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '1 hour');