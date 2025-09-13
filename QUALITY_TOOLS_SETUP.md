# 🔧 Quality Tools Setup - Gasolinera JSM

Este documento describe la configuración completa de herramientas de calidad de código implementada en el proyecto.

## 📋 Herramientas Configuradas

### 1. **Detekt** - Análisis Estático de Código Kotlin

- **Versión:** 1.23.6 (compatible con Kotlin 1.9.24)
- **Configuración:** `config/detekt/detekt.yml`
- **Características:**
  - Análisis de complejidad de código
  - Detección de code smells
  - Verificación de convenciones de naming
  - Auto-corrección de issues simples
  - Reportes en HTML, XML y SARIF

### 2. **Ktlint** - Formateo de Código Kotlin

- **Versión:** 12.1.1
- **Características:**
  - Formateo automático de código
  - Verificación de estilo de código
  - Integración con IDE
  - Auto-corrección de problemas de formato

### 3. **JaCoCo** - Cobertura de Código

- **Versión:** 0.8.11
- **Configuración:**
  - Cobertura mínima: 80%
  - Reportes en XML y HTML
  - Integración con SonarQube
  - Verificación automática en CI

### 4. **SonarQube** - Análisis de Calidad Integral

- **Versión:** 5.1.0.4882
- **Configuración:** `sonar-project.properties`
- **Métricas:**
  - Code coverage
  - Code duplication
  - Security vulnerabilities
  - Technical debt
  - Maintainability rating

## 🚀 Comandos Disponibles

### Comandos de Gradle

```bash
# Verificación de calidad
./gradlew ktlintCheck          # Verificar formato de código
./gradlew detekt              # Análisis estático
./gradlew test jacocoTestReport # Tests con cobertura

# Auto-corrección
./gradlew ktlintFormat        # Formatear código automáticamente
./gradlew detekt --auto-correct # Corregir issues de detekt
```

### Comandos de Make

```bash
# Verificación completa
make quality-check            # Ejecutar todas las verificaciones
make auto-fix                # Auto-corregir problemas comunes

# Comandos específicos
make gradle-ktlint           # Solo ktlint check
make gradle-detekt           # Solo detekt analysis
make gradle-test             # Solo tests con cobertura

# Pre-commit
make pre-commit              # Auto-fix + quality-check
```

### Scripts Personalizados

```bash
# Verificación completa con reporte detallado
./scripts/quality-check.sh

# Auto-corrección de problemas comunes
./scripts/auto-fix.sh
```

## 🔄 Integración con CI/CD

### GitHub Actions

- **Workflow:** `.github/workflows/ci-quality-check.yml`
- **Triggers:** Push y Pull Request
- **Checks:**
  - Ktlint format verification
  - Detekt static analysis
  - Unit tests with coverage
  - Security vulnerability scan
  - SonarQube analysis (main/develop branches)

### Pre-commit Hooks

- **Configuración:** `.husky/pre-commit`
- **Acciones:**
  1. Auto-fix de problemas comunes
  2. Verificación de calidad completa
  3. Bloqueo de commit si hay errores críticos

## 📊 Métricas de Calidad

### Umbrales Configurados

- **Cobertura de Tests:** Mínimo 80%
- **Complejidad Ciclomática:** Máximo 15 por método
- **Líneas por Método:** Máximo 60
- **Parámetros por Función:** Máximo 6
- **Longitud de Línea:** Máximo 120 caracteres

### Reportes Generados

- **Detekt:** `build/reports/detekt/detekt.html`
- **JaCoCo:** `build/reports/jacoco/test/jacocoTestReport/index.html`
- **Test Results:** `build/reports/tests/test/index.html`

## 🛠️ Configuración por Servicio

Cada servicio hereda automáticamente la configuración de calidad:

```kotlin
// build.gradle.kts (aplicado automáticamente)
apply(plugin = "io.gitlab.arturbosch.detekt")
apply(plugin = "org.jlleitschuh.gradle.ktlint")
apply(plugin = "jacoco")
```

### Exclusiones Configuradas

- **Detekt:** Archivos generados, tests
- **JaCoCo:** DTOs, entities, configuración
- **Ktlint:** Build artifacts, generated code

## 🔍 Verificación Local

### Antes de Commit

```bash
# Opción 1: Usar make
make pre-commit

# Opción 2: Usar script directo
./scripts/quality-check.sh

# Opción 3: Comandos individuales
./gradlew ktlintCheck detekt test
```

### Resolución de Problemas Comunes

#### 1. Errores de Formato (Ktlint)

```bash
# Auto-fix
./gradlew ktlintFormat
# o
make gradle-ktlint-fix
```

#### 2. Issues de Detekt

```bash
# Ver reporte detallado
open build/reports/detekt/detekt.html

# Auto-fix (issues simples)
./gradlew detekt --auto-correct
```

#### 3. Cobertura Insuficiente

```bash
# Ver reporte de cobertura
open build/reports/jacoco/test/jacocoTestReport/index.html

# Ejecutar tests específicos
./gradlew :services:SERVICE_NAME:test
```

## 📈 Integración con IDEs

### IntelliJ IDEA / Android Studio

1. **Ktlint Plugin:** Instalar "ktlint" plugin
2. **Detekt Plugin:** Instalar "Detekt" plugin
3. **Configuración:** Los plugins detectarán automáticamente la configuración del proyecto

### VS Code

1. **Kotlin Extension:** Instalar extensión oficial de Kotlin
2. **EditorConfig:** Respeta automáticamente `.editorconfig`

## 🚨 Troubleshooting

### Problemas Comunes

#### Error: "detekt was compiled with Kotlin X but is currently running with Y"

**Solución:** Verificar compatibilidad de versiones en `build.gradle.kts`

#### Error: "No repositories are defined"

**Solución:** Verificar que `repositories` esté configurado en el módulo

#### Tests fallan en CI pero pasan localmente

**Solución:** Verificar variables de entorno y configuración de TestContainers

### Logs de Debug

```bash
# Gradle con logs detallados
./gradlew build --info --stacktrace

# Detekt con debug
./gradlew detekt --debug

# Tests con logs detallados
./gradlew test --info
```

## 📚 Referencias

- [Detekt Documentation](https://detekt.dev/)
- [Ktlint Documentation](https://ktlint.github.io/)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [SonarQube Documentation](https://docs.sonarqube.org/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

---

_Última actualización: $(date)_
_Configurado por: Kiro AI Assistant_
