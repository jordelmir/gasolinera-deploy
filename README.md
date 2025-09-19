# 🚀 Gasolinera JSM - Sistema de Gestión Completo

Sistema completo de gestión para gasolineras con arquitectura de microservicios, desarrollado en Spring Boot + Kotlin y frontend en Next.js.

## 🏗️ Arquitectura del Sistema

### Backend (Microservicios)
- **API Gateway** (Puerto 8080) - Punto de entrada único
- **Auth Service** (Puerto 8091) - Autenticación y autorización
- **Station Service** (Puerto 8092) - Gestión de estaciones
- **Coupon Service** (Puerto 8093) - Sistema de cupones
- **Raffle Service** (Puerto 8094) - Sistema de rifas
- **Redemption Service** (Puerto 8095) - Canje de recompensas
- **Ad Engine** (Puerto 8096) - Motor de publicidad
- **Message Improver** (Puerto 8097) - Mejora de mensajes

### Frontend
- **Admin Dashboard** (Next.js) - Panel administrativo
- **Owner Dashboard** (Next.js) - Dashboard para propietarios
- **Mobile Apps** (React Native) - Apps móviles

### Infraestructura
- **PostgreSQL** - Base de datos principal
- **Redis** - Cache y sesiones
- **RabbitMQ** - Mensajería asíncrona
- **Prometheus + Grafana** - Monitoreo

## 🚀 Despliegue Rápido

### Opción 1: Render.com (Recomendado - GRATIS)
```bash
# 1. Ejecutar script de preparación
./scripts/deploy-render-complete.sh

# 2. Commit y push
git add .
git commit -m "feat: Ready for Render deployment"
git push origin main

# 3. Ir a render.com y conectar este repositorio
# 4. Usar el archivo render.yaml para configuración automática
```

### Opción 2: Docker Local
```bash
# Compilar proyecto
./gradlew build -x :integration-tests:test --no-daemon

# Iniciar con Docker Compose
docker-compose -f docker-compose.production.yml up -d
```

### Opción 3: Desarrollo Local
```bash
# 1. Iniciar infraestructura
docker-compose -f docker-compose.simple.yml up -d

# 2. Compilar servicios
./gradlew build -x :integration-tests:test --no-daemon

# 3. Iniciar servicios
./start-services-fixed.sh

# 4. Verificar sistema
./verify-system.sh
```

## 📊 URLs de Acceso (Después del Despliegue)

### Render.com
- **API Gateway**: `https://gasolinera-api-gateway.onrender.com`
- **Admin Panel**: `https://gasolinera-admin-frontend.onrender.com`
- **Health Check**: `https://gasolinera-api-gateway.onrender.com/health`

### Local
- **API Gateway**: `http://localhost:8080`
- **Admin Panel**: `http://localhost:3000`
- **Health Check**: `http://localhost:8080/health`

## 🛠️ Comandos Útiles

```bash
# Compilar proyecto completo
./gradlew build -x :integration-tests:test --no-daemon

# Verificar estado del sistema
./verify-system.sh

# Iniciar servicios localmente
./start-services-fixed.sh

# Detener servicios
./stop-services.sh

# Preparar para despliegue en Render
./scripts/deploy-render-complete.sh
```

## 📚 Documentación de Despliegue

- [`DESPLIEGUE-RENDER-COMPLETO.md`](./DESPLIEGUE-RENDER-COMPLETO.md) - Guía completa para Render.com
- [`DESPLIEGUE-VERCEL-RAILWAY.md`](./DESPLIEGUE-VERCEL-RAILWAY.md) - Estrategia híbrida
- [`DESPLIEGUE-DOCKER-COMPLETO.md`](./DESPLIEGUE-DOCKER-COMPLETO.md) - Despliegue con Docker
- [`DESPLIEGUE-AWS-ENTERPRISE.md`](./DESPLIEGUE-AWS-ENTERPRISE.md) - Solución enterprise
- [`GUIA-DESPLIEGUE-MAGISTRAL.md`](./GUIA-DESPLIEGUE-MAGISTRAL.md) - Recomendaciones estratégicas

## 🎯 Inicio Rápido (5 minutos)

1. **Clonar repositorio**
   ```bash
   git clone https://github.com/jordelmir/gasolinera-deploy.git
   cd gasolinera-deploy
   ```

2. **Preparar para despliegue**
   ```bash
   ./scripts/deploy-render-complete.sh
   ```

3. **Subir a GitHub y conectar con Render.com**
   - El archivo `render.yaml` está listo
   - Configuración automática incluida
   - ¡Sistema funcionando en 2-3 horas!

## 🏆 Características Principales

- ✅ **Arquitectura de microservicios** escalable
- ✅ **Frontend moderno** con Next.js
- ✅ **Base de datos robusta** con PostgreSQL
- ✅ **Cache inteligente** con Redis
- ✅ **Mensajería asíncrona** con RabbitMQ
- ✅ **Monitoreo completo** con Prometheus/Grafana
- ✅ **Despliegue automático** con Docker
- ✅ **SSL automático** en producción
- ✅ **Escalabilidad horizontal** lista

## 📞 Soporte

Para soporte técnico o preguntas sobre el despliegue, consulta la documentación en la carpeta de guías de despliegue.

---

**¡Tu sistema Gasolinera JSM estará funcionando en internet en menos de 4 horas!** 🚀
