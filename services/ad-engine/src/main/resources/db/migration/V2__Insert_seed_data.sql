-- V2__Insert_seed_data.sql
-- Seed data for Ad Engine Service development environment

-- Insert test ad campaigns
INSERT INTO ad_schema.ad_campaigns (
    name, description, advertiser_id, advertiser_name, campaign_type, objective,
    budget_total, budget_daily, target_engagements, start_date, end_date,
    status, priority_level, frequency_cap, created_by
) VALUES
-- Active campaigns
(
    'Promoción Combustible Premium',
    'Campaña para promover el uso de combustible premium',
    1001, 'Gasolinera JSM Marketing', 'STANDARD', 'ENGAGEMENT',
    15000.00, 200.00, 2000,
    CURRENT_TIMESTAMP - INTERVAL '15 days',
    CURRENT_TIMESTAMP + INTERVAL '45 days',
    'ACTIVE', 8, 3, 'system'
),
(
    'Bienvenida Nuevos Clientes',
    'Campaña de bienvenida para atraer nuevos clientes',
    1002, 'JSM Customer Acquisition', 'PROMOTIONAL', 'CONVERSIONS',
    8000.00, 150.00, 1000,
    CURRENT_TIMESTAMP - INTERVAL '10 days',
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    'ACTIVE', 9, 2, 'system'
),
(
    'Programa de Lealtad',
    'Promoción del programa de lealtad JSM',
    1001, 'Gasolinera JSM Marketing', 'PREMIUM', 'BRAND_AWARENESS',
    25000.00, 300.00, 3000,
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP + INTERVAL '60 days',
    'ACTIVE', 7, 5, 'system'
)-- M
ore campaigns
,(
    'Descuentos de Verano',
    'Promociones especiales para la temporada de verano',
    1003, 'JSM Seasonal Promotions', 'STANDARD', 'TRAFFIC',
    12000.00, 180.00, 1500,
    CURRENT_TIMESTAMP - INTERVAL '20 days',
    CURRENT_TIMESTAMP + INTERVAL '40 days',
    'ACTIVE', 6, 4, 'system'
),
(
    'Campaña Corporativa',
    'Dirigida a empresas y flotas comerciales',
    1004, 'JSM Corporate Sales', 'PREMIUM', 'CONVERSIONS',
    30000.00, 400.00, 500,
    CURRENT_TIMESTAMP - INTERVAL '45 days',
    CURRENT_TIMESTAMP + INTERVAL '90 days',
    'ACTIVE', 5, 10, 'system'
),

-- Test campaigns with different statuses
(
    'Campaña Pausada',
    'Campaña temporalmente pausada',
    1001, 'Gasolinera JSM Marketing', 'STANDARD', 'ENGAGEMENT',
    5000.00, 100.00, 800,
    CURRENT_TIMESTAMP - INTERVAL '5 days',
    CURRENT_TIMESTAMP + INTERVAL '25 days',
    'PAUSED', 4, 3, 'system'
),
(
    'Campaña Completada',
    'Campaña que ya finalizó',
    1002, 'JSM Customer Acquisition', 'PROMOTIONAL', 'REACH',
    10000.00, 200.00, 2000,
    CURRENT_TIMESTAMP - INTERVAL '90 days',
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    'COMPLETED', 8, 2, 'system'
),
(
    'Campaña Futura',
    'Campaña programada para el futuro',
    1003, 'JSM Seasonal Promotions', 'STANDARD', 'BRAND_AWARENESS',
    18000.00, 250.00, 2500,
    CURRENT_TIMESTAMP + INTERVAL '15 days',
    CURRENT_TIMESTAMP + INTERVAL '75 days',
    'DRAFT', 7, 3, 'system'
)

ON CONFLICT DO NOTHING;

-- Insert test advertisements
INSERT INTO ad_schema.advertisements (
    title, description, ad_type, content_url, thumbnail_url, duration_seconds,
    advertiser_id, advertiser_name, campaign_id, campaign_name, ticket_multiplier,
    priority_level, status, start_date, end_date, approval_status, content_rating,
    language_code, cost_per_engagement, max_engagements_per_user, frequency_cap, created_by
) VALUES
-- Premium fuel campaign ads
(
    'Descubre el Poder del Premium',
    'Experimenta la diferencia con nuestro combustible premium de alta calidad',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/premium-power.mp4',
    'https://cdn.gasolinerajsm.com/ads/premium-power-thumb.jpg', 30,
    1001, 'Gasolinera JSM Marketing', 1, 'Promoción Combustible Premium', 2,
    8, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days',
    'APPROVED', 'G', 'es', 2.50, 3, 3, 'system'
),
(
    'Rendimiento Superior',
    'Más kilómetros por litro con combustible premium JSM',
    'IMAGE', 'https://cdn.gasolinerajsm.com/ads/superior-performance.jpg',
    'https://cdn.gasolinerajsm.com/ads/superior-performance-thumb.jpg', NULL,
    1001, 'Gasolinera JSM Marketing', 1, 'Promoción Combustible Premium', 3,
    7, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP + INTERVAL '45 days',
    'APPROVED', 'G', 'es', 1.80, 5, 4, 'system'
),

-- Welcome campaign ads
(
    'Bienvenido a la Familia JSM',
    'Únete a miles de clientes satisfechos en Gasolinera JSM',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/welcome-family.mp4',
    'https://cdn.gasolinerajsm.com/ads/welcome-family-thumb.jpg', 25,
    1002, 'JSM Customer Acquisition', 2, 'Bienvenida Nuevos Clientes', 4,
    9, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '30 days',
    'APPROVED', 'G', 'es', 3.00, 2, 2, 'system'
),
(
    'Beneficios Exclusivos',
    'Descubre todos los beneficios de ser cliente JSM',
    'INTERACTIVE', 'https://cdn.gasolinerajsm.com/ads/exclusive-benefits.html',
    'https://cdn.gasolinerajsm.com/ads/exclusive-benefits-thumb.jpg', 45,
    1002, 'JSM Customer Acquisition', 2, 'Bienvenida Nuevos Clientes', 5,
    8, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP + INTERVAL '30 days',
    'APPROVED', 'G', 'es', 4.50, 1, 1, 'system'
),

-- Loyalty program ads
(
    'Programa de Lealtad JSM',
    'Acumula puntos y gana premios increíbles',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/loyalty-program.mp4',
    'https://cdn.gasolinerajsm.com/ads/loyalty-program-thumb.jpg', 35,
    1001, 'Gasolinera JSM Marketing', 3, 'Programa de Lealtad', 3,
    7, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '30 days', CURRENT_TIMESTAMP + INTERVAL '60 days',
    'APPROVED', 'G', 'es', 2.20, 8, 5, 'system'
),

-- Summer campaign ads
(
    'Verano JSM 2024',
    'Descuentos especiales para disfrutar el verano',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/summer-2024.mp4',
    'https://cdn.gasolinerajsm.com/ads/summer-2024-thumb.jpg', 28,
    1003, 'JSM Seasonal Promotions', 4, 'Descuentos de Verano', 2,
    6, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP + INTERVAL '40 days',
    'APPROVED', 'G', 'es', 1.90, 4, 4, 'system'
),

-- Corporate campaign ads
(
    'Soluciones Corporativas',
    'Servicios especializados para empresas y flotas',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/corporate-solutions.mp4',
    'https://cdn.gasolinerajsm.com/ads/corporate-solutions-thumb.jpg', 40,
    1004, 'JSM Corporate Sales', 5, 'Campaña Corporativa', 1,
    5, 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '45 days', CURRENT_TIMESTAMP + INTERVAL '90 days',
    'APPROVED', 'G', 'es', 8.00, 15, 10, 'system'
),

-- Test ads with different statuses
(
    'Anuncio Pausado',
    'Anuncio temporalmente pausado para pruebas',
    'IMAGE', 'https://cdn.gasolinerajsm.com/ads/paused-ad.jpg',
    'https://cdn.gasolinerajsm.com/ads/paused-ad-thumb.jpg', NULL,
    1001, 'Gasolinera JSM Marketing', 6, 'Campaña Pausada', 2,
    4, 'PAUSED', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP + INTERVAL '25 days',
    'APPROVED', 'G', 'es', 2.00, 3, 3, 'system'
),
(
    'Anuncio Pendiente',
    'Anuncio pendiente de aprobación',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/pending-ad.mp4',
    'https://cdn.gasolinerajsm.com/ads/pending-ad-thumb.jpg', 20,
    1002, 'JSM Customer Acquisition', 2, 'Bienvenida Nuevos Clientes', 3,
    6, 'INACTIVE', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '28 days',
    'PENDING', 'G', 'es', 2.80, 2, 2, 'system'
),
(
    'Anuncio Rechazado',
    'Anuncio rechazado por no cumplir políticas',
    'VIDEO', 'https://cdn.gasolinerajsm.com/ads/rejected-ad.mp4',
    'https://cdn.gasolinerajsm.com/ads/rejected-ad-thumb.jpg', 15,
    1003, 'JSM Seasonal Promotions', 4, 'Descuentos de Verano', 2,
    3, 'CANCELLED', CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP + INTERVAL '33 days',
    'REJECTED', 'PG', 'es', 1.50, 2, 2, 'system'
)

ON CONFLICT DO NOTHING;-- Inse
rt test ad engagements
INSERT INTO ad_schema.ad_engagements (
    advertisement_id, user_id, session_id, engagement_type, status,
    started_at, completed_at, duration_seconds, completion_percentage,
    interaction_count, tickets_multiplied, multiplier_applied,
    station_id, device_type, ip_address, user_agent, engagement_quality_score,
    fraud_score, is_fraudulent, created_by
) VALUES
-- Completed engagements
(1, 11, 'session_ad_001', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '2 hours', CURRENT_TIMESTAMP - INTERVAL '2 hours' + INTERVAL '30 seconds',
 30, 100, 2, 6, 2, 1, 'mobile', '192.168.1.101', 'JSM Mobile App v1.0', 95, 5, false, 'system'),

(2, 12, 'session_ad_002', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '4 hours', CURRENT_TIMESTAMP - INTERVAL '4 hours' + INTERVAL '25 seconds',
 25, 95, 1, 9, 3, 2, 'mobile', '192.168.1.102', 'JSM Mobile App v1.0', 90, 8, false, 'system'),

(3, 13, 'session_ad_003', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '6 hours', CURRENT_TIMESTAMP - INTERVAL '6 hours' + INTERVAL '25 seconds',
 25, 100, 3, 12, 4, 2, 'web', '192.168.1.103', 'Mozilla/5.0 Chrome', 98, 3, false, 'system'),

(4, 14, 'session_ad_004', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '45 seconds',
 45, 100, 5, 15, 5, 3, 'tablet', '192.168.1.104', 'JSM Tablet App v1.0', 100, 2, false, 'system'),

(5, 15, 'session_ad_005', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP - INTERVAL '8 hours' + INTERVAL '35 seconds',
 35, 100, 2, 9, 3, 1, 'mobile', '192.168.1.105', 'JSM Mobile App v1.0', 92, 6, false, 'system'),

-- Partial engagements
(1, 16, 'session_ad_006', 'VIEW', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '3 hours', CURRENT_TIMESTAMP - INTERVAL '3 hours' + INTERVAL '20 seconds',
 20, 67, 1, 0, 1, 1, 'mobile', '192.168.1.106', 'JSM Mobile App v1.0', 60, 15, false, 'system'),

(2, 17, 'session_ad_007', 'VIEW', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '5 hours', CURRENT_TIMESTAMP - INTERVAL '5 hours' + INTERVAL '15 seconds',
 15, 60, 0, 0, 1, 2, 'web', '192.168.1.107', 'Mozilla/5.0 Firefox', 45, 20, false, 'system'),

-- Skipped engagements
(3, 18, 'session_ad_008', 'SKIP', 'SKIPPED',
 CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '1 hour' + INTERVAL '5 seconds',
 5, 20, 0, 0, 1, 3, 'mobile', '192.168.1.108', 'JSM Mobile App v1.0', 10, 25, false, 'system'),

(6, 19, 'session_ad_009', 'SKIP', 'SKIPPED',
 CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP - INTERVAL '30 minutes' + INTERVAL '3 seconds',
 3, 15, 0, 0, 1, 4, 'web', '192.168.1.109', 'Mozilla/5.0 Safari', 8, 30, false, 'system'),

-- Abandoned engagements
(1, 20, 'session_ad_010', 'VIEW', 'ABANDONED',
 CURRENT_TIMESTAMP - INTERVAL '45 minutes', NULL,
 NULL, 0, 0, 0, 1, 1, 'mobile', '192.168.1.110', 'JSM Mobile App v1.0', 0, 10, false, 'system'),

-- Fraudulent engagements
(2, 99, 'session_ad_011', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '12 hours', CURRENT_TIMESTAMP - INTERVAL '12 hours' + INTERVAL '1 second',
 1, 100, 0, 0, 1, 1, 'unknown', '10.0.0.1', 'Suspicious Bot v1.0', 5, 95, true, 'system'),

(3, 99, 'session_ad_012', 'COMPLETION', 'COMPLETED',
 CURRENT_TIMESTAMP - INTERVAL '11 hours', CURRENT_TIMESTAMP - INTERVAL '11 hours' + INTERVAL '2 seconds',
 2, 100, 0, 0, 1, 1, 'unknown', '10.0.0.1', 'Suspicious Bot v1.0', 3, 98, true, 'system')

ON CONFLICT DO NOTHING;

-- Insert ad analytics data
INSERT INTO ad_schema.ad_analytics (
    advertisement_id, campaign_id, date_recorded, impressions, views, clicks,
    completions, skips, engagements, unique_users, total_duration_seconds,
    average_completion_percentage, tickets_multiplied, cost_spent,
    conversion_rate, engagement_rate, fraud_detections, quality_score
) VALUES
-- Analytics for premium fuel ads
(1, 1, CURRENT_DATE - INTERVAL '1 day', 150, 120, 85, 75, 45, 85, 65, 2250, 87.5, 150, 212.50, 0.625, 0.708, 2, 85.2),
(1, 1, CURRENT_DATE - INTERVAL '2 days', 180, 145, 102, 88, 57, 102, 78, 2640, 89.2, 176, 255.00, 0.607, 0.703, 1, 87.8),
(2, 1, CURRENT_DATE - INTERVAL '1 day', 200, 165, 98, 92, 73, 98, 82, 0, 92.1, 276, 176.40, 0.558, 0.594, 0, 91.5),

-- Analytics for welcome ads
(3, 2, CURRENT_DATE - INTERVAL '1 day', 120, 95, 78, 72, 23, 78, 58, 1800, 94.2, 288, 234.00, 0.758, 0.821, 0, 94.8),
(4, 2, CURRENT_DATE - INTERVAL '1 day', 100, 85, 68, 65, 20, 68, 52, 2925, 96.8, 325, 306.00, 0.765, 0.800, 1, 96.2),

-- Analytics for loyalty ads
(5, 3, CURRENT_DATE - INTERVAL '1 day', 80, 68, 52, 48, 20, 52, 42, 1680, 91.5, 144, 114.40, 0.706, 0.765, 0, 89.7),

-- Analytics for summer ads
(6, 4, CURRENT_DATE - INTERVAL '1 day', 160, 128, 89, 82, 46, 89, 71, 2296, 88.3, 164, 170.10, 0.641, 0.695, 1, 86.4),

-- Analytics for corporate ads
(7, 5, CURRENT_DATE - INTERVAL '1 day', 45, 38, 28, 25, 13, 28, 22, 1000, 85.7, 25, 200.00, 0.658, 0.737, 0, 82.3)

ON CONFLICT (advertisement_id, date_recorded) DO NOTHING;

-- Update campaign budget spent and engagement counts
UPDATE ad_schema.ad_campaigns
SET budget_spent = (
    SELECT COALESCE(SUM(aa.cost_spent), 0)
    FROM ad_schema.ad_analytics aa
    JOIN ad_schema.advertisements a ON aa.advertisement_id = a.id
    WHERE a.campaign_id = ad_campaigns.id
),
actual_engagements = (
    SELECT COALESCE(SUM(aa.engagements), 0)
    FROM ad_schema.ad_analytics aa
    JOIN ad_schema.advertisements a ON aa.advertisement_id = a.id
    WHERE a.campaign_id = ad_campaigns.id
);

-- Update advertisement budget spent
UPDATE ad_schema.advertisements
SET budget_spent = (
    SELECT COALESCE(SUM(cost_spent), 0)
    FROM ad_schema.ad_analytics
    WHERE advertisement_id = advertisements.id
);