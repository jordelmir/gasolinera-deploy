# Auditoría del Sistema - Gasolinera JSM Ultimate

*Este documento es generado automáticamente por Kiro-OPS como parte del Paso 1 del plan de despliegue.*

**Fecha de Auditoría:** 2025-08-25

## 1. Stack Tecnológico

El sistema es un monorepo gestionado con **Nx**.

- **Backend:** Kotlin con Spring Boot 3 y Java 17. Gestionado con Gradle.
- **Frontend:** Next.js, TypeScript, Tailwind CSS.
- **Mobile:** Expo (React Native).
- **Infraestructura:** Docker, Docker Compose.
- **Plataformas de Despliegue (Target):**
  - **Frontend:** Vercel (`vercel.json`).
  - **Backend:** Render.com (`render.yaml`).
- **CI/CD:** GitHub Actions (definido en la misión, workflows por crear).

## 2. Mapa de Servicios y Puertos

Los servicios se orquestan localmente vía `docker-compose.yml`.

| Servicio             | Puerto Local | Puerto Contenedor | Notas                               |
| -------------------- | ------------ | ----------------- | ----------------------------------- |
| `postgres`           | `5432`       | `5432`            | Base de datos principal.            |
| `redis`              | `6379`       | `6379`            | Caché y locks distribuidos.         |
| `rabbitmq`           | `5672`, `15672` | `5672`, `15672`  | Cola de mensajería (UI en 15672).   |
| `jaeger`             | `16686`      | `16686`           | Tracing distribuido (OpenTelemetry).|
| `vault`              | `8200`       | `8200`            | Gestión de secretos en desarrollo.  |
| `debezium-connect`   | `8083`       | `8083`            | Captura de Datos de Cambio (CDC).   |
| `api-gateway`        | `8080`       | `8080`            | Punto de entrada a la API.          |
| `auth-service`       | `8081`       | `8080`            | Servicio de autenticación.          |
| `redemption-service` | `8082`       | `8080`            | Lógica de canje de cupones.         |
| `station-service`    | N/A          | `8080`            | Gestión de estaciones.              |
| `ad-engine`          | `8084`       | `8080`            | Motor de anuncios.                  |
| `coupon-service`     | `8086`       | `8080`            | Creación y gestión de cupones.      |
| `raffle-service`     | Comentado    | `8080`            | **Inactivo en `docker-compose.yml`**. |

## 3. Dependencias Críticas y Vulnerabilidades (Análisis Inicial)

- **SCA (Software Composition Analysis):** Se requiere un análisis profundo con herramientas como Dependabot, Snyk o `gradle-dependency-check`. A primera vista, las dependencias NPM y Gradle deben ser escaneadas.
- **Vulnerabilidades conocidas:**
  - **Vault en Dev:** El token de Vault (`myroottoken`) está hardcodeado y el `api-gateway` escribe secretos al iniciar. **Esto es una vulnerabilidad crítica en el flujo de desarrollo** y debe ser remediado. Los secretos deben ser gestionados fuera del ciclo de vida de la aplicación.

## 4. Gestión de Secretos y Variables de Entorno

El archivo `.env.example` define la plantilla de configuración. Los servicios esperan las siguientes variables:

- `SPRING_PROFILES_ACTIVE`: Perfil de Spring (e.g., `docker`).
- `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: Credenciales de la base de datos.
- `REDIS_URL`: URL de conexión a Redis.
- `KAFKA_BOOTSTRAP_SERVERS`: (Placeholder) Configuración para Debezium.
- `JWT_SECRET`: Clave para firmar tokens JWT.
- `QR_PUBLIC_KEY`: Clave pública para verificar la firma de los QR.
- `GEOfence_RADIUS_METERS`: Configuración para `redemption-service`.
- `AD_FALLBACK_URL`: URL de fallback para `ad-engine`.

**Plan de Acción:**
1.  Centralizar la gestión de variables en `/ops/env/`.
2.  Eliminar la inicialización de secretos de Vault desde el `api-gateway`.
3.  Utilizar GitHub Actions Secrets para CI/CD y las herramientas de gestión de secretos de Render/Vercel para los despliegues.

## 5. Estado de Scripts y Gaps

El `Makefile` es el punto de entrada principal.

| Script              | Propósito                                      | Estado / Gaps                                                                                             |
| ------------------- | ---------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `make build-all`    | Construir todas las imágenes Docker.           | **Pendiente de ejecución.** Potenciales fallos si los `Dockerfile` no están bien configurados.            |
| `make dev`          | Levantar el entorno de desarrollo.             | **Pendiente de ejecución.** Depende de `build-all`. Puede fallar por problemas de red o dependencias.     |
| `make test`         | Ejecutar todos los tests.                      | **Pendiente de ejecución.** No hay garantía de que existan tests o de que pasen.                          |
| `make lint` / `format` | Formatear y verificar el estilo del código.    | **Pendiente de ejecución.** Necesario para la normalización del código.                                   |
| `make k8s-up/down`  | Desplegar en Kubernetes local.                 | Configuración de Helm presente en `infra/helm`. No es la opción prioritaria de despliegue (Render es la A). |
| `ops:vercel:setup`  | Script para configurar Vercel.                 | **No existe.** Debe ser creado como se especifica en la misión.                                           |

## 6. Health Endpoints y Observabilidad

- **Health Checks:**
  - `api-gateway` y `coupon-service` tienen `healthcheck` definidos en `docker-compose.yml`, apuntando a `/actuator/health`.
  - Se asume que todos los servicios Spring Boot exponen endpoints de Actuator (`/actuator/health`, `/actuator/prometheus`).
- **Observabilidad (Tracing):**
  - Jaeger está configurado en `docker-compose.yml`.
  - Los servicios Java parecen estar configurados para exportar traces a Jaeger (`OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317`).
  - **Gap:** No hay garantía de que el traceo esté implementado de manera consistente en el código (e.g., propagación de `trace-id`).

## 7. Siguientes Pasos Inmediatos

1.  **Crear rama `feature/initial-audit-and-build`** para aislar el trabajo.
2.  **Ejecutar `make build-all`** para identificar y corregir fallos de compilación en los servicios Docker.
3.  **Ejecutar `make dev`** para validar que el entorno de extremo a extremo levanta correctamente.
4.  Crear PRs `fix/build-service-<nombre>` para cada problema encontrado.
5.  Actualizar este documento y el `STATUS_REPORT.md` con los hallazgos.
