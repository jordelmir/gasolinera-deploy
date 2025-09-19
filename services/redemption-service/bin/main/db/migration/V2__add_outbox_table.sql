-- Tabla para el patrón Outbox: garantiza la entrega de eventos
CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Habilitar replicación lógica para Debezium
ALTER TABLE qrs REPLICA IDENTITY FULL;
ALTER TABLE outbox REPLICA IDENTITY FULL;
