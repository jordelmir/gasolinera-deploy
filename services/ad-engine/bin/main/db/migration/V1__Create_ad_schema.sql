-- V1__Create_ad_schema.sql
-- Create the ad schema and initial tables for the ad engine service

-- Create the ad schema
CREATE SCHEMA IF NOT EXISTS ad_schema;

-- Set the search path to include the ad schema
SET search_path TO ad_schema, public;

-- Create the advertisements table
CREATE TABLE ad_schema.advertisements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    ad_type VARCHAR(30) NOT NULL DEFAULT 'VIDEO',
    content_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    duration_seconds INTEGER,
    advertiser_id BIGINT NOT NULL,
    advertiser_name VARCHAR(200) NOT NULL,
    campaign_id BIGINT,
    campaign_name VARCHAR(200),
    ticket_multiplier INTEGER NOT NULL DEFAULT 1,
    target_audience JSONB,
    targeting_criteria JSONB,
    budget_total DECIMAL(12,2),
    budget_spent DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cost_per_engagement DECIMAL(8,2),
    max_engagements_per_user INTEGER DEFAULT 3,
    priority_level INTEGER NOT NULL DEFAULT 5,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(100),
    approved_at TIMESTAMP,
    rejection_reason VARCHAR(500),
    content_rating VARCHAR(10) NOT NULL DEFAULT 'G',
    language_code VARCHAR(5) NOT NULL DEFAULT 'es',
    geographic_targeting JSONB,
    demographic_targeting JSONB,
    behavioral_targeting JSONB,
    frequency_cap INTEGER DEFAULT 5,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_advertisements_ad_type CHECK (ad_type IN ('VIDEO', 'IMAGE', 'INTERACTIVE', 'AUDIO', 'TEXT')),
    CONSTRAINT chk_advertisements_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_advertisements_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'NEEDS_REVIEW')),
    CONSTRAINT chk_advertisements_content_rating CHECK (content_rating IN ('G', 'PG', 'PG-13', 'R')),
    CONSTRAINT chk_advertisements_ticket_multiplier CHECK (ticket_multiplier >= 1 AND ticket_multiplier <= 10),
    CONSTRAINT chk_advertisements_priority_level CHECK (priority_level >= 1 AND priority_level <= 10),
    CONSTRAINT chk_advertisements_budget_total CHECK (budget_total IS NULL OR budget_total >= 0),
    CONSTRAINT chk_advertisements_budget_spent CHECK (budget_spent >= 0),
    CONSTRAINT chk_advertisements_cost_per_engagement CHECK (cost_per_engagement IS NULL OR cost_per_engagement >= 0),
    CONSTRAINT chk_advertisements_max_engagements CHECK (max_engagements_per_user IS NULL OR max_engagements_per_user > 0),
    CONSTRAINT chk_advertisements_frequency_cap CHECK (frequency_cap IS NULL OR frequency_cap > 0),
    CONSTRAINT chk_advertisements_duration CHECK (duration_seconds IS NULL OR duration_seconds > 0),
    CONSTRAINT chk_advertisements_date_range CHECK (end_date > start_date),
    CONSTRAINT chk_advertisements_budget_limit CHECK (budget_total IS NULL OR budget_spent <= budget_total)
);

-- Create indexes for advertisements
CREATE INDEX idx_advertisements_advertiser_id ON ad_schema.advertisements(advertiser_id);
CREATE INDEX idx_advertisements_campaign_id ON ad_schema.advertisements(campaign_id);
CREATE INDEX idx_advertisements_status ON ad_schema.advertisements(status);
CREATE INDEX idx_advertisements_approval_status ON ad_schema.advertisements(approval_status);
CREATE INDEX idx_advertisements_dates ON ad_schema.advertisements(start_date, end_date);
CREATE INDEX idx_advertisements_priority ON ad_schema.advertisements(priority_level);
CREATE INDEX idx_advertisements_ad_type ON ad_schema.advertisements(ad_type);
CREATE INDEX idx_advertisements_content_rating ON ad_schema.advertisements(content_rating);
CREATE INDEX idx_advertisements_language ON ad_schema.advertisements(language_code);
CREATE INDEX idx_advertisements_created_at ON ad_schema.advertisements(created_at);

-- Create the ad engagements table
CREATE TABLE ad_schema.ad_engagements (
    id BIGSERIAL PRIMARY KEY,
    advertisement_id BIGINT NOT NULL REFERENCES ad_schema.advertisements(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    session_id VARCHAR(255),
    engagement_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'STARTED',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    duration_seconds INTEGER,
    completion_percentage INTEGER NOT NULL DEFAULT 0,
    interaction_count INTEGER NOT NULL DEFAULT 0,
    skip_count INTEGER NOT NULL DEFAULT 0,
    replay_count INTEGER NOT NULL DEFAULT 0,
    tickets_multiplied INTEGER NOT NULL DEFAULT 0,
    multiplier_applied INTEGER NOT NULL DEFAULT 1,
    station_id BIGINT,
    device_type VARCHAR(50),
    device_info JSONB,
    ip_address INET,
    user_agent TEXT,
    referrer_url VARCHAR(500),
    location_data JSONB,
    engagement_quality_score INTEGER,
    fraud_score INTEGER NOT NULL DEFAULT 0,
    is_fraudulent BOOLEAN NOT NULL DEFAULT false,
    fraud_reasons TEXT[],
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_ad_engagements_engagement_type CHECK (
        engagement_type IN ('VIEW', 'CLICK', 'INTERACTION', 'COMPLETION', 'SKIP', 'REPLAY')
    ),
    CONSTRAINT chk_ad_engagements_status CHECK (
        status IN ('STARTED', 'IN_PROGRESS', 'COMPLETED', 'SKIPPED', 'ABANDONED', 'ERROR')
    ),
    CONSTRAINT chk_ad_engagements_completion_percentage CHECK (
        completion_percentage >= 0 AND completion_percentage <= 100
    ),
    CONSTRAINT chk_ad_engagements_interaction_count CHECK (interaction_count >= 0),
    CONSTRAINT chk_ad_engagements_skip_count CHECK (skip_count >= 0),
    CONSTRAINT chk_ad_engagements_replay_count CHECK (replay_count >= 0),
    CONSTRAINT chk_ad_engagements_tickets_multiplied CHECK (tickets_multiplied >= 0),
    CONSTRAINT chk_ad_engagements_multiplier_applied CHECK (multiplier_applied >= 1),
    CONSTRAINT chk_ad_engagements_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT chk_ad_engagements_quality_score CHECK (
        engagement_quality_score IS NULL OR (engagement_quality_score >= 0 AND engagement_quality_score <= 100)
    ),
    CONSTRAINT chk_ad_engagements_fraud_score CHECK (fraud_score >= 0 AND fraud_score <= 100),
    CONSTRAINT chk_ad_engagements_completed_at CHECK (
        completed_at IS NULL OR completed_at >= started_at
    )
);

-- Create indexes for ad engagements
CREATE INDEX idx_ad_engagements_advertisement_id ON ad_schema.ad_engagements(advertisement_id);
CREATE INDEX idx_ad_engagements_user_id ON ad_schema.ad_engagements(user_id);
CREATE INDEX idx_ad_engagements_session_id ON ad_schema.ad_engagements(session_id);
CREATE INDEX idx_ad_engagements_engagement_type ON ad_schema.ad_engagements(engagement_type);
CREATE INDEX idx_ad_engagements_status ON ad_schema.ad_engagements(status);
CREATE INDEX idx_ad_engagements_started_at ON ad_schema.ad_engagements(started_at);
CREATE INDEX idx_ad_engagements_completed_at ON ad_schema.ad_engagements(completed_at);
CREATE INDEX idx_ad_engagements_station_id ON ad_schema.ad_engagements(station_id);
CREATE INDEX idx_ad_engagements_completion_percentage ON ad_schema.ad_engagements(completion_percentage);
CREATE INDEX idx_ad_engagements_fraud_score ON ad_schema.ad_engagements(fraud_score);
CREATE INDEX idx_ad_engagements_is_fraudulent ON ad_schema.ad_engagements(is_fraudulent);
CREATE INDEX idx_ad_engagements_created_at ON ad_schema.ad_engagements(created_at);

-- Create ad campaigns table
CREATE TABLE ad_schema.ad_campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    advertiser_id BIGINT NOT NULL,
    advertiser_name VARCHAR(200) NOT NULL,
    campaign_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    objective VARCHAR(50) NOT NULL,
    budget_total DECIMAL(12,2) NOT NULL,
    budget_daily DECIMAL(10,2),
    budget_spent DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    target_engagements INTEGER,
    actual_engagements INTEGER NOT NULL DEFAULT 0,
    target_audience JSONB,
    geographic_targeting JSONB,
    demographic_targeting JSONB,
    behavioral_targeting JSONB,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority_level INTEGER NOT NULL DEFAULT 5,
    frequency_cap INTEGER DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_ad_campaigns_campaign_type CHECK (
        campaign_type IN ('STANDARD', 'PREMIUM', 'SPONSORED', 'PROMOTIONAL')
    ),
    CONSTRAINT chk_ad_campaigns_objective CHECK (
        objective IN ('BRAND_AWARENESS', 'ENGAGEMENT', 'CONVERSIONS', 'TRAFFIC', 'REACH')
    ),
    CONSTRAINT chk_ad_campaigns_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_ad_campaigns_budget_total CHECK (budget_total > 0),
    CONSTRAINT chk_ad_campaigns_budget_daily CHECK (budget_daily IS NULL OR budget_daily > 0),
    CONSTRAINT chk_ad_campaigns_budget_spent CHECK (budget_spent >= 0),
    CONSTRAINT chk_ad_campaigns_target_engagements CHECK (target_engagements IS NULL OR target_engagements > 0),
    CONSTRAINT chk_ad_campaigns_actual_engagements CHECK (actual_engagements >= 0),
    CONSTRAINT chk_ad_campaigns_priority_level CHECK (priority_level >= 1 AND priority_level <= 10),
    CONSTRAINT chk_ad_campaigns_frequency_cap CHECK (frequency_cap IS NULL OR frequency_cap > 0),
    CONSTRAINT chk_ad_campaigns_date_range CHECK (end_date > start_date),
    CONSTRAINT chk_ad_campaigns_budget_limit CHECK (budget_spent <= budget_total)
);

-- Create indexes for ad campaigns
CREATE INDEX idx_ad_campaigns_advertiser_id ON ad_schema.ad_campaigns(advertiser_id);
CREATE INDEX idx_ad_campaigns_status ON ad_schema.ad_campaigns(status);
CREATE INDEX idx_ad_campaigns_campaign_type ON ad_schema.ad_campaigns(campaign_type);
CREATE INDEX idx_ad_campaigns_objective ON ad_schema.ad_campaigns(objective);
CREATE INDEX idx_ad_campaigns_dates ON ad_schema.ad_campaigns(start_date, end_date);
CREATE INDEX idx_ad_campaigns_priority ON ad_schema.ad_campaigns(priority_level);
CREATE INDEX idx_ad_campaigns_created_at ON ad_schema.ad_campaigns(created_at);

-- Create ad analytics table
CREATE TABLE ad_schema.ad_analytics (
    id BIGSERIAL PRIMARY KEY,
    advertisement_id BIGINT NOT NULL REFERENCES ad_schema.advertisements(id) ON DELETE CASCADE,
    campaign_id BIGINT REFERENCES ad_schema.ad_campaigns(id) ON DELETE SET NULL,
    date_recorded DATE NOT NULL,
    impressions INTEGER NOT NULL DEFAULT 0,
    views INTEGER NOT NULL DEFAULT 0,
    clicks INTEGER NOT NULL DEFAULT 0,
    completions INTEGER NOT NULL DEFAULT 0,
    skips INTEGER NOT NULL DEFAULT 0,
    engagements INTEGER NOT NULL DEFAULT 0,
    unique_users INTEGER NOT NULL DEFAULT 0,
    total_duration_seconds BIGINT NOT NULL DEFAULT 0,
    average_completion_percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    tickets_multiplied INTEGER NOT NULL DEFAULT 0,
    cost_spent DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    revenue_generated DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    conversion_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    engagement_rate DECIMAL(5,4) NOT NULL DEFAULT 0.0000,
    fraud_detections INTEGER NOT NULL DEFAULT 0,
    quality_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_ad_analytics_impressions CHECK (impressions >= 0),
    CONSTRAINT chk_ad_analytics_views CHECK (views >= 0 AND views <= impressions),
    CONSTRAINT chk_ad_analytics_clicks CHECK (clicks >= 0),
    CONSTRAINT chk_ad_analytics_completions CHECK (completions >= 0 AND completions <= views),
    CONSTRAINT chk_ad_analytics_skips CHECK (skips >= 0),
    CONSTRAINT chk_ad_analytics_engagements CHECK (engagements >= 0),
    CONSTRAINT chk_ad_analytics_unique_users CHECK (unique_users >= 0),
    CONSTRAINT chk_ad_analytics_total_duration CHECK (total_duration_seconds >= 0),
    CONSTRAINT chk_ad_analytics_completion_percentage CHECK (
        average_completion_percentage >= 0.00 AND average_completion_percentage <= 100.00
    ),
    CONSTRAINT chk_ad_analytics_tickets_multiplied CHECK (tickets_multiplied >= 0),
    CONSTRAINT chk_ad_analytics_cost_spent CHECK (cost_spent >= 0),
    CONSTRAINT chk_ad_analytics_revenue_generated CHECK (revenue_generated >= 0),
    CONSTRAINT chk_ad_analytics_conversion_rate CHECK (
        conversion_rate >= 0.0000 AND conversion_rate <= 1.0000
    ),
    CONSTRAINT chk_ad_analytics_engagement_rate CHECK (
        engagement_rate >= 0.0000 AND engagement_rate <= 1.0000
    ),
    CONSTRAINT chk_ad_analytics_fraud_detections CHECK (fraud_detections >= 0),
    CONSTRAINT chk_ad_analytics_quality_score CHECK (
        quality_score >= 0.00 AND quality_score <= 100.00
    ),
    CONSTRAINT uk_ad_analytics_ad_date UNIQUE (advertisement_id, date_recorded)
);

-- Create indexes for ad analytics
CREATE INDEX idx_ad_analytics_advertisement_id ON ad_schema.ad_analytics(advertisement_id);
CREATE INDEX idx_ad_analytics_campaign_id ON ad_schema.ad_analytics(campaign_id);
CREATE INDEX idx_ad_analytics_date_recorded ON ad_schema.ad_analytics(date_recorded);
CREATE INDEX idx_ad_analytics_impressions ON ad_schema.ad_analytics(impressions);
CREATE INDEX idx_ad_analytics_engagement_rate ON ad_schema.ad_analytics(engagement_rate);
CREATE INDEX idx_ad_analytics_quality_score ON ad_schema.ad_analytics(quality_score);
CREATE INDEX idx_ad_analytics_created_at ON ad_schema.ad_analytics(created_at);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION ad_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_advertisements_updated_at
    BEFORE UPDATE ON ad_schema.advertisements
    FOR EACH ROW
    EXECUTE FUNCTION ad_schema.update_updated_at_column();

CREATE TRIGGER update_ad_engagements_updated_at
    BEFORE UPDATE ON ad_schema.ad_engagements
    FOR EACH ROW
    EXECUTE FUNCTION ad_schema.update_updated_at_column();

CREATE TRIGGER update_ad_campaigns_updated_at
    BEFORE UPDATE ON ad_schema.ad_campaigns
    FOR EACH ROW
    EXECUTE FUNCTION ad_schema.update_updated_at_column();

CREATE TRIGGER update_ad_analytics_updated_at
    BEFORE UPDATE ON ad_schema.ad_analytics
    FOR EACH ROW
    EXECUTE FUNCTION ad_schema.update_updated_at_column();

-- Create views for common queries
CREATE VIEW ad_schema.active_advertisements AS
SELECT
    a.id,
    a.title,
    a.description,
    a.ad_type,
    a.content_url,
    a.thumbnail_url,
    a.duration_seconds,
    a.advertiser_name,
    a.campaign_name,
    a.ticket_multiplier,
    a.priority_level,
    a.start_date,
    a.end_date,
    a.content_rating,
    a.language_code,
    a.created_at
FROM ad_schema.advertisements a
WHERE a.status = 'ACTIVE'
  AND a.approval_status = 'APPROVED'
  AND a.start_date <= CURRENT_TIMESTAMP
  AND a.end_date > CURRENT_TIMESTAMP
  AND (a.budget_total IS NULL OR a.budget_spent < a.budget_total);

CREATE VIEW ad_schema.engagement_summary AS
SELECT
    ae.advertisement_id,
    a.title as advertisement_title,
    a.advertiser_name,
    COUNT(*) as total_engagements,
    COUNT(CASE WHEN ae.status = 'COMPLETED' THEN 1 END) as completed_engagements,
    COUNT(CASE WHEN ae.status = 'SKIPPED' THEN 1 END) as skipped_engagements,
    AVG(ae.completion_percentage) as avg_completion_percentage,
    AVG(ae.duration_seconds) as avg_duration_seconds,
    SUM(ae.tickets_multiplied) as total_tickets_multiplied,
    COUNT(DISTINCT ae.user_id) as unique_users
FROM ad_schema.ad_engagements ae
JOIN ad_schema.advertisements a ON ae.advertisement_id = a.id
WHERE ae.is_fraudulent = false
GROUP BY ae.advertisement_id, a.title, a.advertiser_name;

CREATE VIEW ad_schema.daily_ad_performance AS
SELECT
    DATE(ae.started_at) as engagement_date,
    ae.advertisement_id,
    a.title as advertisement_title,
    a.advertiser_name,
    COUNT(*) as total_engagements,
    COUNT(CASE WHEN ae.completion_percentage >= 80 THEN 1 END) as quality_engagements,
    SUM(ae.tickets_multiplied) as tickets_multiplied,
    COUNT(DISTINCT ae.user_id) as unique_users,
    AVG(ae.completion_percentage) as avg_completion_percentage
FROM ad_schema.ad_engagements ae
JOIN ad_schema.advertisements a ON ae.advertisement_id = a.id
WHERE ae.is_fraudulent = false
GROUP BY DATE(ae.started_at), ae.advertisement_id, a.title, a.advertiser_name;

-- Insert sample data for development
INSERT INTO ad_schema.ad_campaigns (
    name, description, advertiser_id, advertiser_name, campaign_type, objective,
    budget_total, budget_daily, target_engagements, start_date, end_date, status
) VALUES
(
    'Summer Fuel Savings',
    'Promote fuel savings during summer season',
    1001, 'Gasolinera JSM Marketing', 'STANDARD', 'ENGAGEMENT',
    5000.00, 100.00, 1000,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days', 'ACTIVE'
),
(
    'New Customer Welcome',
    'Welcome campaign for new customers',
    1002, 'JSM Customer Acquisition', 'PROMOTIONAL', 'CONVERSIONS',
    3000.00, 75.00, 500,
    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE'
) ON CONFLICT DO NOTHING;

INSERT INTO ad_schema.advertisements (
    title, description, ad_type, content_url, thumbnail_url, duration_seconds,
    advertiser_id, advertiser_name, campaign_id, campaign_name, ticket_multiplier,
    priority_level, status, start_date, end_date, approval_status, content_rating
) VALUES
(
    'Save Big on Premium Fuel',
    'Discover how premium fuel can save you money in the long run',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/premium-fuel-savings.mp4',
    'https://cdn.gasolinerajsm.com/ads/premium-fuel-thumb.jpg', 30,
    1001, 'Gasolinera JSM Marketing', 1, 'Summer Fuel Savings', 2,
    8, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '60 days',
    'APPROVED', 'G'
),
(
    'Welcome to JSM Family',
    'Join the JSM family and enjoy exclusive benefits',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/welcome-family.mp4',
    'https://cdn.gasolinerajsm.com/ads/welcome-thumb.jpg', 25,
    1002, 'JSM Customer Acquisition', 2, 'New Customer Welcome', 3,
    9, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '30 days',
    'APPROVED', 'G'
) ON CONFLICT DO NOTHING;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA ad_schema TO ad_engine_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ad_schema TO ad_engine_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ad_schema TO ad_engine_user;