-- V2__Insert_seed_data.sql
-- Seed data for Raffle Service development environment

-- Insert additional test raffles
INSERT INTO raffle_schema.raffles (
    name, description, raffle_type, status, registration_start, registration_end, draw_date,
    min_tickets_to_participate, max_tickets_per_user, max_participants, entry_fee, currency,
    requires_verification, is_public, prize_pool_total, draw_algorithm, created_by
) VALUES
-- Weekly raffle (currently open)
(
    'Sorteo Semanal JSM',
    'Sorteo semanal con premios en efectivo y combustible gratis',
    'STANDARD', 'OPEN',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP + INTERVAL '4 days',
    CURRENT_TIMESTAMP + INTERVAL '7 days',
    1, 15, 500, NULL, 'MXN',
    false, true, 8000.00, 'RANDOM', 'system'
),

-- Special event raffle (registration closed, pending draw)
(
    'Gran Sorteo Aniversario',
    'Sorteo especial por el aniversario de Gasolinera JSM',
    'SPECIAL_EVENT', 'CLOSED',
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP - INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '2 days',
    5, 50, 200, NULL, 'MXN',
    true, true, 75000.00, 'PROVABLY_FAIR', 'system'
),

-- Completed raffle with winners
(
    'Sorteo de Año Nuevo',
    'Sorteo especial para celebrar el año nuevo',
    'SPECIAL_EVENT', 'COMPLETED',
    CURRENT_TIMESTAMP - INTERVAL '90 days',
    CURRENT_TIMESTAMP - INTERVAL '60 days',
    CURRENT_TIMESTAMP - INTERVAL '55 days',
    3, 20, 300, NULL, 'MXN',
    false, true, 25000.00, 'RANDOM', 'system'
),

-- Monthly raffle (upcoming)
(
    'Sorteo Mensual Marzo',
    'Sorteo mensual con premios especiales',
    'STANDARD', 'DRAFT',
    CURRENT_TIMESTAMP + INTERVAL '15 days',
    CURRENT_TIMESTAMP + INTERVAL '45 days',
    CURRENT_TIMESTAMP + INTERVAL '50 days',
    2, 25, 1000, NULL, 'MXN',
    false, true, 15000.00, 'RANDOM', 'system'
),

-- VIP raffle (exclusive)
(
    'Sorteo VIP Exclusivo',
    'Sorteo exclusivo para clientes VIP',
    'EXCLUSIVE', 'OPEN',
    CURRENT_TIMESTAMP - INTERVAL '7 days',
    CURRENT_TIMESTAMP + INTERVAL '14 days',
    CURRENT_TIMESTAMP + INTERVAL '21 days',
    10, 100, 50, NULL, 'MXN',
    true, false, 50000.00, 'PROVABLY_FAIR', 'system'
)

ON CONFLICT DO NOTHING;

-- Insert raffle prizes
INSERT INTO raffle_schema.raffle_prizes (
    raffle_id, prize_name, prize_description, prize_type, prize_value,
    quantity_available, winner_selection_order, is_grand_prize, created_by
) VALUES
-- Weekly raffle prizes
(1, 'Gran Premio Efectivo', '$5,000 pesos en efectivo', 'CASH', 5000.00, 1, 1, true, 'system'),
(1, 'Combustible Gratis', '500 litros de combustible gratis', 'FUEL_CREDIT', 500.00, 2, 2, false, 'system'),
(1, 'Tarjeta de Regalo', 'Tarjeta de regalo $1,000 pesos', 'GIFT_CARD', 1000.00, 5, 3, false, 'system'),
(1, 'Descuento Premium', '50% descuento en próxima carga', 'DISCOUNT_COUPON', 50.00, 10, 4, false, 'system'),

-- Anniversary raffle prizes
(2, 'Auto Nuevo', 'Automóvil Nissan Versa 2024', 'VEHICLE', 250000.00, 1, 1, true, 'system'),
(2, 'Efectivo Grande', '$25,000 pesos en efectivo', 'CASH', 25000.00, 2, 2, false, 'system'),
(2, 'Combustible Anual', '1 año de combustible gratis', 'FUEL_CREDIT', 15000.00, 3, 3, false, 'system'),
(2, 'Viaje Familiar', 'Viaje familiar a Cancún', 'TRAVEL_VOUCHER', 20000.00, 2, 4, false, 'system'),

-- New Year raffle prizes (completed)
(3, 'Efectivo Año Nuevo', '$10,000 pesos para empezar el año', 'CASH', 10000.00, 1, 1, true, 'system'),
(3, 'Combustible Mensual', '6 meses de combustible gratis', 'FUEL_CREDIT', 8000.00, 2, 2, false, 'system'),
(3, 'Electrodomésticos', 'Paquete de electrodomésticos', 'MERCHANDISE', 5000.00, 3, 3, false, 'system'),

-- VIP raffle prizes
(5, 'Experiencia VIP', 'Experiencia VIP en evento exclusivo', 'EXPERIENCE', 15000.00, 1, 1, true, 'system'),
(5, 'Combustible Premium', '1000 litros de combustible premium', 'FUEL_CREDIT', 1000.00, 2, 2, false, 'system'),
(5, 'Servicios Premium', 'Servicios premium por 1 año', 'SERVICE_CREDIT', 10000.00, 3, 3, false, 'system')

ON CONFLICT DO NOTHING;

-- Insert raffle tickets for active raffles
INSERT INTO raffle_schema.raffle_tickets (
    raffle_id, user_id, ticket_number, ticket_batch, source_type, source_id,
    purchase_amount, tickets_used, status, created_by
) VALUES
-- Weekly raffle tickets
(1, 11, 'WK001001', 'BATCH_WK_001', 'COUPON_REDEMPTION', 1, 0.00, 3, 'ACTIVE', 'system'),
(1, 12, 'WK001002', 'BATCH_WK_001', 'COUPON_REDEMPTION', 2, 0.00, 2, 'ACTIVE', 'system'),
(1, 13, 'WK001003', 'BATCH_WK_001', 'AD_ENGAGEMENT', 1, 0.00, 4, 'ACTIVE', 'system'),
(1, 14, 'WK001004', 'BATCH_WK_001', 'COUPON_REDEMPTION', 4, 0.00, 3, 'ACTIVE', 'system'),
(1, 15, 'WK001005', 'BATCH_WK_001', 'AD_ENGAGEMENT', 2, 0.00, 5, 'ACTIVE', 'system'),
(1, 16, 'WK001006', 'BATCH_WK_001', 'COUPON_REDEMPTION', 8, 0.00, 2, 'ACTIVE', 'system'),
(1, 17, 'WK001007', 'BATCH_WK_001', 'AD_ENGAGEMENT', 3, 0.00, 1, 'ACTIVE', 'system'),
(1, 18, 'WK001008', 'BATCH_WK_001', 'COUPON_REDEMPTION', 15, 0.00, 1, 'ACTIVE', 'system'),

-- Anniversary raffle tickets (closed)
(2, 21, 'AN002001', 'BATCH_AN_001', 'COUPON_REDEMPTION', 18, 0.00, 5, 'ACTIVE', 'system'),
(2, 22, 'AN002002', 'BATCH_AN_001', 'AD_ENGAGEMENT', 4, 0.00, 8, 'ACTIVE', 'system'),
(2, 11, 'AN002003', 'BATCH_AN_001', 'COUPON_REDEMPTION', 21, 0.00, 2, 'ACTIVE', 'system'),
(2, 12, 'AN002004', 'BATCH_AN_001', 'AD_ENGAGEMENT', 5, 0.00, 3, 'ACTIVE', 'system'),
(2, 13, 'AN002005', 'BATCH_AN_001', 'COUPON_REDEMPTION', 23, 0.00, 1, 'ACTIVE', 'system'),

-- VIP raffle tickets
(5, 21, 'VIP005001', 'BATCH_VIP_001', 'COUPON_REDEMPTION', 18, 0.00, 10, 'ACTIVE', 'system'),
(5, 22, 'VIP005002', 'BATCH_VIP_001', 'AD_ENGAGEMENT', 4, 0.00, 15, 'ACTIVE', 'system')

ON CONFLICT (ticket_number) DO NOTHING;

-- Insert raffle winners for completed raffle
INSERT INTO raffle_schema.raffle_winners (
    raffle_id, prize_id, user_id, ticket_number, winning_position,
    prize_claimed, claim_deadline, notification_sent, created_by
) VALUES
-- New Year raffle winners
(3, 7, 21, 'NY003001', 1, true, CURRENT_TIMESTAMP + INTERVAL '30 days', true, 'system'),
(3, 8, 22, 'NY003002', 2, false, CURRENT_TIMESTAMP + INTERVAL '30 days', true, 'system'),
(3, 8, 11, 'NY003003', 3, true, CURRENT_TIMESTAMP + INTERVAL '30 days', true, 'system'),
(3, 9, 12, 'NY003004', 4, false, CURRENT_TIMESTAMP + INTERVAL '30 days', false, 'system'),
(3, 9, 13, 'NY003005', 5, true, CURRENT_TIMESTAMP + INTERVAL '30 days', true, 'system'),
(3, 9, 14, 'NY003006', 6, false, CURRENT_TIMESTAMP + INTERVAL '30 days', true, 'system')

ON CONFLICT DO NOTHING;

-- Insert raffle entries for active raffles
INSERT INTO raffle_schema.raffle_entries (
    raffle_id, user_id, entry_date, tickets_used, entry_method, transaction_reference, created_by
) VALUES
-- Weekly raffle entries
(1, 11, CURRENT_TIMESTAMP - INTERVAL '2 days', 3, 'MOBILE_APP', 'ENTRY_WK_001', 'system'),
(1, 12, CURRENT_TIMESTAMP - INTERVAL '1 day', 2, 'MOBILE_APP', 'ENTRY_WK_002', 'system'),
(1, 13, CURRENT_TIMESTAMP - INTERVAL '3 hours', 4, 'WEB_PORTAL', 'ENTRY_WK_003', 'system'),
(1, 14, CURRENT_TIMESTAMP - INTERVAL '1 hour', 3, 'MOBILE_APP', 'ENTRY_WK_004', 'system'),
(1, 15, CURRENT_TIMESTAMP - INTERVAL '6 hours', 5, 'MOBILE_APP', 'ENTRY_WK_005', 'system'),
(1, 16, CURRENT_TIMESTAMP - INTERVAL '12 hours', 2, 'WEB_PORTAL', 'ENTRY_WK_006', 'system'),
(1, 17, CURRENT_TIMESTAMP - INTERVAL '4 hours', 1, 'MOBILE_APP', 'ENTRY_WK_007', 'system'),
(1, 18, CURRENT_TIMESTAMP - INTERVAL '8 hours', 1, 'WEB_PORTAL', 'ENTRY_WK_008', 'system'),

-- Anniversary raffle entries
(2, 21, CURRENT_TIMESTAMP - INTERVAL '25 days', 5, 'MOBILE_APP', 'ENTRY_AN_001', 'system'),
(2, 22, CURRENT_TIMESTAMP - INTERVAL '20 days', 8, 'WEB_PORTAL', 'ENTRY_AN_002', 'system'),
(2, 11, CURRENT_TIMESTAMP - INTERVAL '15 days', 2, 'MOBILE_APP', 'ENTRY_AN_003', 'system'),
(2, 12, CURRENT_TIMESTAMP - INTERVAL '10 days', 3, 'MOBILE_APP', 'ENTRY_AN_004', 'system'),
(2, 13, CURRENT_TIMESTAMP - INTERVAL '5 days', 1, 'WEB_PORTAL', 'ENTRY_AN_005', 'system'),

-- VIP raffle entries
(5, 21, CURRENT_TIMESTAMP - INTERVAL '5 days', 10, 'MOBILE_APP', 'ENTRY_VIP_001', 'system'),
(5, 22, CURRENT_TIMESTAMP - INTERVAL '3 days', 15, 'WEB_PORTAL', 'ENTRY_VIP_002', 'system')

ON CONFLICT (transaction_reference) DO NOTHING;

-- Update raffle statistics
UPDATE raffle_schema.raffles
SET current_participants = (
    SELECT COUNT(DISTINCT user_id)
    FROM raffle_schema.raffle_entries
    WHERE raffle_id = raffles.id
),
total_tickets_sold = (
    SELECT COALESCE(SUM(tickets_used), 0)
    FROM raffle_schema.raffle_entries
    WHERE raffle_id = raffles.id
);

-- Insert some raffle audit logs
INSERT INTO raffle_schema.raffle_audit_log (
    raffle_id, user_id, event_type, event_details, performed_by, created_at
) VALUES
(1, NULL, 'RAFFLE_CREATED', '{"raffle_type": "STANDARD", "prize_pool": 8000}', 'system', CURRENT_TIMESTAMP - INTERVAL '7 days'),
(1, NULL, 'RAFFLE_OPENED', '{"registration_start": "2024-01-01"}', 'system', CURRENT_TIMESTAMP - INTERVAL '3 days'),
(2, NULL, 'RAFFLE_CREATED', '{"raffle_type": "SPECIAL_EVENT", "prize_pool": 75000}', 'system', CURRENT_TIMESTAMP - INTERVAL '35 days'),
(2, NULL, 'RAFFLE_CLOSED', '{"registration_end": "2024-01-15"}', 'system', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(3, NULL, 'RAFFLE_COMPLETED', '{"winners_selected": 6, "draw_date": "2024-01-01"}', 'system', CURRENT_TIMESTAMP - INTERVAL '55 days'),
(3, 21, 'PRIZE_CLAIMED', '{"prize_id": 7, "prize_value": 10000}', 'system', CURRENT_TIMESTAMP - INTERVAL '50 days'),
(1, 11, 'ENTRY_SUBMITTED', '{"tickets_used": 3, "entry_method": "MOBILE_APP"}', 'system', CURRENT_TIMESTAMP - INTERVAL '2 days'),
(1, 12, 'ENTRY_SUBMITTED', '{"tickets_used": 2, "entry_method": "MOBILE_APP"}', 'system', CURRENT_TIMESTAMP - INTERVAL '1 day'),
(5, 21, 'ENTRY_SUBMITTED', '{"tickets_used": 10, "entry_method": "MOBILE_APP"}', 'system', CURRENT_TIMESTAMP - INTERVAL '5 days'),
(5, 22, 'ENTRY_SUBMITTED', '{"tickets_used": 15, "entry_method": "WEB_PORTAL"}', 'system', CURRENT_TIMESTAMP - INTERVAL '3 days')