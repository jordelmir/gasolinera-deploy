-- V3__Create_points_ledger_table.sql

CREATE TABLE points_ledger (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    points_credited INT NOT NULL,
    redemption_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
