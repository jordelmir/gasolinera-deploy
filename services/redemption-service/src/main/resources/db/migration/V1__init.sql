
-- V1__Create_redemption_schema.sql
-- Create the redemption schema and initial tables for the redemption service

-- Create the redemption schema
CREATE SCHEMA IF NOT EXISTS redemption_schema;

-- Set the search path to include the redemption schema
SET search_path TO redemption_schema, public;

-- Create the redemptions table
CREATE TABLE redemption_schema.redemptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    qr_code VARCHAR(500) NOT NULL,
    coupon_code VARCHAR(50) NOT NULL,
    fuel_type VARCHAR(50),
    fuel_quantity DECIMAL(8,3),
    fuel_price_per_unit DECIMAL(8,3),
    purchase_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(10,2) NOT NULL,
    raffle_tickets_earned INTEGER NOT NULL DEFAULT 0,
    payment_method VARCHAR(50),
    payment_reference VARCHAR(100),
    validation_timestamp TIMESTAMP NOT NULL,
    redemption_timestamp TIMESTAMP,
    completion_timestamp TIMESTAMP,
    failure_reason VARCHAR(500),
    notes VARCHAR(1000),
    metadata VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_redemptions_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_redemptions_fuel_quantity CHECK (fuel_quantity IS NULL OR fuel_quantity >= 0),
    CONSTRAINT chk_redemptions_fuel_price CHECK (fuel_price_per_unit IS NULL OR fuel_price_per_unit >= 0),
    CONSTRAINT chk_redemptions_purchase_amount CHECK (purchase_amount >= 0),
    CONSTRAINT chk_redemptions_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT chk_redemptions_final_amount CHECK (final_amount >= 0),
    CONSTRAINT chk_redemptions_raffle_tickets CHECK (raffle_tickets_earned >= 0),
    CONSTRAINT chk_redemptions_amounts CHECK (final_amount = purchase_amount - discount_amount),
    CONSTRAINT chk_redemptions_transaction_ref_format CHECK (LENGTH(transaction_reference) >= 5)
);

-- Create indexes for redemptions
CREATE INDEX idx_redemptions_user_id ON redemption_schema.redemptions(user_id);
CREATE INDEX idx_redemptions_station_id ON redemption_schema.redemptions(station_id);
CREATE INDEX idx_redemptions_employee_id ON redemption_schema.redemptions(employee_id);
CREATE INDEX idx_redemptions_coupon_id ON redemption_schema.redemptions(coupon_id);
CREATE INDEX idx_redemptions_status ON redemption_schema.redemptions(status);
CREATE INDEX idx_redemptions_transaction_ref ON redemption_schema.redemptions(transaction_reference);
CREATE INDEX idx_redemptions_created_at ON redemption_schema.redemptions(created_at);
CREATE INDEX idx_redemptions_validation_timestamp ON redemption_schema.redemptions(validation_timestamp);
CREATE INDEX idx_redemptions_completion_timestamp ON redemption_schema.redemptions(completion_timestamp);
CREATE INDEX idx_redemptions_campaign_id ON redemption_schema.redemptions(campaign_id);

-- Create the raffle tickets table (generated from redemptions)
CREATE TABLE redemption_schema.raffle_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    redemption_id BIGINT NOT NULL REFERENCES redemption_schema.redemptions(id) ON DELETE CASCADE,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source_type VARCHAR(30) NOT NULL DEFAULT 'COUPON_REDEMPTION',
    source_reference VARCHAR(100),
    multiplier_applied INTEGER NOT NULL DEFAULT 1,
    is_bonus_ticket BOOLEAN NOT NULL DEFAULT false,
    expires_at TIMESTAMP,
    used_at TIMESTAMP,
    raffle_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_raffle_tickets_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT chk_raffle_tickets_source_type CHECK (source_type IN ('COUPON_REDEMPTION', 'AD_ENGAGEMENT', 'PROMOTIONAL', 'BONUS')),
    CONSTRAINT chk_raffle_tickets_multiplier CHECK (multiplier_applied >= 1),
    CONSTRAINT chk_raffle_tickets_number_format CHECK (LENGTH(ticket_number) >= 8)
);

-- Create indexes for raffle tickets
CREATE INDEX idx_raffle_tickets_user_id ON redemption_schema.raffle_tickets(user_id);
CREATE INDEX idx_raffle_tickets_redemption_id ON redemption_schema.raffle_tickets(redemption_id);
CREATE INDEX idx_raffle_tickets_ticket_number ON redemption_schema.raffle_tickets(ticket_number);
CREATE INDEX idx_raffle_tickets_status ON redemption_schema.raffle_tickets(status);
CREATE INDEX idx_raffle_tickets_source_type ON redemption_schema.raffle_tickets(source_type);
CREATE INDEX idx_raffle_tickets_created_at ON redemption_schema.raffle_tickets(created_at);
CREATE INDEX idx_raffle_tickets_expires_at ON redemption_schema.raffle_tickets(expires_at);
CREATE INDEX idx_raffle_tickets_raffle_id ON redemption_schema.raffle_tickets(raffle_id);

-- Create fraud detection table
CREATE TABLE redemption_schema.fraud_detection_log (
    id BIGSERIAL PRIMARY KEY,
    redemption_id BIGINT REFERENCES redemption_schema.redemptions(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    coupon_code VARCHAR(50) NOT NULL,
    fraud_type VARCHAR(50) NOT NULL,
    risk_score INTEGER NOT NULL,
    detection_rules TEXT[],
    additional_data JSONB,
    action_taken VARCHAR(50) NOT NULL,
    reviewed_by VARCHAR(100),
    reviewed_at TIMESTAMP,
    resolution VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_fraud_detection_type CHECK (
        fraud_type IN ('DUPLICATE_REDEMPTION', 'SUSPICIOUS_PATTERN', 'VELOCITY_FRAUD', 'LOCATION_ANOMALY', 'DEVICE_FRAUD')
    ),
    CONSTRAINT chk_fraud_detection_risk_score CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_fraud_detection_action CHECK (
        action_taken IN ('BLOCKED', 'FLAGGED', 'ALLOWED_WITH_REVIEW', 'ALLOWED')
    )
);

-- Create indexes for fraud detection
CREATE INDEX idx_fraud_detection_redemption_id ON redemption_schema.fraud_detection_log(redemption_id);
CREATE INDEX idx_fraud_detection_user_id ON redemption_schema.fraud_detection_log(user_id);
CREATE INDEX idx_fraud_detection_station_id ON redemption_schema.fraud_detection_log(station_id);
CREATE INDEX idx_fraud_detection_fraud_type ON redemption_schema.fraud_detection_log(fraud_type);
CREATE INDEX idx_fraud_detection_risk_score ON redemption_schema.fraud_detection_log(risk_score);
CREATE INDEX idx_fraud_detection_created_at ON redemption_schema.fraud_detection_log(created_at);
CREATE INDEX idx_fraud_detection_action_taken ON redemption_schema.fraud_detection_log(action_taken);

-- Create QR code validation table (legacy support)
CREATE TABLE redemption_schema.qr_validations (
    id BIGSERIAL PRIMARY KEY,
    qr_code_hash VARCHAR(255) NOT NULL UNIQUE,
    station_id BIGINT NOT NULL,
    dispenser_code VARCHAR(50),
    nonce VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    signature_algorithm VARCHAR(50) NOT NULL DEFAULT 'RS256',
    signature TEXT NOT NULL,
    validation_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_qr_validations_expires_at CHECK (expires_at > created_at),
    CONSTRAINT chk_qr_validations_used_at CHECK (used_at IS NULL OR used_at >= created_at)
);

-- Create indexes for QR validations
CREATE INDEX idx_qr_validations_nonce ON redemption_schema.qr_validations(nonce);
CREATE INDEX idx_qr_validations_station_id ON redemption_schema.qr_validations(station_id);
CREATE INDEX idx_qr_validations_expires_at ON redemption_schema.qr_validations(expires_at);
CREATE INDEX idx_qr_validations_used_at ON redemption_schema.qr_validations(used_at);
CREATE INDEX idx_qr_validations_hash ON redemption_schema.qr_validations(qr_code_hash);

-- Create points ledger table (legacy support)
CREATE TABLE redemption_schema.points_ledger (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount INTEGER NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    source_transaction_id BIGINT,
    source_transaction_type VARCHAR(50),
    description VARCHAR(200),
    balance_after INTEGER NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_points_ledger_transaction_type CHECK (
        transaction_type IN ('EARNED', 'REDEEMED', 'EXPIRED', 'ADJUSTED', 'BONUS')
    ),
    CONSTRAINT chk_points_ledger_balance_after CHECK (balance_after >= 0)
);

-- Create indexes for points ledger
CREATE INDEX idx_points_ledger_user_id ON redemption_schema.points_ledger(user_id);
CREATE INDEX idx_points_ledger_transaction_type ON redemption_schema.points_ledger(transaction_type);
CREATE INDEX idx_points_ledger_source_transaction ON redemption_schema.points_ledger(source_transaction_id, source_transaction_type);
CREATE INDEX idx_points_ledger_created_at ON redemption_schema.points_ledger(created_at);
CREATE INDEX idx_points_ledger_expires_at ON redemption_schema.points_ledger(expires_at);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION redemption_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_redemptions_updated_at
    BEFORE UPDATE ON redemption_schema.redemptions
    FOR EACH ROW
    EXECUTE FUNCTION redemption_schema.update_updated_at_column();

CREATE TRIGGER update_raffle_tickets_updated_at
    BEFORE UPDATE ON redemption_schema.raffle_tickets
    FOR EACH ROW
    EXECUTE FUNCTION redemption_schema.update_updated_at_column();

-- Create function to generate raffle tickets automatically
CREATE OR REPLACE FUNCTION redemption_schema.generate_raffle_tickets()
RETURNS TRIGGER AS $$
DECLARE
    i INTEGER;
    ticket_num VARCHAR(50);
BEGIN
    -- Only generate tickets for completed redemptions
    IF NEW.status = 'COMPLETED' AND NEW.raffle_tickets_earned > 0 THEN
        FOR i IN 1..NEW.raffle_tickets_earned LOOP
            ticket_num := 'RT' || NEW.id || '_' || i || '_' || EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT;

            INSERT INTO redemption_schema.raffle_tickets (
                user_id,
                redemption_id,
                ticket_number,
                status,
                source_type,
                source_reference,
                multiplier_applied
            ) VALUES (
                NEW.user_id,
                NEW.id,
                ticket_num,
                'ACTIVE',
                'COUPON_REDEMPTION',
                NEW.coupon_code,
                1
            );
        END LOOP;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to generate raffle tickets
CREATE TRIGGER generate_raffle_tickets_trigger
    AFTER UPDATE ON redemption_schema.redemptions
    FOR EACH ROW
    WHEN (OLD.status != 'COMPLETED' AND NEW.status = 'COMPLETED')
    EXECUTE FUNCTION redemption_schema.generate_raffle_tickets();

-- Create views for common queries
CREATE VIEW redemption_schema.successful_redemptions AS
SELECT
    r.id,
    r.user_id,
    r.station_id,
    r.employee_id,
    r.coupon_code,
    r.purchase_amount,
    r.discount_amount,
    r.final_amount,
    r.raffle_tickets_earned,
    r.payment_method,
    r.validation_timestamp,
    r.completion_timestamp,
    r.created_at
FROM redemption_schema.redemptions r
WHERE r.status = 'COMPLETED';

CREATE VIEW redemption_schema.active_raffle_tickets AS
SELECT
    rt.id,
    rt.user_id,
    rt.redemption_id,
    rt.ticket_number,
    rt.source_type,
    rt.source_reference,
    rt.multiplier_applied,
    rt.created_at,
    r.station_id,
    r.coupon_code
FROM redemption_schema.raffle_tickets rt
JOIN redemption_schema.redemptions r ON rt.redemption_id = r.id
WHERE rt.status = 'ACTIVE'
  AND (rt.expires_at IS NULL OR rt.expires_at > CURRENT_TIMESTAMP);

CREATE VIEW redemption_schema.redemption_statistics AS
SELECT
    DATE(r.created_at) as redemption_date,
    r.station_id,
    COUNT(*) as total_redemptions,
    COUNT(CASE WHEN r.status = 'COMPLETED' THEN 1 END) as successful_redemptions,
    COUNT(CASE WHEN r.status = 'FAILED' THEN 1 END) as failed_redemptions,
    SUM(CASE WHEN r.status = 'COMPLETED' THEN r.purchase_amount ELSE 0 END) as total_purchase_amount,
    SUM(CASE WHEN r.status = 'COMPLETED' THEN r.discount_amount ELSE 0 END) as total_discount_given,
    SUM(CASE WHEN r.status = 'COMPLETED' THEN r.raffle_tickets_earned ELSE 0 END) as total_raffle_tickets_generated
FROM redemption_schema.redemptions r
GROUP BY DATE(r.created_at), r.station_id;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA redemption_schema TO redemption_service_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA redemption_schema TO redemption_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA redemption_schema TO redemption_service_user;
