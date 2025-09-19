-- Inicialización de base de datos para Gasolinera JSM (PostgreSQL)
-- La base de datos principal ya se crea en docker-compose
-- Solo necesitamos crear los esquemas
-- Crear esquemas para cada servicio
CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS station_schema;
CREATE SCHEMA IF NOT EXISTS coupon_schema;
CREATE SCHEMA IF NOT EXISTS raffle_schema;
CREATE SCHEMA IF NOT EXISTS redemption_schema;
CREATE SCHEMA IF NOT EXISTS ad_engine_schema;
-- El usuario ya se crea en docker-compose con las variables de entorno
-- Solo otorgamos permisos adicionales si es necesario
GRANT USAGE ON SCHEMA auth_schema TO gasolinera_user;
GRANT USAGE ON SCHEMA station_schema TO gasolinera_user;
GRANT USAGE ON SCHEMA coupon_schema TO gasolinera_user;
GRANT USAGE ON SCHEMA raffle_schema TO gasolinera_user;
GRANT USAGE ON SCHEMA redemption_schema TO gasolinera_user;
GRANT USAGE ON SCHEMA ad_engine_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA auth_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA station_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA coupon_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA raffle_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA redemption_schema TO gasolinera_user;
GRANT CREATE ON SCHEMA ad_engine_schema TO gasolinera_user;