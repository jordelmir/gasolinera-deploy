# HashiCorp Vault Integration for Gasolinera JSM

Este directorio contiene la configuración completa de HashiCorp Vault para el manejo seguro de secretos en el ecosistema de microservicios Gasolinera JSM.

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Configuración](#configuración)
- [Instalación](#instalación)
- [Uso](#uso)
- [Rotación de Secretos](#rotación-de-secretos)
- [Monitoreo](#monitoreo)
- [Troubleshooting](#troubleshooting)

## 🏗️ Arquitectura

### Componentes de Vault

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Microservice  │    │   Vault Agent   │    │  Vault Server   │
│                 │◄──►│                 │◄──►│                 │
│  - Auth Service │    │  - Auto Auth    │    │  - KV Store     │
│  - Station Svc  │    │  - Templates    │    │  - DB Engine    │
│  - Coupon Svc   │    │  - Caching      │    │  - Transit      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                       │
                                               ┌─────────────────┐
                                               │   PostgreSQL    │
                                               │   (Storage)     │
                                               └─────────────────┘
```

### Secret Engines Habilitados

1. **KV v2** - Almacenamiento de secretos de aplicación
2. **Database** - Credenciales dinámicas de base de datos
3. **Transit** - Encriptación como servicio
4. **PKI** - Gestión de certificados

### Políticas de Acceso

- **gasolinera-app** - Acceso de lectura para microservicios
- **gasolinera-admin** - Acceso completo para administradores

## ⚙️ Configuración

### Variables de Entorno

```bash
# Vault Server
export VAULT_ADDR="http://localhost:8200"
export VAULT_TOKEN="your-vault-token"

# AppRole Authentication
export VAULT_ROLE_ID="your-role-id"
export VAULT_SECRET_ID="your-secret-id"

# Database
export VAULT_DB_HOST="localhost"
export VAULT_DB_PORT="5432"
export VAULT_DB_NAME="vault_db"
export VAULT_DB_USER="vault_user"
export VAULT_DB_PASSWORD="vault_password"
```

### Estructura de Secretos

```
gasolinera-jsm/
├── auth-service/
│   ├── jwt-secret
│   ├── jwt-issuer
│   ├── jwt-access-expiration
│   └── jwt-refresh-expiration
├── shared/
│   ├── database/
│   │   ├── host
│   │   ├── port
│   │   ├── database
│   │   ├── username
│   │   └── password
│   ├── redis/
│   │   ├── host
│   │   ├── port
│   │   └── password
│   ├── rabbitmq/
│   │   ├── host
│   │   ├── port
│   │   ├── username
│   │   ├── password
│   │   └── virtual-host
│   └── external-apis/
│       ├── google-maps-api-key
│       ├── sendgrid-api-key
│       ├── twilio-api-key
│       └── twilio-auth-token
├── coupon-service/
│   ├── qr-encryption-key
│   └── coupon-salt
├── raffle-service/
│   ├── random-seed
│   └── prize-encryption-key
└── ad-engine/
    ├── analytics-api-key
    └── targeting-secret
```

## 🚀 Instalación

### 1. Iniciar Vault con Docker Compose

```bash
# Crear red de Docker
docker network create gasolinera-network

# Iniciar servicios de Vault
cd infrastructure/vault
docker-compose -f docker-compose.vault.yml up -d

# Verificar que Vault esté ejecutándose
docker logs gasolinera-vault
```

### 2. Inicializar Vault

```bash
# Ejecutar script de inicialización
./init-vault.sh

# El script creará:
# - /tmp/vault-root-token (token de root)
# - /tmp/vault-unseal-keys (claves de desbloqueo)
# - /tmp/vault-role-id (ID de rol para AppRole)
# - /tmp/vault-secret-id (ID secreto para AppRole)
```

### 3. Configurar Aplicaciones

```yaml
# application.yml
vault:
  address: http://localhost:8200
  role-id: ${VAULT_ROLE_ID}
  secret-id: ${VAULT_SECRET_ID}
  namespace: gasolinera-jsm
  database:
    enabled: true
  redis:
    enabled: true
  rabbitmq:
    enabled: true
```

## 💻 Uso

### Integración en Spring Boot

```kotlin
// Inyectar VaultSecretManager
@Autowired
private lateinit var vaultSecretManager: VaultSecretManager

// Obtener configuración JWT
val jwtConfig = vaultSecretManager.getJwtConfig()

// Obtener credenciales de base de datos dinámicas
val dbCredentials = vaultClient.getDatabaseCredentials("gasolinera-readwrite")

// Encriptar datos sensibles
val encryptedData = vaultSecretManager.encryptSensitiveData("sensitive-info")

// Desencriptar datos
val decryptedData = vaultSecretManager.decryptSensitiveData(encryptedData)
```

### Comandos CLI Útiles

```bash
# Ver estado de Vault
vault status

# Listar secretos
vault kv list gasolinera-jsm/

# Obtener secreto específico
vault kv get gasolinera-jsm/auth-service

# Actualizar secreto
vault kv put gasolinera-jsm/auth-service jwt-secret="new-secret"

# Obtener credenciales de base de datos
vault read database/creds/gasolinera-readwrite

# Encriptar datos
vault write transit/encrypt/gasolinera-key plaintext=$(base64 <<< "my-secret")

# Desencriptar datos
vault write transit/decrypt/gasolinera-key ciphertext="vault:v1:..."
```

## 🔄 Rotación de Secretos

### Rotación Automática

```bash
# Rotar todos los secretos
./rotate-secrets.sh all

# Rotar solo secretos JWT
./rotate-secrets.sh jwt

# Rotar solo contraseñas de base de datos
./rotate-secrets.sh database

# Crear backup antes de rotación
./rotate-secrets.sh backup
```

### Programar Rotación

```bash
# Agregar a crontab para rotación semanal
0 2 * * 0 /path/to/rotate-secrets.sh all >> /var/log/vault-rotation.log 2>&1
```

### Rotación Manual de Claves Transit

```bash
# Rotar clave de encriptación principal
vault write -f transit/keys/gasolinera-key/rotate

# Rotar clave de PII
vault write -f transit/keys/gasolinera-pii-key/rotate
```

## 📊 Monitoreo

### Health Checks

```bash
# Verificar estado de Vault
curl -s http://localhost:8200/v1/sys/health | jq

# Verificar métricas de Prometheus
curl -s http://localhost:8200/v1/sys/metrics?format=prometheus
```

### Logs Importantes

```bash
# Logs de Vault Server
docker logs gasolinera-vault

# Logs de Vault Agent
docker logs gasolinera-vault-agent

# Logs de rotación de secretos
tail -f /var/log/vault-rotation.log
```

### Alertas Recomendadas

1. **Vault Sealed** - Vault se ha sellado inesperadamente
2. **Token Expiration** - Tokens próximos a expirar
3. **Failed Authentication** - Intentos de autenticación fallidos
4. **Secret Access** - Acceso a secretos sensibles
5. **Rotation Failures** - Fallos en rotación de secretos

## 🔧 Troubleshooting

### Problemas Comunes

#### Vault Sellado

```bash
# Verificar estado
vault status

# Desbloquear con claves
vault operator unseal <key1>
vault operator unseal <key2>
vault operator unseal <key3>
```

#### Token Expirado

```bash
# Renovar token
vault token renew

# O re-autenticar con AppRole
vault write auth/approle/login role_id="$ROLE_ID" secret_id="$SECRET_ID"
```

#### Credenciales de Base de Datos Inválidas

```bash
# Verificar configuración de base de datos
vault read database/config/postgresql

# Generar nuevas credenciales
vault read database/creds/gasolinera-readwrite

# Verificar roles
vault list database/roles
```

#### Problemas de Conectividad

```bash
# Verificar conectividad de red
curl -s http://localhost:8200/v1/sys/health

# Verificar logs de Docker
docker logs gasolinera-vault

# Verificar configuración de red
docker network inspect gasolinera-network
```

### Recuperación de Desastres

#### Backup de Vault

```bash
# Backup de datos (solo para desarrollo)
docker exec gasolinera-vault vault operator raft snapshot save /vault/data/backup.snap

# Backup de configuración
cp -r infrastructure/vault /backup/vault-config-$(date +%Y%m%d)
```

#### Restauración

```bash
# Restaurar desde snapshot
docker exec gasolinera-vault vault operator raft snapshot restore /vault/data/backup.snap

# Re-inicializar si es necesario
./init-vault.sh
```

## 🔒 Seguridad

### Mejores Prácticas

1. **Nunca usar tokens de root en producción**
2. **Rotar secretos regularmente**
3. **Usar AppRole para autenticación de servicios**
4. **Habilitar auditoría en producción**
5. **Usar auto-unseal con KMS en la nube**
6. **Implementar políticas de acceso granulares**
7. **Monitorear accesos y cambios**

### Configuración de Producción

```hcl
# vault-prod.hcl
storage "postgresql" {
  connection_url = "postgres://vault:password@db:5432/vault?sslmode=require"
}

listener "tcp" {
  address     = "0.0.0.0:8200"
  tls_cert_file = "/vault/tls/vault.crt"
  tls_key_file  = "/vault/tls/vault.key"
}

seal "awskms" {
  region     = "us-west-2"
  kms_key_id = "alias/vault-unseal-key"
}

ui = true
disable_mlock = false
```

## 📚 Referencias

- [HashiCorp Vault Documentation](https://www.vaultproject.io/docs)
- [Vault API Reference](https://www.vaultproject.io/api-docs)
- [Spring Cloud Vault](https://spring.io/projects/spring-cloud-vault)
- [Vault Agent](https://www.vaultproject.io/docs/agent)
- [Database Secrets Engine](https://www.vaultproject.io/docs/secrets/databases)
- [Transit Secrets Engine](https://www.vaultproject.io/docs/secrets/transit)
