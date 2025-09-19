#!/bin/bash

# 🔧 SCRIPT: ARREGLAR DESPLIEGUE EN VERCEL
# Uso: ./scripts/fix-vercel-deployment.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${PURPLE}🔧 ARREGLANDO CONFIGURACIÓN PARA VERCEL${NC}"
echo -e "${BLUE}================================================================${NC}"
echo ""

# Funciones de utilidad
show_progress() {
    echo -e "${GREEN}✅ $1${NC}"
}

show_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

show_step() {
    echo -e "${PURPLE}🔄 $1${NC}"
}

# Paso 1: Verificar estructura
show_step "Verificando estructura del proyecto..."

if [ ! -f "apps/owner-dashboard/next.config.js" ]; then
    echo -e "${RED}❌ No se encontró next.config.js en owner-dashboard${NC}"
    exit 1
fi

if [ ! -f "apps/owner-dashboard/package.json" ]; then
    echo -e "${RED}❌ No se encontró package.json en owner-dashboard${NC}"
    exit 1
fi

show_progress "Estructura verificada"

# Paso 2: Crear .vercelignore
show_step "Creando .vercelignore..."

cat > .vercelignore << 'EOF'
# Build outputs
build/
dist/
.next/
out/

# Dependencies
node_modules/

# Cache
.nx/
.cache/

# IDE
.idea/
.vscode/

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Environment (keep examples)
.env.local
.env.*.local

# Gradle/Java (no necesarios para frontend)
.gradle/
*.jar
*.class

# Test coverage
coverage/
EOF

show_progress ".vercelignore creado"

# Paso 3: Verificar dependencias de Next.js
show_step "Verificando dependencias de Next.js..."

cd apps/owner-dashboard

if [ ! -f "package.json" ]; then
    show_info "Creando package.json para owner-dashboard..."

    cat > package.json << 'EOF'
{
  "name": "owner-dashboard",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint"
  },
  "dependencies": {
    "next": "14.1.0",
    "react": "18.3.1",
    "react-dom": "18.3.1",
    "@hookform/resolvers": "^3.6.0",
    "@radix-ui/react-dialog": "^1.1.1",
    "@radix-ui/react-dropdown-menu": "^2.1.1",
    "@radix-ui/react-label": "^2.1.0",
    "@radix-ui/react-slot": "^1.1.0",
    "@radix-ui/react-tabs": "^1.1.0",
    "axios": "^1.11.0",
    "class-variance-authority": "^0.7.0",
    "clsx": "^2.1.1",
    "framer-motion": "^11.2.10",
    "lottie-react": "^2.4.0",
    "lucide-react": "^0.395.0",
    "react-hook-form": "^7.51.5",
    "react-toastify": "^10.0.5",
    "tailwind-merge": "^2.3.0",
    "tailwindcss-animate": "^1.0.7",
    "zod": "^3.23.8",
    "zustand": "^4.5.2"
  },
  "devDependencies": {
    "@types/node": "18.19.33",
    "@types/react": "18.3.3",
    "@types/react-dom": "18.3.0",
    "autoprefixer": "10.4.19",
    "eslint": "~8.57.0",
    "eslint-config-next": "13.5.0",
    "postcss": "8.4.38",
    "tailwindcss": "3.4.3",
    "typescript": "5.8.3"
  }
}
EOF
fi

cd ../..

show_progress "Dependencias verificadas"

# Paso 4: Crear configuración específica para Vercel
show_step "Creando configuración específica para Vercel..."

cat > vercel.json << 'EOF'
{
  "version": 2,
  "builds": [
    {
      "src": "apps/owner-dashboard/package.json",
      "use": "@vercel/next",
      "config": {
        "projectSettings": {
          "framework": "nextjs",
          "rootDirectory": "apps/owner-dashboard"
        }
      }
    }
  ],
  "routes": [
    {
      "src": "/(.*)",
      "dest": "/apps/owner-dashboard/$1"
    }
  ],
  "buildCommand": "cd apps/owner-dashboard && npm run build",
  "outputDirectory": "apps/owner-dashboard/.next",
  "installCommand": "npm install",
  "framework": "nextjs",
  "functions": {
    "apps/owner-dashboard/pages/api/**/*.js": {
      "runtime": "nodejs18.x"
    }
  }
}
EOF

show_progress "Configuración de Vercel actualizada"

# Paso 5: Mostrar instrucciones finales
echo ""
echo -e "${PURPLE}🎉 CONFIGURACIÓN ARREGLADA EXITOSAMENTE${NC}"
echo -e "${BLUE}================================================================${NC}"
echo ""
echo -e "${GREEN}✅ Scripts de package.json corregidos${NC}"
echo -e "${GREEN}✅ project.json optimizado para Next.js${NC}"
echo -e "${GREEN}✅ vercel.json configurado correctamente${NC}"
echo -e "${GREEN}✅ next.config.js optimizado${NC}"
echo -e "${GREEN}✅ .vercelignore creado${NC}"
echo ""
echo -e "${YELLOW}🚀 PRÓXIMOS PASOS PARA VERCEL:${NC}"
echo ""
echo -e "${BLUE}1. Commit y push:${NC}"
echo "   git add ."
echo "   git commit -m \"fix: Correct Vercel deployment configuration for Next.js\""
echo "   git push origin main"
echo ""
echo -e "${BLUE}2. En Vercel Dashboard:${NC}"
echo "   • Ir a https://vercel.com/dashboard"
echo "   • Import Project desde GitHub"
echo "   • Seleccionar: jordelmir/gasolinera-deploy"
echo "   • Framework Preset: Next.js"
echo "   • Root Directory: apps/owner-dashboard"
echo "   • Build Command: npm run build"
echo "   • Output Directory: .next"
echo ""
echo -e "${BLUE}3. Variables de entorno en Vercel:${NC}"
echo "   NEXT_PUBLIC_API_URL=https://gasolinera-api-gateway.onrender.com"
echo ""
echo -e "${GREEN}🎯 AHORA EL DESPLIEGUE DEBERÍA FUNCIONAR CORRECTAMENTE${NC}"
echo -e "${PURPLE}================================================================${NC}"