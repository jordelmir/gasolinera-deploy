-- Migración V1: Esquema de autenticación
-- Autor: Sistema Gasolinera JSM
-- Fecha: 2024-01-01
-- Compatibilidad: PostgreSQL y MySQL
-- Crear esquema de autenticación
CREATE SCHEMA IF NOT EXISTS auth_schema;
-- Tabla de usuarios
CREATE TABLE auth_schema.users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone_number VARCHAR(20) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  is_active BOOLEAN DEFAULT true,
  is_verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  last_login TIMESTAMP WITH TIME ZONE,
  failed_login_attempts INTEGER DEFAULT 0,
  locked_until TIMESTAMP WITH TIME ZONE
);
-- Tabla de roles
CREATE TABLE auth_schema.roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- Tabla de permisos
CREATE TABLE auth_schema.permissions (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  resource VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL,
  description TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- Tabla de relación usuario-rol
CREATE TABLE auth_schema.user_roles (
  user_id BIGINT REFERENCES auth_schema.users(id) ON DELETE CASCADE,
  role_id BIGINT REFERENCES auth_schema.roles(id) ON DELETE CASCADE,
  assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  assigned_by BIGINT REFERENCES auth_schema.users(id),
  PRIMARY KEY (user_id, role_id)
);
-- Tabla de relación rol-permiso
CREATE TABLE auth_schema.role_permissions (
  role_id BIGINT REFERENCES auth_schema.roles(id) ON DELETE CASCADE,
  permission_id BIGINT REFERENCES auth_schema.permissions(id) ON DELETE CASCADE,
  granted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id)
);
-- Tabla de tokens de refresh
CREATE TABLE auth_schema.refresh_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES auth_schema.users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) UNIQUE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP WITH TIME ZONE,
  device_info JSONB
);
-- Tabla de códigos OTP
CREATE TABLE auth_schema.otp_codes (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES auth_schema.users(id) ON DELETE CASCADE,
  code VARCHAR(10) NOT NULL,
  purpose VARCHAR(50) NOT NULL,
  -- 'REGISTRATION', 'LOGIN', 'PASSWORD_RESET'
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  used_at TIMESTAMP WITH TIME ZONE,
  attempts INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
-- Índices para optimización
CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_users_phone ON auth_schema.users(phone_number);
CREATE INDEX idx_users_active ON auth_schema.users(is_active);
CREATE INDEX idx_refresh_tokens_user ON auth_schema.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires ON auth_schema.refresh_tokens(expires_at);
CREATE INDEX idx_otp_codes_user ON auth_schema.otp_codes(user_id);
CREATE INDEX idx_otp_codes_expires ON auth_schema.otp_codes(expires_at);
-- Insertar roles por defecto
INSERT INTO auth_schema.roles (name, description)
VALUES (
    'ADMIN',
    'Administrador del sistema con acceso completo'
  ),
  (
    'STATION_MANAGER',
    'Gerente de estación con acceso a gestión local'
  ),
  (
    'CUSTOMER',
    'Cliente con acceso a funciones básicas'
  ),
  (
    'OPERATOR',
    'Operador con acceso limitado a operaciones'
  );
-- Insertar permisos por defecto
INSERT INTO auth_schema.permissions (name, resource, action, description)
VALUES (
    'USER_READ',
    'users',
    'read',
    'Leer información de usuarios'
  ),
  (
    'USER_WRITE',
    'users',
    'write',
    'Crear y modificar usuarios'
  ),
  (
    'USER_DELETE',
    'users',
    'delete',
    'Eliminar usuarios'
  ),
  (
    'STATION_READ',
    'stations',
    'read',
    'Leer información de estaciones'
  ),
  (
    'STATION_WRITE',
    'stations',
    'write',
    'Crear y modificar estaciones'
  ),
  ('COUPON_READ', 'coupons', 'read', 'Leer cupones'),
  (
    'COUPON_WRITE',
    'coupons',
    'write',
    'Crear y modificar cupones'
  ),
  (
    'COUPON_REDEEM',
    'coupons',
    'redeem',
    'Redimir cupones'
  ),
  ('RAFFLE_READ', 'raffles', 'read', 'Leer sorteos'),
  (
    'RAFFLE_WRITE',
    'raffles',
    'write',
    'Crear y modificar sorteos'
  ),
  (
    'RAFFLE_PARTICIPATE',
    'raffles',
    'participate',
    'Participar en sorteos'
  );
-- Asignar permisos a roles
-- ADMIN: todos los permisos
INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM auth_schema.roles r,
  auth_schema.permissions p
WHERE r.name = 'ADMIN';
-- STATION_MANAGER: permisos de estación y operaciones
INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM auth_schema.roles r,
  auth_schema.permissions p
WHERE r.name = 'STATION_MANAGER'
  AND p.name IN (
    'STATION_READ',
    'STATION_WRITE',
    'COUPON_READ',
    'COUPON_REDEEM',
    'RAFFLE_READ'
  );
-- CUSTOMER: permisos básicos
INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM auth_schema.roles r,
  auth_schema.permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN (
    'COUPON_READ',
    'COUPON_REDEEM',
    'RAFFLE_READ',
    'RAFFLE_PARTICIPATE'
  );
-- OPERATOR: permisos de lectura
INSERT INTO auth_schema.role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM auth_schema.roles r,
  auth_schema.permissions p
WHERE r.name = 'OPERATOR'
  AND p.name IN ('STATION_READ', 'COUPON_READ', 'RAFFLE_READ');