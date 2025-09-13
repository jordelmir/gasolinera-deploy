
-- V1__Create_station_schema.sql
-- Create the station schema and initial tables for the station service

-- Create the station schema
CREATE SCHEMA IF NOT EXISTS station_schema;

-- Set the search path to include the station schema
SET search_path TO station_schema, public;

-- Create the stations table
CREATE TABLE station_schema.stations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'Mexico',
    latitude DECIMAL(10,8) NOT NULL,
    longitude DECIMAL(11,8) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(100),
    manager_name VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    station_type VARCHAR(30) NOT NULL DEFAULT 'FULL_SERVICE',
    operating_hours VARCHAR(100),
    services_offered VARCHAR(1000),
    fuel_types VARCHAR(500),
    payment_methods VARCHAR(500),
    is_24_hours BOOLEAN NOT NULL DEFAULT false,
    has_convenience_store BOOLEAN NOT NULL DEFAULT false,
    has_car_wash BOOLEAN NOT NULL DEFAULT false,
    has_atm BOOLEAN NOT NULL DEFAULT false,
    has_restrooms BOOLEAN NOT NULL DEFAULT true,
    pump_count INTEGER NOT NULL DEFAULT 1,
    capacity_vehicles_per_hour INTEGER,
    average_service_time_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_stations_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE', 'CONSTRUCTION', 'PERMANENTLY_CLOSED')),
    CONSTRAINT chk_stations_type CHECK (station_type IN ('FULL_SERVICE', 'SELF_SERVICE', 'HYBRID', 'TRUCK_STOP', 'CONVENIENCE', 'PREMIUM')),
    CONSTRAINT chk_stations_code_format CHECK (code ~ '^[A-Z0-9]{3,20}$'),
    CONSTRAINT chk_stations_postal_code_format CHECK (postal_code ~ '^[0-9]{5}(-[0-9]{4})?$'),
    CONSTRAINT chk_stations_phone_format CHECK (phone_number IS NULL OR phone_number ~ '^\+?[1-9]\d{1,14}$'),
    CONSTRAINT chk_stations_latitude CHECK (latitude >= -90.0 AND latitude <= 90.0),
    CONSTRAINT chk_stations_longitude CHECK (longitude >= -180.0 AND longitude <= 180.0),
    CONSTRAINT chk_stations_pump_count CHECK (pump_count >= 1 AND pump_count <= 50),
    CONSTRAINT chk_stations_capacity CHECK (capacity_vehicles_per_hour IS NULL OR capacity_vehicles_per_hour >= 0),
    CONSTRAINT chk_stations_service_time CHECK (average_service_time_minutes IS NULL OR average_service_time_minutes >= 1)
);

-- Create indexes for stations
CREATE INDEX idx_stations_status ON station_schema.stations(status);
CREATE INDEX idx_stations_location ON station_schema.stations(latitude, longitude);
CREATE INDEX idx_stations_name ON station_schema.stations(name);
CREATE INDEX idx_stations_created_at ON station_schema.stations(created_at);
CREATE INDEX idx_stations_code ON station_schema.stations(code);
CREATE INDEX idx_stations_city ON station_schema.stations(city);
CREATE INDEX idx_stations_state ON station_schema.stations(state);
CREATE INDEX idx_stations_type ON station_schema.stations(station_type);

-- Create the employees table
CREATE TABLE station_schema.employees (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    station_id BIGINT NOT NULL REFERENCES station_schema.stations(id) ON DELETE CASCADE,
    employee_number VARCHAR(20) NOT NULL UNIQUE,
    role VARCHAR(30) NOT NULL,
    employment_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    shift VARCHAR(20) NOT NULL DEFAULT 'DAY',
    hire_date DATE NOT NULL,
    termination_date DATE,
    hourly_rate DECIMAL(8,2),
    monthly_salary DECIMAL(10,2),
    is_active BOOLEAN NOT NULL DEFAULT true,
    can_process_transactions BOOLEAN NOT NULL DEFAULT true,
    can_handle_cash BOOLEAN NOT NULL DEFAULT false,
    can_supervise BOOLEAN NOT NULL DEFAULT false,
    emergency_contact_name VARCHAR(200),
    emergency_contact_phone VARCHAR(20),
    notes VARCHAR(1000),
    training_completed BOOLEAN NOT NULL DEFAULT false,
    training_completion_date DATE,
    certification_expiry_date DATE,
    performance_rating INTEGER,
    last_performance_review DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    -- Constraints
    CONSTRAINT chk_employees_role CHECK (role IN ('CASHIER', 'ATTENDANT', 'SUPERVISOR', 'ASSISTANT_MANAGER', 'MANAGER', 'MAINTENANCE', 'SECURITY')),
    CONSTRAINT chk_employees_employment_type CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'TEMPORARY', 'INTERN')),
    CONSTRAINT chk_employees_shift CHECK (shift IN ('DAY', 'EVENING', 'NIGHT', 'ROTATING', 'FLEXIBLE')),
    CONSTRAINT chk_employees_number_format CHECK (employee_number ~ '^EMP[0-9]{6,15}$'),
    CONSTRAINT chk_employees_emergency_phone_format CHECK (emergency_contact_phone IS NULL OR emergency_contact_phone ~ '^\+?[1-9]\d{1,14}$'),
    CONSTRAINT chk_employees_hire_date CHECK (hire_date <= CURRENT_DATE),
    CONSTRAINT chk_employees_termination_date CHECK (termination_date IS NULL OR termination_date >= hire_date),
    CONSTRAINT chk_employees_hourly_rate CHECK (hourly_rate IS NULL OR (hourly_rate >= 0.0 AND hourly_rate <= 999999.99)),
    CONSTRAINT chk_employees_monthly_salary CHECK (monthly_salary IS NULL OR (monthly_salary >= 0.0 AND monthly_salary <= 99999999.99)),
    CONSTRAINT chk_employees_performance_rating CHECK (performance_rating IS NULL OR (performance_rating >= 1 AND performance_rating <= 5)),
    CONSTRAINT uk_employees_user_station UNIQUE (user_id, station_id)
);

-- Create indexes for employees
CREATE INDEX idx_employees_station_id ON station_schema.employees(station_id);
CREATE INDEX idx_employees_user_id ON station_schema.employees(user_id);
CREATE INDEX idx_employees_role ON station_schema.employees(role);
CREATE INDEX idx_employees_active ON station_schema.employees(is_active);
CREATE INDEX idx_employees_employee_number ON station_schema.employees(employee_number);
CREATE INDEX idx_employees_hire_date ON station_schema.employees(hire_date);
CREATE INDEX idx_employees_employment_type ON station_schema.employees(employment_type);
CREATE INDEX idx_employees_shift ON station_schema.employees(shift);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION station_schema.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers to automatically update updated_at
CREATE TRIGGER update_stations_updated_at
    BEFORE UPDATE ON station_schema.stations
    FOR EACH ROW
    EXECUTE FUNCTION station_schema.update_updated_at_column();

CREATE TRIGGER update_employees_updated_at
    BEFORE UPDATE ON station_schema.employees
    FOR EACH ROW
    EXECUTE FUNCTION station_schema.update_updated_at_column();

-- Create station audit log table
CREATE TABLE station_schema.station_audit_log (
    id BIGSERIAL PRIMARY KEY,
    station_id BIGINT REFERENCES station_schema.stations(id) ON DELETE SET NULL,
    employee_id BIGINT REFERENCES station_schema.employees(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    event_details JSONB,
    old_values JSONB,
    new_values JSONB,
    performed_by VARCHAR(100),
    ip_address INET,
    user_agent TEXT,
    session_id VARCHAR(255),
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_station_audit_event_type CHECK (
        event_type IN (
            'STATION_CREATED', 'STATION_UPDATED', 'STATION_DELETED',
            'STATION_ACTIVATED', 'STATION_DEACTIVATED', 'STATION_MAINTENANCE',
            'EMPLOYEE_HIRED', 'EMPLOYEE_UPDATED', 'EMPLOYEE_TERMINATED',
            'EMPLOYEE_ACTIVATED', 'EMPLOYEE_DEACTIVATED', 'EMPLOYEE_PROMOTED',
            'TRAINING_COMPLETED', 'PERFORMANCE_REVIEW', 'CERTIFICATION_UPDATED'
        )
    )
);

-- Create indexes for audit log
CREATE INDEX idx_station_audit_station_id ON station_schema.station_audit_log(station_id);
CREATE INDEX idx_station_audit_employee_id ON station_schema.station_audit_log(employee_id);
CREATE INDEX idx_station_audit_event_type ON station_schema.station_audit_log(event_type);
CREATE INDEX idx_station_audit_created_at ON station_schema.station_audit_log(created_at);
CREATE INDEX idx_station_audit_performed_by ON station_schema.station_audit_log(performed_by);
CREATE INDEX idx_station_audit_session_id ON station_schema.station_audit_log(session_id);
CREATE INDEX idx_station_audit_correlation_id ON station_schema.station_audit_log(correlation_id);

-- Create views for common queries
CREATE VIEW station_schema.active_stations AS
SELECT
    id,
    name,
    code,
    address,
    city,
    state,
    postal_code,
    latitude,
    longitude,
    phone_number,
    email,
    manager_name,
    station_type,
    operating_hours,
    is_24_hours,
    pump_count,
    created_at,
    updated_at
FROM station_schema.stations
WHERE status = 'ACTIVE';

CREATE VIEW station_schema.active_employees AS
SELECT
    e.id,
    e.user_id,
    e.station_id,
    e.employee_number,
    e.role,
    e.employment_type,
    e.shift,
    e.hire_date,
    e.can_process_transactions,
    e.can_handle_cash,
    e.can_supervise,
    e.training_completed,
    e.performance_rating,
    s.name as station_name,
    s.code as station_code
FROM station_schema.employees e
JOIN station_schema.stations s ON e.station_id = s.id
WHERE e.is_active = true AND e.termination_date IS NULL;

CREATE VIEW station_schema.station_statistics AS
SELECT
    s.id,
    s.name,
    s.code,
    s.city,
    s.state,
    s.status,
    s.pump_count,
    COUNT(e.id) as total_employees,
    COUNT(CASE WHEN e.is_active AND e.termination_date IS NULL THEN 1 END) as active_employees,
    COUNT(CASE WHEN e.role IN ('MANAGER', 'ASSISTANT_MANAGER') THEN 1 END) as management_count,
    COUNT(CASE WHEN e.training_completed THEN 1 END) as trained_employees
FROM station_schema.stations s
LEFT JOIN station_schema.employees e ON s.id = e.station_id
GROUP BY s.id, s.name, s.code, s.city, s.state, s.status, s.pump_count;

-- Insert sample data for development
INSERT INTO station_schema.stations (
    name, code, address, city, state, postal_code, country,
    latitude, longitude, phone_number, email, manager_name,
    status, station_type, is_24_hours, pump_count
) VALUES
(
    'Gasolinera JSM Central', 'JSM001',
    'Av. Principal 123', 'Ciudad de México', 'CDMX', '01000', 'Mexico',
    19.4326, -99.1332, '+525555551234', 'central@gasolinerajsm.com', 'Juan Pérez',
    'ACTIVE', 'FULL_SERVICE', true, 8
),
(
    'Gasolinera JSM Norte', 'JSM002',
    'Blvd. Norte 456', 'Guadalajara', 'Jalisco', '44100', 'Mexico',
    20.6597, -103.3496, '+523333334567', 'norte@gasolinerajsm.com', 'María González',
    'ACTIVE', 'HYBRID', false, 6
),
(
    'Gasolinera JSM Sur', 'JSM003',
    'Carretera Sur 789', 'Monterrey', 'Nuevo León', '64000', 'Mexico',
    25.6866, -100.3161, '+528111118901', 'sur@gasolinerajsm.com', 'Carlos Rodríguez',
    'ACTIVE', 'SELF_SERVICE', true, 10
) ON CONFLICT (code) DO NOTHING;

-- Grant necessary permissions (adjust based on your database user setup)
-- GRANT USAGE ON SCHEMA station_schema TO station_service_user;
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA station_schema TO station_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA station_schema TO station_service_user;