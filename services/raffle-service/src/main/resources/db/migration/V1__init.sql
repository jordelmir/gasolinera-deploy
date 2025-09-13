
-- V1__Create_raffle_schema.sql
-- Create the raffle schema and initial tables for the raffle service

-- Create the raffle schema
CREATE SCHEMA IF NOT EXISTS raffle_schema;

-- Set the search path to include the raffle schema
SET search_path TO raffle_schema, public;

-- Create the raffles table
CREATE TABLE raffle_schema.raffles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    raffle_type VARCHAR(30) NOT NULL DEFAULT 'STANDARD',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    registration_start TIMESTAMP NOT NULL,
    registration_end TIMESTAMP NOT NULL,
    draw_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP,
    min_tickets_to_participate INTEGER NOT NULL DEFAULT 1,
    max_tickets_per_user INTEGER,
    max_participants INTEGER,
    current_participants INTEGER NOT NULL DEFAULT 0,
    total_tickets_issued INTEGER NOT NULL DEFAULT 0,
    entry_fee DECIMAL(10,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    requires_verification BOOLEAN NOT NULL DEFAULT false,
    is_public BOOLEAN NOT NULL DEFAULT true,
    eligibility_criteria JSONB,
    terms_and_conditions TEXT,
    prize_pool_total DECIMAL(12,2),
    draw_algorithm VARCHAR(50) NOT NULL DEFAULT 'RANDOM',
    seed_source VARCHAR(100),
    seed_value VARCHAR(255),
    merkle_root VARCHAR(255),
    draw_metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_raffles_raffle_type CHECK (
        raffle_type IN ('STANDARD', 'PREMIUM', 'SPECIAL_EVENT', 'PROMOTIONAL', 'CHARITY')
    ),
    CONSTRAINT chk_raffles_status CHECK (
        status IN ('DRAFT', 'OPEN', 'CLOSED', 'DRAWING', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_raffles_registration_period CHECK (registration_end > registration_start),
    CONSTRAINT chk_raffles_draw_date CHECK (draw_date >= registration_end),
    CONSTRAINT chk_raffles_min_tickets CHECK (min_tickets_to_participate >= 1),
    CONSTRAINT chk_raffles_max_tickets_per_user CHECK (max_tickets_per_user IS NULL OR max_tickets_per_user >= min_tickets_to_participate),
    CONSTRAINT chk_raffles_max_participants CHECK (max_participants IS NULL OR max_participants > 0),
    CONSTRAINT chk_raffles_current_participants CHECK (current_participants >= 0),
    CONSTRAINT chk_raffles_total_tickets CHECK (total_tickets_issued >= 0),
    CONSTRAINT chk_raffles_entry_fee CHECK (entry_fee IS NULL OR entry_fee >= 0),
    CONSTRAINT chk_raffles_prize_pool CHECK (prize_pool_total IS NULL OR prize_pool_total >= 0),
    CONSTRAINT chk_raffles_draw_algorithm CHECK (
        draw_algorithm IN ('RANDOM', 'WEIGHTED', 'MERKLE_TREE', 'PROVABLY_FAIR')
    )
);

-- Create indexes for raffles
CREATE INDEX idx_raffles_status ON raffle_schema.raffles(status);
CREATE INDEX idx_raffles_registration_dates ON raffle_schema.raffles(registration_start, registration_end);
CREATE INDEX idx_raffles_draw_date ON raffle_schema.raffles(draw_date);
CREATE INDEX idx_raffles_raffle_type ON raffle_schema.raffles(raffle_type);
CREATE INDEX idx_raffles_is_public ON raffle_schema.raffles(is_public);
CREATE INDEX idx_raffles_created_at ON raffle_schema.raffles(created_at);

-- Create the raffle tickets table
CREATE TABLE raffle_schema.raffle_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    raffle_id BIGINT NOT NULL REFERENCES raffle_schema.raffles(id) ON DELETE CASCADE,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source_type VARCHAR(30) NOT NULL,
    source_reference VARCHAR(100),
    coupon_id BIGINT,
    campaign_id BIGINT,
    station_id BIGINT,
    transaction_reference VARCHAR(100),
    purchase_amount DECIMAL(10,2),
    verification_code VARCHAR(10),
    is_verified BOOLEAN NOT NULL DEFAULT false,
    verified_at TIMESTAMP,
    verified_by VARCHAR(100),
    is_winner BOOLEAN NOT NULL DEFAULT false,
    prize_claimed BOOLEAN NOT NULL DEFAULT false,
    prize_claim_date TIMESTAMP,
    notes VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_raffle_tickets_status CHECK (
        status IN ('ACTIVE', 'USED', 'EXPIRED', 'CANCELLED', 'SUSPENDED')
    ),
    CONSTRAINT chk_raffle_tickets_source_type CHECK (
        source_type IN ('COUPON_REDEMPTION', 'DIRECT_PURCHASE', 'PROMOTIONAL', 'AD_ENGAGEMENT', 'BONUS')
    ),
    CONSTRAINT chk_raffle_tickets_purchase_amount CHECK (purchase_amount IS NULL OR purchase_amount >= 0),
    CONSTRAINT chk_raffle_tickets_verification_code_format CHECK (
        verification_code IS NULL OR LENGTH(verification_code) = 6
    ),
    CONSTRAINT chk_raffle_tickets_verified_consistency CHECK (
        (is_verified = false AND verified_at IS NULL) OR
        (is_verified = true AND verified_at IS NOT NULL)
    ),
    CONSTRAINT chk_raffle_tickets_prize_claim_consistency CHECK (
        (prize_claimed = false AND prize_claim_date IS NULL) OR
        (prize_claimed = true AND prize_claim_date IS NOT NULL)
    )
);

-- Create indexes for raffle tickets
CREATE INDEX idx_raffle_tickets_user_id ON raffle_schema.raffle_tickets(user_id);
CREATE INDEX idx_raffle_tickets_raffle_id ON raffle_schema.raffle_tickets(raffle_id);
CREATE INDEX idx_raffle_tickets_ticket_number ON raffle_schema.raffle_tickets(ticket_number);
CREATE INDEX idx_raffle_tickets_status ON raffle_schema.raffle_tickets(status);
CREATE INDEX idx_raffle_tickets_source_type ON raffle_schema.raffle_tickets(source_type);
CREATE INDEX idx_raffle_tickets_coupon_id ON raffle_schema.raffle_tickets(coupon_id);
CREATE INDEX idx_raffle_tickets_station_id ON raffle_schema.raffle_tickets(station_id);
CREATE INDEX idx_raffle_tickets_verification_code ON raffle_schema.raffle_tickets(verification_code);
CREATE INDEX idx_raffle_tickets_is_verified ON raffle_schema.raffle_tickets(is_verified);
CREATE INDEX idx_raffle_tickets_is_winner ON raffle_schema.raffle_tickets(is_winner);
CREATE INDEX idx_raffle_tickets_prize_claimed ON raffle_schema.raffle_tickets(prize_claimed);
CREATE INDEX idx_raffle_tickets_created_at ON raffle_schema.raffle_tickets(created_at);

-- Create the raffle prizes table
CREATE TABLE raffle_schema.raffle_prizes (
    id BIGSERIAL PRIMARY KEY,
    raffle_id BIGINT NOT NULL REFERENCES raffle_schema.raffles(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    prize_type VARCHAR(30) NOT NULL,
    value DECIMAL(10,2),
    currency VARCHAR(3) NOT NULL DEFAULT 'MXN',
    quantity INTEGER NOT NULL DEFAULT 1,
    remaining_quantity INTEGER NOT NULL,
    prize_rank INTEGER NOT NULL,
    probability DECIMAL(8,6),
    image_url VARCHAR(500),
    terms_and_conditions TEXT,
    sponsor_name VARCHAR(200),
    sponsor_logo_url VARCHAR(500),
    fulfillment_method VARCHAR(50) NOT NULL DEFAULT 'MANUAL',
    fulfillment_data JSONB,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_raffle_prizes_prize_type CHECK (
        prize_type IN ('CASH', 'FUEL_CREDIT', 'MERCHANDISE', 'DISCOUNT_COUPON', 'GIFT_CARD', 'SERVICE', 'OTHER')
    ),
    CONSTRAINT chk_raffle_prizes_value CHECK (value IS NULL OR value >= 0),
    CONSTRAINT chk_raffle_prizes_quantity CHECK (quantity > 0),
    CONSTRAINT chk_raffle_prizes_remaining_quantity CHECK (
        remaining_quantity >= 0 AND remaining_quantity <= quantity
    ),
    CONSTRAINT chk_raffle_prizes_prize_rank CHECK (prize_rank > 0),
    CONSTRAINT chk_raffle_prizes_probability CHECK (
        probability IS NULL OR (probability >= 0 AND probability <= 1)
    ),
    CONSTRAINT chk_raffle_prizes_fulfillment_method CHECK (
        fulfillment_method IN ('MANUAL', 'AUTOMATIC', 'DIGITAL', 'PHYSICAL', 'CREDIT')
    )
);

-- Create indexes for raffle prizes
CREATE INDEX idx_raffle_prizes_raffle_id ON raffle_schema.raffle_prizes(raffle_id);
CREATE INDEX idx_raffle_prizes_prize_type ON raffle_schema.raffle_prizes(prize_type);
CREATE INDEX idx_raffle_prizes_prize_rank ON raffle_schema.raffle_prizes(prize_rank);
CREATE INDEX idx_raffle_prizes_is_active ON raffle_schema.raffle_prizes(is_active);
CREATE INDEX idx_raffle_prizes_remaining_quantity ON raffle_schema.raffle_prizes(remaining_quantity);
CREATE INDEX idx_raffle_prizes_created_at ON raffle_schema.raffle_prizes(created_at);

-- Create the raffle winners table
CREATE TABLE raffle_schema.raffle_winners (
    id BIGSERIAL PRIMARY KEY,
    raffle_id BIGINT NOT NULL REFERENCES raffle_schema.raffles(id) ON DELETE CASCADE,
    prize_id BIGINT NOT NULL REFERENCES raffle_schema.raffle_prizes(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    winning_ticket_id BIGINT NOT NULL REFERENCES raffle_schema.raffle_tickets(id) ON DELETE CASCADE,
    winning_ticket_number VARCHAR(50) NOT NULL,
    prize_name VARCHAR(200) NOT NULL,
    prize_value DECIMAL(10,2),
    draw_number INTEGER NOT NULL DEFAULT 1,
    winning_position INTEGER NOT NULL,
    selection_algorithm VARCHAR(50) NOT NULL,
    selection_seed VARCHAR(255),
    selection_metadata JSONB,
    notification_sent BOOLEAN NOT NULL DEFAULT false,
    notification_sent_at TIMESTAMP,
    prize_claimed BOOLEAN NOT NULL DEFAULT false,
    prize_claimed_at TIMESTAMP,
    prize_delivered BOOLEAN NEFAULT false,
    prize_delivered_at TIMESTAMP,
    delivery_method VARCHAR(50),
    delivery_reference VARCHAR(100),
    delivery_address TEXT,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_raffle_winners_prize_value CHECK (prize_value IS NULL OR prize_value >= 0),
    CONSTRAINT chk_raffle_winners_draw_number CHECK (draw_number > 0),
    CONSTRAINT chk_raffle_winners_winning_position CHECK (winning_position > 0),
    CONSTRAINT chk_raffle_winners_selection_algorithm CHECK (
        selection_algorithm IN ('RANDOM', 'WEIGHTED', 'MERKLE_TREE', 'PROVABLY_FAIR')
    ),
    CONSTRAINT chk_raffle_winners_notification_consistency CHECK (
        (notification_sent = false AND notification_sent_at IS NULL) OR
        (notification_sent = true AND notification_sent_at IS NOT NULL)
    ),
    CONSTRAINT chk_raffle_winners_claim_consistency CHECK (
        (prize_claimed = false AND prize_claimed_at IS NULL) OR
        (prize_claimed = true AND prize_claimed_at IS NOT NULL)
    ),
    CONSTRAINT chk_raffle_winners_delivery_consistency CHECK (
        (prize_delivered = false AND prize_delivered_at IS NULL) OR
        (prize_delivered = true AND prize_delivered_at IS NOT NULL)
    ),
    CONSTRAINT chk_raffle_winners_delivery_method CHECK (
        delivery_method IS NULL OR delivery_method IN ('PICKUP', 'MAIL', 'EMAIL', 'DIGITAL', 'CREDIT')
    )
);

-- Create indexes for raffle winners
CREATE INDEX idx_raffle_winners_raffle_id ON raffle_schema.raffle_winners(raffle_id);
CREATE INDEX idx_raffle_winners_prize_id ON raffle_schema.raffle_winners(prize_id);
CREATE INDEX idx_raffle_winners_user_id ON raffle_schema.raffle_winners(user_id);
CREATE INDEX idx_raffle_winners_winning_ticket_id ON raffle_schema.raffle_winners(winning_ticket_id);
CREATE INDEX idx_raffle_winners_draw_number ON raffle_schema.raffle_winners(draw_number);
CREATE INDEX idx_raffle_winners_winning_position ON raffle_schema.raffle_winners(winning_position);
CREATE INDEX idx_raffle_winners_notification_sent ON raffle_schema.raffle_winners(notification_sent);
CREATE INDEX idx_raffle_winners_prize_claimed ON raffle_schema.raffle_winners(prize_claimed);
CREATE INDEX idx_raffle_winners_prize_delivered ON raffle_schema.raffle_winners(prize_delivered);
CREATE INDEX idx_raffle_winners_created_at ON raffle_schema.raffle_winners(created_at);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION raffle_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_raffles_updated_at
    BEFORE UPDATE ON raffle_schema.raffles
    FOR EACH ROW
    EXECUTE FUNCTION raffle_schema.update_updated_at_column();

CREATE TRIGGER update_raffle_tickets_updated_at
    BEFORE UPDATE ON raffle_schema.raffle_tickets
    FOR EACH ROW
    EXECUTE FUNCTION raffle_schema.update_updated_at_column();

CREATE TRIGGER update_raffle_prizes_updated_at
    BEFORE UPDATE ON raffle_schema.raffle_prizes
    FOR EACH ROW
    EXECUTE FUNCTION raffle_schema.update_updated_at_column();

CREATE TRIGGER update_raffle_winners_updated_at
    BEFORE UPDATE ON raffle_schema.raffle_winners
    FOR EACH ROW
    EXECUTE FUNCTION raffle_schema.update_updated_at_column();

-- Create function to update raffle participant count
CREATE OR REPLACE FUNCTION raffle_schema.update_raffle_participant_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE raffle_schema.raffles
        SET current_participants = (
            SELECT COUNT(DISTINCT user_id)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = NEW.raffle_id AND status = 'ACTIVE'
        ),
        total_tickets_issued = (
            SELECT COUNT(*)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = NEW.raffle_id AND status = 'ACTIVE'
        )
        WHERE id = NEW.raffle_id;
        RETURN NEW;
    ELSIF TG_OP = 'UPDATE' THEN
        UPDATE raffle_schema.raffles
        SET current_participants = (
            SELECT COUNT(DISTINCT user_id)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = NEW.raffle_id AND status = 'ACTIVE'
        ),
        total_tickets_issued = (
            SELECT COUNT(*)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = NEW.raffle_id AND status = 'ACTIVE'
        )
        WHERE id = NEW.raffle_id;
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE raffle_schema.raffles
        SET current_participants = (
            SELECT COUNT(DISTINCT user_id)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = OLD.raffle_id AND status = 'ACTIVE'
        ),
        total_tickets_issued = (
            SELECT COUNT(*)
            FROM raffle_schema.raffle_tickets
            WHERE raffle_id = OLD.raffle_id AND status = 'ACTIVE'
        )
        WHERE id = OLD.raffle_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ language 'plpgsql';

-- Create trigger to update participant count
CREATE TRIGGER update_raffle_participant_count_trigger
    AFTER INSERT OR UPDATE OR DELETE ON raffle_schema.raffle_tickets
    FOR EACH ROW
    EXECUTE FUNCTION raffle_schema.update_raffle_participant_count();

-- Create views for common queries
CREATE VIEW raffle_schema.active_raffles AS
SELECT
    r.id,
    r.name,
    r.description,
    r.raffle_type,
    r.registration_start,
    r.registration_end,
    r.draw_date,
    r.min_tickets_to_participate,
    r.max_tickets_per_user,
    r.max_participants,
    r.current_participants,
    r.total_tickets_issued,
    r.entry_fee,
    r.currency,
    r.is_public,
    r.prize_pool_total,
    r.created_at
FROM raffle_schema.raffles r
WHERE r.status = 'OPEN'
  AND r.registration_start <= CURRENT_TIMESTAMP
  AND r.registration_end > CURRENT_TIMESTAMP
  AND r.is_public = true;

CREATE VIEW raffle_schema.raffle_summary AS
SELECT
    r.id,
    r.name,
    r.status,
    r.raffle_type,
    r.draw_date,
    r.current_participants,
    r.total_tickets_issued,
    r.prize_pool_total,
    COUNT(rp.id) as total_prizes,
    COUNT(rw.id) as winners_selected,
    COUNT(CASE WHEN rw.prize_claimed THEN 1 END) as prizes_claimed
FROM raffle_schema.raffles r
LEFT JOIN raffle_schema.raffle_prizes rp ON r.id = rp.raffle_id AND rp.is_active = true
LEFT JOIN raffle_schema.raffle_winners rw ON r.id = rw.raffle_id
GROUP BY r.id, r.name, r.status, r.raffle_type, r.draw_date,
         r.current_participants, r.total_tickets_issued, r.prize_pool_total;

CREATE VIEW raffle_schema.user_ticket_summary AS
SELECT
    rt.user_id,
    rt.raffle_id,
    r.name as raffle_name,
    COUNT(*) as ticket_count,
    COUNT(CASE WHEN rt.status = 'ACTIVE' THEN 1 END) as active_tickets,
    COUNT(CASE WHEN rt.is_winner THEN 1 END) as winning_tickets,
    COUNT(CASE WHEN rt.prize_claimed THEN 1 END) as claimed_prizes,
    MIN(rt.created_at) as first_ticket_date,
    MAX(rt.created_at) as last_ticket_date
FROM raffle_schema.raffle_tickets rt
JOIN raffle_schema.raffles r ON rt.raffle_id = r.id
GROUP BY rt.user_id, rt.raffle_id, r.name;

-- Insert sample data for development
INSERT INTO raffle_schema.raffles (
    name, description, raffle_type, status, registration_start, registration_end, draw_date,
    min_tickets_to_participate, max_tickets_per_user, max_participants, entry_fee, currency,
    requires_verification, is_public, prize_pool_total, draw_algorithm
) VALUES
(
    'Monthly Fuel Prize Draw',
    'Win free fuel for a month! Enter with your coupon redemptions.',
    'STANDARD', 'OPEN',
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP + INTERVAL '23 days',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    1, 10, 1000, NULL, 'MXN',
    false, true, 15000.00, 'RANDOM'
),
(
    'Grand Prize Raffle',
    'Our biggest raffle of the year with amazing prizes!',
    'PREMIUM', 'OPEN',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP + INTERVAL '57 days',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    5, 25, 500, NULL, 'MXN',
    true, true, 50000.00, 'PROVABLY_FAIR'
) ON CONFLICT DO NOTHING;

INSERT INTO raffle_schema.raffle_prizes (
    raffle_id, name, description, prize_type, value, currency, quantity, remaining_quantity,
    prize_rank, probability, fulfillment_method, is_active
) VALUES
(1, 'Free Fuel for a Month', '1000 MXN fuel credit', 'FUEL_CREDIT', 1000.00, 'MXN', 1, 1, 1, 0.001, 'CREDIT', true),
(1, 'Free Fuel for a Week', '250 MXN fuel credit', 'FUEL_CREDIT', 250.00, 'MXN', 4, 4, 2, 0.004, 'CREDIT', true),
(1, '20% Discount Coupon', '20% off next fuel purchase', 'DISCOUNT_COUPON', 50.00, 'MXN', 50, 50, 3, 0.05, 'DIGITAL', true),
(2, 'Brand New Car', 'Toyota Corolla 2024', 'MERCHANDISE', 350000.00, 'MXN', 1, 1, 1, 0.002, 'PHYSICAL', true),
(2, 'Motorcycle', 'Honda CB125F', 'MERCHANDISE', 45000.00, 'MXN', 2, 2, 2, 0.004, 'PHYSICAL', true),
(2, 'Smartphone', 'Latest iPhone', 'MERCHANDISE', 25000.00, 'MXN', 5, 5, 3, 0.01, 'PHYSICAL', true)
ON CONFLICT DO NOTHING;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA raffle_schema TO raffle_service_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA raffle_schema TO raffle_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA raffle_schema TO raffle_service_user;
