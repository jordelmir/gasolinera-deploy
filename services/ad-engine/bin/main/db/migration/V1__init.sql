
CREATE TABLE advertisers (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_info VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE ad_campaigns (
    id UUID PRIMARY KEY,
    advertiser_id UUID REFERENCES advertisers(id),
    name VARCHAR(255) NOT NULL,
    start_date DATE,
    end_date DATE,
    budget NUMERIC(19, 4),
    targeting_rules JSONB,
    ad_creative_url VARCHAR(255),
    cpm_target NUMERIC(10, 4),
    active BOOLEAN DEFAULT true
);

CREATE TABLE ad_impressions (
    id UUID PRIMARY KEY,
    campaign_id UUID REFERENCES ad_campaigns(id),
    user_id UUID,
    station_id VARCHAR(255),
    revenue_generated NUMERIC(10, 4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
