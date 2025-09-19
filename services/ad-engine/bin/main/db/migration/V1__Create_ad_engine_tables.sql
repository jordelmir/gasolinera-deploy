-- V1__Create_ad_engine_tables.sql

CREATE TABLE advertisers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    contact_email VARCHAR(255)
);

CREATE TABLE ad_campaigns (
    id BIGSERIAL PRIMARY KEY,
    advertiser_id BIGINT NOT NULL REFERENCES advertisers(id),
    name VARCHAR(255) NOT NULL,
    ad_url VARCHAR(2048) NOT NULL,
    station_id BIGINT, -- Can be null for global campaigns
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ad_impressions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL REFERENCES ad_campaigns(id),
    creative_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
