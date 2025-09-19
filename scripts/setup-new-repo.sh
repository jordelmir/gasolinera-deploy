#!/bin/bash

# 🚀 SCRIPT: CONFIGURAR NUEVO REPOSITORIO PARA DESPLIEGUE
# Uso: ./scripts/setup-new-repo.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
NEW_REPO_URL="https://github.com/jordelmir/gasolinera-deploy.git"

echo -e "${PURPLE}🚀 CONFIGURANDO NUEVO REPOSITORIO PARA DESPLIEGUE${NC}"
echo -e "${BLUE}================================================================${NC}"
echo -e "${BLUE}Repositorio destino: ${NEW_REPO_URL}${NC}"
echo ""

# Funciones de utilidad
show_progress() {
    echo -e "${GREEN}✅ $1${NC}"
}

show_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

show_error() {
    echo -e "${RED}❌ $1${NC}"
}

show_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

show_step() {
    echo -e "${PURPLE}🔄 $1${NC}"
}

# Paso 1: Verificar prerrequisitos
check_prerequisites() {
    show_step "Verificando prerrequisitos..."

    cd "$PROJECT_ROOT"

    # Verificar Git
    if ! command -v git &> /dev/null; then
        show_error "Git no está instalado"
        exit 1
    fi

    # Verificar que estamos en el directorio correcto
    if [ ! -f "gradlew" ]; then
        show_error "No estamos en el directorio raíz del proyecto"
        exit 1
    fi

    show_progress "Prerrequisitos verificados"
}

# Paso 2: Limpiar archivos innecesarios
clean_project() {
    show_step "Limpiando archivos innecesarios para despliegue..."

    cd "$PROJECT_ROOT"

    # Crear .gitignore optimizado para despliegue
    cat > .gitignore << 'EOF'
# Build outputs
build/
target/
*.jar
*.war
*.ear
*.class

# Gradle
.gradle/
gradle-app.setting
!gradle-wrapper.jar
!gradle-wrapper.properties

# IDE
.idea/
.vscode/
*.iml
*.ipr
*.iws
.project
.classpath
.settings/

# OS
.DS_Store
Thumbs.db

# Logs
logs/
*.log

# Runtime
tmp/
temp/
.tmp/

# Node modules (para frontend)
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Next.js
.next/
out/

# Environment variables (mantener ejemplos)
.env.local
.env.development.local
.env.test.local
.env.production.local

# Docker
.dockerignore

# Cache
.nx/cache/
.cache/

# Test coverage
coverage/
.nyc_output/

# Dependency directories
jspm_packages/

# Optional npm cache directory
.npm

# Optional REPL history
.node_repl_history

# Output of 'npm pack'
*.tgz

# Yarn Integrity file
.yarn-integrity

# parcel-bundler cache (https://parceljs.org/)
.parcel-cache

# Stores VSCode versions used for testing VSCode extensions
.vscode-test

# Temporary folders
tmp/
temp/

# Runtime data
pids
*.pid
*.seed
*.pid.lock

# Coverage directory used by tools like istanbul
coverage
*.lcov

# Compiled binary addons (https://nodejs.org/api/addons.html)
build/Release

# Dependency directories
node_modules/
jspm_packages/

# Snowpack dependency directory (https://snowpack.dev/)
web_modules/

# Rollup.js default build output
dist/

# Stores VSCode versions used for testing VSCode extensions
.vscode-test

# Microbundle cache
.rpt2_cache/
.rts2_cache_cjs/
.rts2_cache_es/
.rts2_cache_umd/

# Optional REPL history
.node_repl_history

# Output of 'npm pack'
*.tgz

# Yarn Integrity file
.yarn-integrity

# dotenv environment variables file
.env.test

# Stores VSCode versions used for testing VSCode extensions
.vscode-test

# Temporary folders
tmp/
temp/
EOF

    # Limpiar builds anteriores
    show_info "Limpiando builds anteriores..."
    ./gradlew clean --no-daemon || true

    # Remover directorios de build
    find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
    find . -name ".gradle" -type d -exec rm -rf {} + 2>/dev/null || true

    # Limpiar logs
    rm -rf logs/*.log 2>/dev/null || true

    # Limpiar node_modules si existen
    find . -name "node_modules" -type d -exec rm -rf {} + 2>/dev/null || true

    show_progress "Proyecto limpiado"
}

# Paso 3: Crear README optimizado para despliegue
create_deployment_readme() {
    show_step "Creando README optimizado para despliegue..."

    cd "$PROJECT_ROOT"

    cat > README.md << 'EOF'
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
EOF

    show_progress "README de despliegue creado"
}

# Paso 4: Configurar Git para el nuevo repositorio
setup_git() {
    show_step "Configurando Git para el nuevo repositorio..."

    cd "$PROJECT_ROOT"

    # Verificar si ya es un repositorio Git
    if [ -d ".git" ]; then
        show_info "Repositorio Git existente detectado"

        # Hacer backup del remote actual
        current_remote=$(git remote get-url origin 2>/dev/null || echo "none")
        if [ "$current_remote" != "none" ]; then
            show_info "Remote actual: $current_remote"
            git remote rename origin old-origin 2>/dev/null || true
        fi
    else
        show_info "Inicializando nuevo repositorio Git"
        git init
    fi

    # Configurar nuevo remote
    git remote add origin "$NEW_REPO_URL" 2>/dev/null || git remote set-url origin "$NEW_REPO_URL"

    # Configurar branch principal
    git branch -M main 2>/dev/null || true

    show_progress "Git configurado para el nuevo repositorio"
}

# Paso 5: Preparar commit inicial
prepare_initial_commit() {
    show_step "Preparando commit inicial..."

    cd "$PROJECT_ROOT"

    # Agregar todos los archivos
    git add .

    # Verificar si hay cambios para commitear
    if git diff --staged --quiet; then
        show_warning "No hay cambios para commitear"
    else
        show_info "Archivos preparados para commit inicial"

        # Mostrar estadísticas
        echo ""
        show_info "Estadísticas del commit:"
        git diff --staged --stat
        echo ""
    fi

    show_progress "Commit inicial preparado"
}

# Paso 6: Mostrar instrucciones finales
show_final_instructions() {
    echo ""
    echo -e "${PURPLE}🎉 REPOSITORIO PREPARADO EXITOSAMENTE${NC}"
    echo -e "${BLUE}================================================================${NC}"
    echo ""
    echo -e "${GREEN}✅ Proyecto limpiado y optimizado${NC}"
    echo -e "${GREEN}✅ README de despliegue creado${NC}"
    echo -e "${GREEN}✅ Git configurado para nuevo repositorio${NC}"
    echo -e "${GREEN}✅ Archivos preparados para commit${NC}"
    echo ""
    echo -e "${YELLOW}🚀 PRÓXIMOS PASOS:${NC}"
    echo ""
    echo -e "${BLUE}1. Hacer commit inicial:${NC}"
    echo "   git commit -m \"feat: Initial commit - Gasolinera JSM deployment ready\""
    echo ""
    echo -e "${BLUE}2. Subir al nuevo repositorio:${NC}"
    echo "   git push -u origin main"
    echo ""
    echo -e "${BLUE}3. Verificar en GitHub:${NC}"
    echo "   https://github.com/jordelmir/gasolinera-deploy"
    echo ""
    echo -e "${BLUE}4. Preparar para despliegue:${NC}"
    echo "   ./scripts/deploy-render-complete.sh"
    echo ""
    echo -e "${BLUE}5. Desplegar en Render.com:${NC}"
    echo "   • Ir a https://render.com"
    echo "   • Conectar repositorio GitHub"
    echo "   • Usar render.yaml para configuración automática"
    echo ""
    echo -e "${GREEN}🎯 REPOSITORIO LISTO PARA DESPLIEGUE${NC}"
    echo -e "${PURPLE}================================================================${NC}"
}

# Función principal
main() {
    check_prerequisites
    clean_project
    create_deployment_readme
    setup_git
    prepare_initial_commit
    show_final_instructions
}

# Manejo de errores
trap 'echo -e "${RED}❌ Error durante la configuración. Revisa los logs arriba.${NC}"; exit 1' ERR

# Ejecutar función principal
main "$@"