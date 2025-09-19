-- V2__Insert_seed_data.sql
-- Seed data for Station Service development environment

-- Insert test stations across Mexico
INSERT INTO station_schema.stations (
    name, code, address, city, state, postal_code, country,
    latitude, longitude, phone_number, email, manager_name,
    status, station_type, operating_hours, services_offered, fuel_types, payment_methods,
    is_24_hours, has_convenience_store, has_car_wash, has_atm, has_restrooms,
    pump_count, capacity_vehicles_per_hour, average_service_time_minutes, created_by
) VALUES
-- Mexico City Stations
(
    'Gasolinera JSM Centro', 'JSM001',
    'Av. Juárez 123, Centro Histórico', 'Ciudad de México', 'CDMX', '06000', 'Mexico',
    19.4326, -99.1332, '+525555551001', 'centro@gasolinerajsm.com', 'Carlos Rodríguez',
    'ACTIVE', 'FULL_SERVICE', '06:00-22:00', 'Combustible,Tienda,Baños,ATM', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    false, true, false, true, true, 8, 120, 5, 'system'
),
(
    'Gasolinera JSM Polanco', 'JSM002',
    'Av. Presidente Masaryk 456, Polanco', 'Ciudad de México', 'CDMX', '11560', 'Mexico',
    19.4284, -99.1917, '+525555551002', 'polanco@gasolinerajsm.com', 'María González',
    'ACTIVE', 'PREMIUM', '24 horas', 'Combustible,Tienda,Lavado,ATM,Café', 'Magna,Premium,Diesel,Super', 'Efectivo,Tarjeta,App,Vales',
    true, true, true, true, true, 12, 200, 4, 'system'
),
(
    'Gasolinera JSM Roma Norte', 'JSM003',
    'Av. Álvaro Obregón 789, Roma Norte', 'Ciudad de México', 'CDMX', '06700', 'Mexico',
    19.4147, -99.1655, '+525555551003', 'roma@gasolinerajsm.com', 'Juan Pérez',
    'ACTIVE', 'HYBRID', '05:00-23:00', 'Combustible,Tienda,Baños', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    false, true, false, false, true, 6, 90, 6, 'system'
),

-- Guadalajara Stations
(
    'Gasolinera JSM Guadalajara Centro', 'JSM004',
    'Av. Hidalgo 321, Centro', 'Guadalajara', 'Jalisco', '44100', 'Mexico',
    20.6597, -103.3496, '+523333331001', 'gdlcentro@gasolinerajsm.com', 'Ana López',
    'ACTIVE', 'FULL_SERVICE', '06:00-22:00', 'Combustible,Tienda,Baños,ATM', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    false, true, false, true, true, 10, 150, 5, 'system'
),
(
    'Gasolinera JSM Zapopan', 'JSM005',
    'Av. López Mateos 654, Zapopan', 'Zapopan', 'Jalisco', '45050', 'Mexico',
    20.7214, -103.3918, '+523333331002', 'zapopan@gasolinerajsm.com', 'Pedro Martínez',
    'ACTIVE', 'SELF_SERVICE', '24 horas', 'Combustible,Tienda,Lavado,ATM', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    true, true, true, true, true, 8, 180, 3, 'system'
),

-- Monterrey Stations
(
    'Gasolinera JSM Monterrey Centro', 'JSM006',
    'Av. Constitución 987, Centro', 'Monterrey', 'Nuevo León', '64000', 'Mexico',
    25.6866, -100.3161, '+528111111001', 'mtycentro@gasolinerajsm.com', 'Laura Hernández',
    'ACTIVE', 'FULL_SERVICE', '06:00-22:00', 'Combustible,Tienda,Baños', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    false, true, false, false, true, 6, 100, 6, 'system'
),
(
    'Gasolinera JSM San Pedro', 'JSM007',
    'Av. Vasconcelos 147, San Pedro', 'San Pedro Garza García', 'Nuevo León', '66260', 'Mexico',
    25.6515, -100.4095, '+528111111002', 'sanpedro@gasolinerajsm.com', 'Miguel Torres',
    'ACTIVE', 'PREMIUM', '24 horas', 'Combustible,Tienda,Lavado,ATM,Café,Restaurante', 'Magna,Premium,Diesel,Super', 'Efectivo,Tarjeta,App,Vales',
    true, true, true, true, true, 14, 250, 4, 'system'
),

-- Other Cities
(
    'Gasolinera JSM Puebla', 'JSM008',
    'Blvd. 5 de Mayo 258, Centro', 'Puebla', 'Puebla', '72000', 'Mexico',
    19.0414, -98.2063, '+522221111001', 'puebla@gasolinerajsm.com', 'Sofia Ramírez',
    'ACTIVE', 'HYBRID', '06:00-22:00', 'Combustible,Tienda,Baños,ATM', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App',
    false, true, false, true, true, 8, 120, 5, 'system'
),
(
    'Gasolinera JSM Tijuana', 'JSM009',
    'Av. Revolución 369, Centro', 'Tijuana', 'Baja California', '22000', 'Mexico',
    32.5149, -117.0382, '+526641111001', 'tijuana@gasolinerajsm.com', 'Diego Morales',
    'ACTIVE', 'FULL_SERVICE', '24 horas', 'Combustible,Tienda,Lavado,ATM,Cambio de aceite', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App,USD',
    true, true, true, true, true, 10, 180, 4, 'system'
),
(
    'Gasolinera JSM Cancún', 'JSM010',
    'Av. Tulum 741, Centro', 'Cancún', 'Quintana Roo', '77500', 'Mexico',
    21.1619, -86.8515, '+529981111001', 'cancun@gasolinerajsm.com', 'Carmen Vega',
    'ACTIVE', 'HYBRID', '05:00-23:00', 'Combustible,Tienda,Baños,ATM', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta,App,USD',
    false, true, false, true, true, 6, 100, 6, 'system'
),

-- Test stations with different statuses
(
    'Gasolinera JSM Mantenimiento', 'JSM011',
    'Av. Test 123, Test', 'Test City', 'Test State', '12345', 'Mexico',
    19.0000, -99.0000, '+525555551011', 'test@gasolinerajsm.com', 'Test Manager',
    'MAINTENANCE', 'FULL_SERVICE', '06:00-22:00', 'Combustible,Tienda', 'Magna,Premium', 'Efectivo,Tarjeta',
    false, true, false, false, true, 4, 60, 8, 'system'
),
(
    'Gasolinera JSM Construcción', 'JSM012',
    'Av. Nueva 456, Nueva', 'Nueva Ciudad', 'Nuevo Estado', '54321', 'Mexico',
    20.0000, -100.0000, '+525555551012', 'nueva@gasolinerajsm.com', 'Nuevo Manager',
    'CONSTRUCTION', 'SELF_SERVICE', 'En construcción', 'En construcción', 'Magna,Premium,Diesel', 'Efectivo,Tarjeta',
    false, false, false, false, false, 8, 0, 0, 'system'
)

ON CONFLICT (code) DO NOTHING;

-- Insert employees for the stations
INSERT INTO station_schema.employees (
    user_id, station_id, employee_number, role, employment_type, shift,
    hire_date, hourly_rate, monthly_salary, is_active, can_process_transactions,
    can_handle_cash, can_supervise, training_completed, training_completion_date,
    performance_rating, last_performance_review, created_by
) VALUES
-- Station JSM001 (Centro) employees
(2, 1, 'EMP0000001', 'MANAGER', 'FULL_TIME', 'DAY', '2023-01-15', NULL, 25000.00, true, true, true, true, true, '2023-02-01', 5, '2024-01-15', 'system'),
(5, 1, 'EMP0000002', 'SUPERVISOR', 'FULL_TIME', 'EVENING', '2023-03-01', NULL, 18000.00, true, true, true, true, true, '2023-03-15', 4, '2024-03-01', 'system'),
(6, 1, 'EMP0000003', 'CASHIER', 'FULL_TIME', 'DAY', '2023-06-01', 150.00, NULL, true, true, true, false, true, '2023-06-15', 4, '2024-06-01', 'system'),
(7, 1, 'EMP0000004', 'ATTENDANT', 'PART_TIME', 'EVENING', '2023-08-01', 120.00, NULL, true, false, false, false, true, '2023-08-15', 3, '2024-08-01', 'system'),

-- Station JSM002 (Polanco) employees
(3, 2, 'EMP0000005', 'MANAGER', 'FULL_TIME', 'DAY', '2022-11-01', NULL, 30000.00, true, true, true, true, true, '2022-11-15', 5, '2023-11-01', 'system'),
(8, 2, 'EMP0000006', 'ASSISTANT_MANAGER', 'FULL_TIME', 'EVENING', '2023-02-01', NULL, 22000.00, true, true, true, true, true, '2023-02-15', 5, '2024-02-01', 'system'),
(9, 2, 'EMP0000007', 'SUPERVISOR', 'FULL_TIME', 'NIGHT', '2023-04-01', NULL, 19000.00, true, true, true, true, true, '2023-04-15', 4, '2024-04-01', 'system'),
(10, 2, 'EMP0000008', 'CASHIER', 'FULL_TIME', 'DAY', '2023-07-01', 160.00, NULL, true, true, true, false, true, '2023-07-15', 4, '2024-07-01', 'system'),

-- Station JSM003 (Roma Norte) employees
(4, 3, 'EMP0000009', 'MANAGER', 'FULL_TIME', 'DAY', '2023-05-01', NULL, 24000.00, true, true, true, true, true, '2023-05-15', 4, '2024-05-01', 'system'),
(5, 3, 'EMP0000010', 'CASHIER', 'FULL_TIME', 'EVENING', '2023-09-01', 145.00, NULL, true, true, true, false, true, '2023-09-15', 3, '2024-09-01', 'system'),

-- Add employees to other stations (simplified)
(6, 4, 'EMP0000011', 'MANAGER', 'FULL_TIME', 'DAY', '2023-01-01', NULL, 23000.00, true, true, true, true, true, '2023-01-15', 4, '2024-01-01', 'system'),
(7, 5, 'EMP0000012', 'MANAGER', 'FULL_TIME', 'DAY', '2023-02-01', NULL, 24000.00, true, true, true, true, true, '2023-02-15', 5, '2024-02-01', 'system'),
(8, 6, 'EMP0000013', 'MANAGER', 'FULL_TIME', 'DAY', '2023-03-01', NULL, 23500.00, true, true, true, true, true, '2023-03-15', 4, '2024-03-01', 'system'),
(9, 7, 'EMP0000014', 'MANAGER', 'FULL_TIME', 'DAY', '2022-12-01', NULL, 28000.00, true, true, true, true, true, '2022-12-15', 5, '2023-12-01', 'system'),
(10, 8, 'EMP0000015', 'MANAGER', 'FULL_TIME', 'DAY', '2023-04-01', NULL, 22000.00, true, true, true, true, true, '2023-04-15', 4, '2024-04-01', 'system'),

-- Add some maintenance and security staff
(5, 1, 'EMP0000016', 'MAINTENANCE', 'FULL_TIME', 'DAY', '2023-10-01', 180.00, NULL, true, false, false, false, true, '2023-10-15', 4, '2024-10-01', 'system'),
(6, 2, 'EMP0000017', 'SECURITY', 'FULL_TIME', 'NIGHT', '2023-11-01', 140.00, NULL, true, false, false, false, true, '2023-11-15', 3, '2024-11-01', 'system'),

-- Add some inactive/terminated employees for testing
(7, 1, 'EMP0000018', 'CASHIER', 'PART_TIME', 'EVENING', '2023-01-01', 130.00, NULL, false, true, false, false, true, '2023-01-15', 2, '2024-01-01', 'system'),
(8, 2, 'EMP0000019', 'ATTENDANT', 'TEMPORARY', 'DAY', '2023-06-01', 110.00, NULL, false, false, false, false, false, NULL, NULL, NULL, 'system')

ON CONFLICT (employee_number) DO NOTHING;

-- Update some employees with termination dates
UPDATE station_schema.employees
SET termination_date = '2024-06-30', is_active = false
WHERE employee_number = 'EMP0000018';

UPDATE station_schema.employees
SET termination_date = '2023-12-31', is_active = false
WHERE employee_number = 'EMP0000019';

-- Insert some audit log entries
INSERT INTO station_schema.station_audit_log (
    station_id, employee_id, event_type, event_details, performed_by, created_at
) VALUES
(1, NULL, 'STATION_CREATED', '{"initial_setup": true}', 'system', CURRENT_TIMESTAMP - INTERVAL '6 months'),
(1, 1, 'EMPLOYEE_HIRED', '{"role": "MANAGER", "salary": 25000}', 'system', CURRENT_TIMESTAMP - INTERVAL '6 months'),
(2, NULL, 'STATION_CREATED', '{"initial_setup": true}', 'system', CURRENT_TIMESTAMP - INTERVAL '8 months'),
(2, 5, 'EMPLOYEE_HIRED', '{"role": "MANAGER", "salary": 30000}', 'system', CURRENT_TIMESTAMP - INTERVAL '8 months'),
(1, 2, 'EMPLOYEE_HIRED', '{"role": "SUPERVISOR", "salary": 18000}', 'system', CURRENT_TIMESTAMP - INTERVAL '4 months'),
(1, 18, 'EMPLOYEE_TERMINATED', '{"reason": "performance", "termination_date": "2024-06-30"}', 'system', CURRENT_TIMESTAMP - INTERVAL '1 month'),
(11, NULL, 'STATION_MAINTENANCE', '{"reason": "equipment_upgrade", "expected_duration": "2 weeks"}', 'system', CURRENT_TIMESTAMP - INTERVAL '1 week');