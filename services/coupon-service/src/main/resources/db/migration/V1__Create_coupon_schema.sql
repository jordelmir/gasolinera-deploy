-- V1__Create_coupon_schema.sql
-- Create the coupon schema and initial tables for the coupon service

-- Create the coupon schema
CREATE SCHEMA IF NOT EXISTS coupon_schema;

-- Set the search path to include the coupon schema
SET search_path TO coupon_schema, public;

-- Create the campaigns table
CREATE TABLE coupon_schema.campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    campaign_code VARCHAR(50) NOT NULL UNIQUE,
    discount_type VARCHAR(20) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    minimum_purchase DECIMAL(10,2),
    maximum_discount DECIMAL(10,2),
    usage_limit_per_user INTEGER,
    total_usage_limit INTEGER,
    current_usage_count INTEGER NOT NULL DEFAULT 0,
    raffle_tickets_per_coupon INTEGER NOT NULL DEFAULT 1,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    target_audience VARCHAR(100),
    applicable_stations TEXT, -- JSON array of station IDs or 'ALL'
    terms_and_conditions TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_campaigns_discount_type CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'BUY_X_GET_Y')),
    CONSTRAINT chk_campaigns_discount_value CHECK (discount_value > 0),
    CONSTRAINT chk_campaigns_minimum_purchase CHECK (minimum_purchase IS NULL OR minimum_purchase >= 0),
    CONSTRAINT chk_campaigns_maximum_discount CHECK (maximum_discount IS NULL OR maximum_discount >= 0),
    CONSTRAINT chk_campaigns_usage_limits CHECK (usage_limit_per_user IS NULL OR usage_limit_per_user > 0),
    CONSTRAINT chk_campaigns_total_usage_limit CHECK (total_usage_limit IS NULL OR total_usage_limit > 0),
    CONSTRAINT chk_campaigns_current_usage CHECK (current_usage_count >= 0),
    CONSTRAINT chk_campaigns_raffle_tickets CHECK (raffle_tickets_per_coupon >= 0),
    CONSTRAINT chk_campaigns_date_range CHECK (end_date > start_date),
    CONSTRAINT chk_campaigns_code_format CHECK (campaign_code ~ '^[A-Z0-9_]{3,50}$')
);

-- Create indexes for campaigns
CREATE INDEX idx_campaigns_code ON coupon_schema.campaigns(campaign_code);
CREATE INDEX idx_campaigns_active ON coupon_schema.campaigns(is_active);
CREATE INDEX idx_campaigns_dates ON coupon_schema.campaigns(start_date, end_date);
CREATE INDEX idx_campaigns_discount_type ON coupon_schema.campaigns(discount_type);
CREATE INDEX idx_campaigns_created_at ON coupon_schema.campaigns(created_at);

-- Create the coupons table
CREATE TABLE coupon_schema.coupons (
    id BIGSERIAL PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL UNIQUE,
    campaign_id BIGINT NOT NULL REFERENCES coupon_schema.campaigns(id) ON DELETE CASCADE,
    qr_code TEXT NOT NULL,
    qr_signature VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    discount_amount DECIMAL(10,2) NOT NULL,
    raffle_tickets INTEGER NOT NULL DEFAULT 1,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    max_usage INTEGER NOT NULL DEFAULT 1,
    assigned_user_id BIGINT,
    batch_id VARCHAR(100),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_coupons_status CHECK (status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED', 'SUSPENDED')),
    CONSTRAINT chk_coupons_discount_amount CHECK (discount_amount >= 0),
    CONSTRAINT chk_coupons_raffle_tickets CHECK (raffle_tickets >= 0),
    CONSTRAINT chk_coupons_usage_count CHECK (usage_count >= 0),
    CONSTRAINT chk_coupons_max_usage CHECK (max_usage > 0),
    CONSTRAINT chk_coupons_usage_limit CHECK (usage_count <= max_usage),
    CONSTRAINT chk_coupons_validity_period CHECK (valid_until > valid_from),
    CONSTRAINT chk_coupons_code_format CHECK (coupon_code ~ '^[A-Z0-9]{6,50}$')
);

-- Create indexes for coupons
CREATE INDEX idx_coupons_code ON coupon_schema.coupons(coupon_code);
CREATE INDEX idx_coupons_campaign_id ON coupon_schema.coupons(campaign_id);
CREATE INDEX idx_coupons_status ON coupon_schema.coupons(status);
CREATE INDEX idx_coupons_validity ON coupon_schema.coupons(valid_from, valid_until);
CREATE INDEX idx_coupons_assigned_user ON coupon_schema.coupons(assigned_user_id);
CREATE INDEX idx_coupons_batch_id ON coupon_schema.coupons(batch_id);
CREATE INDEX idx_coupons_created_at ON coupon_schema.coupons(created_at);

-- Create the coupon usage history table
CREATE TABLE coupon_schema.coupon_usage_history (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupon_schema.coupons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL,
    employee_id BIGINT,
    redemption_id BIGINT,
    original_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2) NOT NULL,
    raffle_tickets_awarded INTEGER NOT NULL DEFAULT 0,
    transaction_reference VARCHAR(100),
    payment_method VARCHAR(50),
    ip_address INET,
    user_agent TEXT,
    device_info JSONB,
    location_data JSONB,
    validation_method VARCHAR(50) NOT NULL DEFAULT 'QR_SCAN',
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    correlation_id VARCHAR(255),

    -- Constraints
    CONSTRAINT chk_coupon_usage_amounts CHECK (
        original_amount >= 0 AND
        discount_amount >= 0 AND
        final_amount >= 0 AND
        final_amount = original_amount - discount_amount
    ),
    CONSTRAINT chk_coupon_usage_raffle_tickets CHECK (raffle_tickets_awarded >= 0),
    CONSTRAINT chk_coupon_usage_validation_method CHECK (
        validation_method IN ('QR_SCAN', 'MANUAL_ENTRY', 'NFC', 'BARCODE')
    )
);

-- Create indexes for coupon usage history
CREATE INDEX idx_coupon_usage_coupon_id ON coupon_schema.coupon_usage_history(coupon_id);
CREATE INDEX idx_coupon_usage_user_id ON coupon_schema.coupon_usage_history(user_id);
CREATE INDEX idx_coupon_usage_station_id ON coupon_schema.coupon_usage_history(station_id);
CREATE INDEX idx_coupon_usage_used_at ON coupon_schema.coupon_usage_history(used_at);
CREATE INDEX idx_coupon_usage_redemption_id ON coupon_schema.coupon_usage_history(redemption_id);
CREATE INDEX idx_coupon_usage_session_id ON coupon_schema.coupon_usage_history(session_id);
CREATE INDEX idx_coupon_usage_correlation_id ON coupon_schema.coupon_usage_history(correlation_id);

-- Create coupon validation attempts table (for fraud detection)
CREATE TABLE coupon_schema.coupon_validation_attempts (
    id BIGSERIAL PRIMARY KEY,
    coupon_code VARCHAR(50) NOT NULL,
    user_id BIGINT,
    station_id BIGINT,
    employee_id BIGINT,
    validation_result VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    device_fingerprint VARCHAR(255),
    location_data JSONB,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id VARCHAR(255),
    correlation_id VARCHAR(255),

    -- Constraints
    CONSTRAINT chk_validation_result CHECK (
        validation_result IN ('SUCCESS', 'INVALID_CODE', 'EXPIRED', 'ALREADY_USED', 'SUSPENDED', 'FRAUD_DETECTED')
    )
);

-- Create indexes for validation attempts
CREATE INDEX idx_validation_attempts_coupon_code ON coupon_schema.coupon_validation_attempts(coupon_code);
CREATE INDEX idx_validation_attempts_user_id ON coupon_schema.coupon_validation_attempts(user_id);
CREATE INDEX idx_validation_attempts_station_id ON coupon_schema.coupon_validation_attempts(station_id);
CREATE INDEX idx_validation_attempts_result ON coupon_schema.coupon_validation_attempts(validation_result);
CREATE INDEX idx_validation_attempts_attempted_at ON coupon_schema.coupon_validation_attempts(attempted_at);
CREATE INDEX idx_validation_attempts_ip_address ON coupon_schema.coupon_validation_attempts(ip_address);
CREATE INDEX idx_validation_attempts_session_id ON coupon_schema.coupon_validation_attempts(session_id);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION coupon_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_campaigns_updated_at
    BEFORE UPDATE ON coupon_schema.campaigns
    FOR EACH ROW
    EXECUTE FUNCTION coupon_schema.update_updated_at_column();

CREATE TRIGGER update_coupons_updated_at
    BEFORE UPDATE ON coupon_schema.coupons
    FOR EACH ROW
    EXECUTE FUNCTION coupon_schema.update_updated_at_column();

-- Create function to update campaign usage count
CREATE OR REPLACE FUNCTION coupon_schema.update_campaign_usage_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE coupon_schema.campaigns
        SET current_usage_count = current_usage_count + 1
        WHERE id = (SELECT campaign_id FROM coupon_schema.coupons WHERE id = NEW.coupon_id);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

-- Create trigger to update campaign usage count when coupon is used
CREATE TRIGGER update_campaign_usage_count_trigger
    AFTER INSERT ON coupon_schema.coupon_usage_history
    FOR EACH ROW
    EXECUTE FUNCTION coupon_schema.update_campaign_usage_count();

-- Create views for common queries
CREATE VIEW coupon_schema.active_campaigns AS
SELECT
    id,
    name,
    description,
    campaign_code,
    discount_type,
    discount_value,
    minimum_purchase,
    maximum_discount,
    usage_limit_per_user,
    total_usage_limit,
    current_usage_count,
    raffle_tickets_per_coupon,
    start_date,
    end_date,
    target_audience,
    created_at
FROM coupon_schema.campaigns
WHERE is_active = true
  AND start_date <= CURRENT_TIMESTAMP
  AND end_date > CURRENT_TIMESTAMP;

CREATE VIEW coupon_schema.available_coupons AS
SELECT
    c.id,
    c.coupon_code,
    c.campaign_id,
    c.discount_amount,
    c.raffle_tickets,
    c.valid_from,
    c.valid_until,
    c.usage_count,
    c.max_usage,
    c.assigned_user_id,
    camp.name as campaign_name,
    camp.discount_type,
    camp.minimum_purchase,
    camp.maximum_discount
FROM coupon_schema.coupons c
JOIN coupon_schema.campaigns camp ON c.campaign_id = camp.id
WHERE c.status = 'ACTIVE'
  AND c.valid_from <= CURRENT_TIMESTAMP
  AND c.valid_until > CURRENT_TIMESTAMP
  AND c.usage_count < c.max_usage
  AND camp.is_active = true;

CREATE VIEW coupon_schema.coupon_statistics AS
SELECT
    camp.id as campaign_id,
    camp.name as campaign_name,
    camp.campaign_code,
    COUNT(c.id) as total_coupons,
    COUNT(CASE WHEN c.status = 'ACTIVE' THEN 1 END) as active_coupons,
    COUNT(CASE WHEN c.status = 'USED' THEN 1 END) as used_coupons,
    COUNT(CASE WHEN c.status = 'EXPIRED' THEN 1 END) as expired_coupons,
    COALESCE(SUM(cuh.discount_amount), 0) as total_discount_given,
    COALESCE(SUM(cuh.raffle_tickets_awarded), 0) as total_raffle_tickets_awarded,
    COUNT(DISTINCT cuh.user_id) as unique_users_served
FROM coupon_schema.campaigns camp
LEFT JOIN coupon_schema.coupons c ON camp.id = c.campaign_id
LEFT JOIN coupon_schema.coupon_usage_history cuh ON c.id = cuh.coupon_id
GROUP BY camp.id, camp.name, camp.campaign_code;

-- Insert sample campaigns for development
INSERT INTO coupon_schema.campaigns (
    name, description, campaign_code, discount_type, discount_value,
    minimum_purchase, maximum_discount, usage_limit_per_user, total_usage_limit,
    raffle_tickets_per_coupon, start_date, end_date, is_active,
    target_audience, applicable_stations, terms_and_conditions
) VALUES
(
    'Welcome Bonus',
    'Welcome bonus for new customers',
    'WELCOME2024',
    'PERCENTAGE',
    10.00,
    50.00,
    20.00,
    1,
    1000,
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    true,
    'NEW_CUSTOMERS',
    'ALL',
    'Valid for new customers only. Cannot be combined with other offers.'
),
(
    'Summer Special',
    'Summer discount campaign',
    'SUMMER2024',
    'FIXED_AMOUNT',
    15.00,
    100.00,
    15.00,
    3,
    5000,
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    true,
    'ALL_CUSTOMERS',
    'ALL',
    'Valid during summer season. Maximum 3 uses per customer.'
),
(
    'Loyalty Reward',
    'Reward for loyal customers',
    'LOYALTY2024',
    'PERCENTAGE',
    20.00,
    75.00,
    50.00,
    5,
    NULL,
    5,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '90 days',
    true,
    'LOYAL_CUSTOMERS',
    'ALL',
    'For customers with 10+ previous transactions.'
) ON CONFLICT (campaign_code) DO NOTHING;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA coupon_schema TO coupon_service_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA coupon_schema TO coupon_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA coupon_schema TO coupon_service_user;