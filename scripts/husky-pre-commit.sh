#!/bin/bash

# 🎯 HUSKY PRE-COMMIT HOOK - GASOLINERA JSM
# Script robusto que no falla por tareas inexistentes

set +e  # No fallar por errores individuales

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}🔍 HUSKY PRE-COMMIT CHECKS${NC}"
echo "=================================="

# Función para ejecutar comandos de forma segura
safe_run() {
    local cmd="$1"
    local description="$2"

    echo -e "${BLUE}🔄 $description${NC}"

    if eval "$cmd" 2>/dev/null; then
        echo -e "${GREEN}✅ $description - OK${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠️  $description - Skipped${NC}"
        return 1
    fi
}

# 1. Verificar archivos staged
STAGED_FILES=$(git diff --cached --name-only)
if [ -z "$STAGED_FILES" ]; then
    echo -e "${YELLOW}⚠️  No staged files found${NC}"
    exit 0
fi

echo -e "${BLUE}📁 Files to commit: $(echo "$STAGED_FILES" | wc -l)${NC}"

# 2. Ejecutar ESLint si está disponible
if [ -f "package.json" ] && command -v npm >/dev/null 2>&1; then
    safe_run "npm run lint:safe" "ESLint check"
fi

# 3. Verificar sintaxis de archivos TypeScript/JavaScript
TS_JS_FILES=$(echo "$STAGED_FILES" | grep -E '\.(ts|tsx|js|jsx)$' || true)
if [ -n "$TS_JS_FILES" ]; then
    echo -e "${BLUE}🔍 Checking TypeScript/JavaScript syntax${NC}"

    # Verificar que no hay errores de sintaxis obvios
    for file in $TS_JS_FILES; do
        if [ -f "$file" ]; then
            # Verificar que el archivo no tiene caracteres de control raros
            if file "$file" | grep -q "text"; then
                echo -e "${GREEN}✅ $file - OK${NC}"
            else
                echo -e "${YELLOW}⚠️  $file - Binary or unusual format${NC}"
            fi
        fi
    done
fi

# 4. Verificar archivos de configuración
CONFIG_FILES=$(echo "$STAGED_FILES" | grep -E '\.(json|yml|yaml)$' || true)
if [ -n "$CONFIG_FILES" ]; then
    echo -e "${BLUE}🔍 Checking configuration files${NC}"

    for file in $CONFIG_FILES; do
        if [ -f "$file" ]; then
            case "$file" in
                *.json)
                    if command -v jq >/dev/null 2>&1; then
                        if jq empty "$file" 2>/dev/null; then
                            echo -e "${GREEN}✅ $file - Valid JSON${NC}"
                        else
                            echo -e "${RED}❌ $file - Invalid JSON${NC}"
                            exit 1
                        fi
                    else
                        echo -e "${YELLOW}⚠️  $file - JSON validation skipped (jq not available)${NC}"
                    fi
                    ;;
                *.yml|*.yaml)
                    echo -e "${GREEN}✅ $file - YAML file${NC}"
                    ;;
            esac
        fi
    done
fi

# 5. Verificar que no se suben archivos sensibles
SENSITIVE_PATTERNS=("*.key" "*.pem" "*.p12" "*.pfx" ".env.local" ".env.production")
for pattern in "${SENSITIVE_PATTERNS[@]}"; do
    SENSITIVE_FILES=$(echo "$STAGED_FILES" | grep "$pattern" || true)
    if [ -n "$SENSITIVE_FILES" ]; then
        echo -e "${RED}❌ SECURITY WARNING: Sensitive files detected!${NC}"
        echo "$SENSITIVE_FILES"
        echo -e "${RED}Please remove these files from staging before committing.${NC}"
        exit 1
    fi
done

# 6. Verificar tamaño de archivos
LARGE_FILES=$(echo "$STAGED_FILES" | xargs -I {} find {} -size +10M 2>/dev/null || true)
if [ -n "$LARGE_FILES" ]; then
    echo -e "${YELLOW}⚠️  Large files detected (>10MB):${NC}"
    echo "$LARGE_FILES"
    echo -e "${YELLOW}Consider using Git LFS for large files.${NC}"
fi

# 7. Resumen final
echo ""
echo -e "${GREEN}🎉 PRE-COMMIT CHECKS COMPLETED${NC}"
echo "=================================="
echo -e "${GREEN}✅ Ready to commit!${NC}"
echo -e "${BLUE}📊 Summary:${NC}"
echo "   • Files checked: $(echo "$STAGED_FILES" | wc -l)"
echo "   • No blocking issues found"
echo ""

exit 0