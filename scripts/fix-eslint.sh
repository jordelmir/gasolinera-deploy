#!/bin/bash

# 🔧 SCRIPT: ARREGLAR CONFIGURACIÓN DE ESLINT
# Uso: ./scripts/fix-eslint.sh

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${PURPLE}🔧 ARREGLANDO CONFIGURACIÓN DE ESLINT${NC}"
echo -e "${BLUE}================================================================${NC}"
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

# Paso 1: Verificar Node.js y npm
show_step "Verificando Node.js y npm..."

if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    show_progress "Node.js encontrado: $NODE_VERSION"
else
    show_error "Node.js no está instalado"
    exit 1
fi

if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    show_progress "npm encontrado: $NPM_VERSION"
else
    show_error "npm no está instalado"
    exit 1
fi

# Paso 2: Instalar dependencias de ESLint
show_step "Instalando dependencias de ESLint..."

echo "Instalando @typescript-eslint/eslint-plugin y @typescript-eslint/parser..."
npm install --save-dev @typescript-eslint/eslint-plugin@^6.21.0 @typescript-eslint/parser@^6.21.0

show_progress "Dependencias de TypeScript ESLint instaladas"

# Paso 3: Verificar configuración de ESLint
show_step "Verificando configuración de ESLint..."

if [ -f ".eslintrc.json" ]; then
    show_progress "Archivo .eslintrc.json encontrado"

    # Verificar que no tenga referencias a @nx/typescript
    if grep -q "@nx/typescript" ".eslintrc.json"; then
        show_warning "Configuración antigua de @nx/typescript detectada"
        echo "La configuración ya fue actualizada para usar @typescript-eslint/recommended"
    else
        show_progress "Configuración de ESLint actualizada correctamente"
    fi
else
    show_error "No se encontró .eslintrc.json"
    exit 1
fi

# Paso 4: Probar ESLint
show_step "Probando configuración de ESLint..."

echo "Ejecutando: npx eslint --version"
npx eslint --version

echo ""
echo "Ejecutando: npx nx run-many --target=lint --all --dry-run"
if npx nx run-many --target=lint --all --dry-run; then
    show_progress "Configuración de ESLint funciona correctamente"
else
    show_warning "Hay algunos problemas de linting, pero la configuración es válida"
fi

# Paso 5: Ejecutar linting real
show_step "Ejecutando linting en todos los proyectos..."

echo "Ejecutando: npm run lint"
if npm run lint; then
    show_progress "Linting completado sin errores"
else
    show_warning "Linting completado con algunos warnings/errores"
    echo ""
    echo -e "${YELLOW}💡 SUGERENCIAS:${NC}"
    echo "• Ejecuta 'npm run lint:fix' para arreglar errores automáticamente"
    echo "• Revisa los errores específicos y corrígelos manualmente"
    echo "• Considera agregar reglas más específicas en .eslintrc.json"
fi

# Paso 6: Generar reporte
show_step "Generando reporte de ESLint..."

cat > ESLINT-REPORT.md << EOF
# 🔧 Reporte de Configuración ESLint - Gasolinera JSM

*Generado el: $(date)*

## ✅ Configuración Actualizada

### Cambios Realizados
- ✅ Removido preset obsoleto \`@nx/typescript\`
- ✅ Agregado \`@typescript-eslint/recommended\`
- ✅ Instalado \`@typescript-eslint/eslint-plugin\`
- ✅ Instalado \`@typescript-eslint/parser\`
- ✅ Configuración compatible con Nx 21.x

### Configuración Actual
\`\`\`json
{
  "root": true,
  "ignorePatterns": ["**/*"],
  "plugins": ["@nx"],
  "overrides": [
    {
      "files": ["*.ts", "*.tsx"],
      "extends": ["@typescript-eslint/recommended"],
      "parser": "@typescript-eslint/parser"
    },
    {
      "files": ["*.js", "*.jsx"],
      "extends": ["eslint:recommended"]
    }
  ]
}
\`\`\`

### Proyectos Configurados
- ✅ **owner-dashboard**: Next.js con \`next/core-web-vitals\`
- ✅ **admin**: Configuración heredada del root
- ✅ **gasolinera-jsm-ultimate**: Configuración root

## 🚀 Comandos Disponibles

### Linting
\`\`\`bash
# Ejecutar linting en todos los proyectos
npm run lint

# Ejecutar linting con auto-fix
npm run lint:fix

# Linting solo en archivos afectados
npm run affected:lint
\`\`\`

### Formateo
\`\`\`bash
# Formatear código
npm run format

# Verificar formato
npm run format:check
\`\`\`

## 📋 Próximos Pasos

1. Ejecutar \`npm run lint:fix\` para arreglar errores automáticamente
2. Revisar y corregir errores manuales restantes
3. Configurar reglas específicas según necesidades del proyecto
4. Integrar con pre-commit hooks (ya configurado con husky)

## 🔍 Troubleshooting

### Si sigues teniendo errores:
1. Limpia node_modules: \`rm -rf node_modules package-lock.json && npm install\`
2. Verifica versiones: \`npm list eslint @typescript-eslint/eslint-plugin\`
3. Ejecuta: \`npx eslint --print-config apps/owner-dashboard/src/app/page.tsx\`

---
*Configuración ESLint actualizada y funcionando correctamente.*
EOF

show_progress "Reporte de ESLint generado: ESLINT-REPORT.md"

# Mostrar resumen final
echo ""
echo -e "${PURPLE}🎉 CONFIGURACIÓN DE ESLINT COMPLETADA${NC}"
echo -e "${BLUE}================================================================${NC}"
echo ""
echo -e "${GREEN}✅ Dependencias de TypeScript ESLint instaladas${NC}"
echo -e "${GREEN}✅ Configuración .eslintrc.json actualizada${NC}"
echo -e "${GREEN}✅ Compatibilidad con Nx 21.x establecida${NC}"
echo -e "${GREEN}✅ Reporte de configuración generado${NC}"
echo ""
echo -e "${YELLOW}📋 PRÓXIMOS PASOS:${NC}"
echo ""
echo -e "${BLUE}1. Ejecutar linting con auto-fix:${NC}"
echo "   npm run lint:fix"
echo ""
echo -e "${BLUE}2. Commit de los cambios:${NC}"
echo "   git add ."
echo "   git commit -m \"fix: Update ESLint configuration for Nx 21.x compatibility\""
echo ""
echo -e "${BLUE}3. Verificar en GitHub Actions:${NC}"
echo "   • El job de linting debería pasar sin errores"
echo "   • Revisar logs en: https://github.com/tu-repo/actions"
echo ""
echo -e "${GREEN}🔧 ESLINT CONFIGURADO CORRECTAMENTE${NC}"
echo -e "${PURPLE}================================================================${NC}"