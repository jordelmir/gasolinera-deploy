-- Migración V1: Esquema de autenticación - MySQL
-- Autor: Sistema Gasolinera JSM
-- Fecha: 2024-01-01
-- Compatibilidad: MySQL 8.0+
-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS gasolinera_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gasolinera_db;
-- Tabla de usuarios
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  phone_number VARCHAR(20) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  is_active BOOLEAN DEFAULT true,
  is_verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  last_login TIMESTAMP NULL,
  failed_login_attempts INT DEFAULT 0,
  locked_until TIMESTAMP NULL,
  INDEX idx_users_email (email),
  INDEX idx_users_phone (phone_number),
  INDEX idx_users_active (is_active)
) ENGINE = InnoDB;
-- Tabla de roles
CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) UNIQUE NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;
-- Tabla de permisos
CREATE TABLE permissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  resource VARCHAR(100) NOT NULL,
  action VARCHAR(50) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB;
-- Tabla de relación usuario-rol
CREATE TABLE user_roles (
  user_id BIGINT,
  role_id BIGINT,
  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  assigned_by BIGINT,
  PRIMARY KEY (user_id, role_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE = InnoDB;
-- Tabla de relación rol-permiso
CREATE TABLE role_permissions (
  role_id BIGINT,
  permission_id BIGINT,
  granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (role_id, permission_id),
  FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
  FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE = InnoDB;
-- Tabla de tokens de refresh
CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  token_hash VARCHAR(255) UNIQUE NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  revoked_at TIMESTAMP NULL,
  device_info JSON,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_refresh_tokens_user (user_id),
  INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE = InnoDB;
-- Tabla de códigos OTP
CREATE TABLE otp_codes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  code VARCHAR(10) NOT NULL,
  purpose VARCHAR(50) NOT NULL COMMENT 'REGISTRATION, LOGIN, PASSWORD_RESET',
  expires_at TIMESTAMP NOT NULL,
  used_at TIMESTAMP NULL,
  attempts INT DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  INDEX idx_otp_codes_user (user_id),
  INDEX idx_otp_codes_expires (expires_at)
) ENGINE = InnoDB;
-- Insertar roles por defecto
INSERT INTO roles (name, description)
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
INSERT INTO permissions (name, resource, action, description)
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
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM roles r,
  permissions p
WHERE r.name = 'ADMIN';
-- STATION_MANAGER: permisos de estación y operaciones
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM roles r,
  permissions p
WHERE r.name = 'STATION_MANAGER'
  AND p.name IN (
    'STATION_READ',
    'STATION_WRITE',
    'COUPON_READ',
    'COUPON_REDEEM',
    'RAFFLE_READ'
  );
-- CUSTOMER: permisos básicos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM roles r,
  permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN (
    'COUPON_READ',
    'COUPON_REDEEM',
    'RAFFLE_READ',
    'RAFFLE_PARTICIPATE'
  );
-- OPERATOR: permisos de lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id,
  p.id
FROM roles r,
  permissions p
WHERE r.name = 'OPERATOR'
  AND p.name IN ('STATION_READ', 'COUPON_READ', 'RAFFLE_READ');