#!/bin/bash

# Script para corregir la base de datos directamente
set -e

echo "🔧 Corrigiendo base de datos PostgreSQL..."

# Detener y limpiar contenedores existentes
echo "🧹 Limpiando contenedores existentes..."
docker-compose -f docker-compose.simple.yml down -v

# Recrear con variables explícitas
echo "🚀 Recreando PostgreSQL con configuración correcta..."
POSTGRES_DB=gasolinera_db POSTGRES_USER=gasolinera_user POSTGRES_PASSWORD=gasolinera_password docker-compose -f docker-compose.simple.yml up -d postgres

# Esperar a que PostgreSQL esté listo
echo "⏳ Esperando a que PostgreSQL esté listo..."
sleep 15

# Verificar que la base de datos existe
echo "🔍 Verificando base de datos..."
docker exec gasolinera-postgres psql -U gasolinera_user -d gasolinera_db -c "SELECT 'Database exists and is accessible' as status;"

if [ $? -eq 0 ]; then
    echo "✅ Base de datos funcionando correctamente"

    # Crear esquemas básicos
    echo "📋 Creando esquemas básicos..."
    docker exec gasolinera-postgres psql -U gasolinera_user -d gasolinera_db << 'EOF'
-- Crear esquemas básicos
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS station_schema;
CREATE SCHEMA IF NOT EXISTS coupon_schema;
CREATE SCHEMA IF NOT EXISTS raffle_schema;

-- Crear tabla básica de usuarios para testing
CREATE TABLE IF NOT EXISTS auth_schema.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar usuario de prueba
INSERT INTO auth_schema.users (email, phone_number, password_hash, first_name, last_name)
SELECT 'admin@gasolinerajsm.com', '+506-8888-8888', 'hashed_password', 'Admin', 'User'
WHERE NOT EXISTS (SELECT 1 FROM auth_schema.users WHERE email = 'admin@gasolinerajsm.com');

SELECT 'Esquemas y datos básicos creados correctamente' as status;
EOF

    echo "✅ Esquemas creados correctamente"

    # Iniciar Redis y RabbitMQ
    echo "🚀 Iniciando Redis y RabbitMQ..."
    POSTGRES_DB=gasolinera_db POSTGRES_USER=gasolinera_user POSTGRES_PASSWORD=gasolinera_password docker-compose -f docker-compose.simple.yml up -d redis rabbitmq

    echo "⏳ Esperando a que todos los servicios estén listos..."
    sleep 20

    echo "🎉 ¡Base de datos y servicios corregidos exitosamente!"
    echo ""
    echo "📋 Servicios disponibles:"
    echo "   • PostgreSQL: localhost:5432 (gasolinera_db)"
    echo "   • Redis: localhost:6379"
    echo "   • RabbitMQ: localhost:5672 (Management: localhost:15672)"
    echo ""
    echo "🔗 Próximos pasos:"
    echo "   • Ejecutar: ./scripts/validate-infrastructure.sh"
    echo "   • Iniciar aplicaciones: ./start-services.sh"

else
    echo "❌ Error: No se pudo acceder a la base de datos"
    exit 1
fi