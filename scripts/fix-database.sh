#!/bin/bash

# Script para corregir problemas de base de datos
# Uso: ./fix-database.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔧 Corrigiendo configuración de base de datos${NC}"

# Verificar que PostgreSQL esté ejecutándose
if ! docker-compose -f docker-compose.simple.yml ps postgres | grep -q "Up"; then
    echo -e "${YELLOW}⚠️  PostgreSQL no está ejecutándose. Iniciando...${NC}"
    docker-compose -f docker-compose.simple.yml up -d postgres
    sleep 10
fi

# Esperar a que PostgreSQL esté listo
echo -e "${BLUE}ℹ️  Esperando a que PostgreSQL esté listo...${NC}"
timeout=60
while ! docker-compose -f docker-compose.simple.yml exec -T postgres pg_isready -U gasolinera_user; do
    sleep 2
    timeout=$((timeout - 2))
    if [ $timeout -le 0 ]; then
        echo -e "${RED}❌ Timeout esperando PostgreSQL${NC}"
        exit 1
    fi
done

echo -e "${GREEN}✅ PostgreSQL está listo${NC}"

# Ejecutar script de inicialización corregido
echo -e "${BLUE}ℹ️  Ejecutando script de inicialización...${NC}"
docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db -f /docker-entrypoint-initdb.d/init-db.sql

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Script de inicialización ejecutado correctamente${NC}"
else
    echo -e "${YELLOW}⚠️  El script ya se ejecutó anteriormente o hubo un problema menor${NC}"
fi

# Ejecutar migraciones principales
echo -e "${BLUE}ℹ️  Ejecutando migraciones principales...${NC}"

# Migración V1 - Auth Schema
echo -e "${BLUE}ℹ️  Ejecutando migración V1 (Auth Schema)...${NC}"
docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db << 'EOF'
-- Migración V1: Esquema de autenticación
-- Crear esquema de autenticación si no existe
CREATE SCHEMA IF NOT EXISTS auth_schema;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS auth_schema.users (
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
CREATE TABLE IF NOT EXISTS auth_schema.roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar roles por defecto si no existen
INSERT INTO auth_schema.roles (name, description)
SELECT 'ADMIN', 'Administrador del sistema con acceso completo'
WHERE NOT EXISTS (SELECT 1 FROM auth_schema.roles WHERE name = 'ADMIN');

INSERT INTO auth_schema.roles (name, description)
SELECT 'STATION_MANAGER', 'Gerente de estación con acceso a gestión local'
WHERE NOT EXISTS (SELECT 1 FROM auth_schema.roles WHERE name = 'STATION_MANAGER');

INSERT INTO auth_schema.roles (name, description)
SELECT 'CUSTOMER', 'Cliente con acceso a funciones básicas'
WHERE NOT EXISTS (SELECT 1 FROM auth_schema.roles WHERE name = 'CUSTOMER');

INSERT INTO auth_schema.roles (name, description)
SELECT 'OPERATOR', 'Operador con acceso limitado a operaciones'
WHERE NOT EXISTS (SELECT 1 FROM auth_schema.roles WHERE name = 'OPERATOR');

EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Migración V1 ejecutada correctamente${NC}"
else
    echo -e "${YELLOW}⚠️  La migración V1 ya se ejecutó anteriormente${NC}"
fi

# Verificar que las tablas se crearon correctamente
echo -e "${BLUE}ℹ️  Verificando tablas creadas...${NC}"
docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db -c "\dt auth_schema.*"

# Verificar conectividad final
echo -e "${BLUE}ℹ️  Verificando conectividad final...${NC}"
docker-compose -f docker-compose.simple.yml exec -T postgres psql -U gasolinera_user -d gasolinera_db -c "SELECT 'Database connection successful' as status;"

echo -e "${GREEN}🎉 Base de datos corregida y lista para usar${NC}"
echo ""
echo -e "${BLUE}📋 Próximos pasos:${NC}"
echo -e "   • Ejecutar: ./scripts/validate-infrastructure.sh"
echo -e "   • Iniciar servicios: ./start-services.sh"
echo -e "   • Verificar health: curl http://localhost:8080/health"
echo ""