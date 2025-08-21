CREATE TABLE coupon_redemptions (
    id UUID PRIMARY KEY,
    coupon_id UUID NOT NULL,
    user_id UUID NOT NULL,
    redeemed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
