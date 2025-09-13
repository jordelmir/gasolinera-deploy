# 🛡️ Quality Gates & Static Analysis Guide - Gasolinera JSM

## 📋 Overview

Esta guía documenta la configuración completa de quality gates y análisis estático implementado en Gasolinera JSM para mantener estándares de código de clase mundial. Incluye SonarQube, security scanning, dependency vulnerability analysis y reportes de technical debt.

## 🎯 Quality Standards

### Code Quality Metrics

| Metric                     | Threshold | Error Threshold | Description                      |
| -------------------------- | --------- | --------------- | -------------------------------- |
| **Code Coverage**          | ≥ 85%     | ≥ 80%           | Minimum test coverage required   |
| **New Code Coverage**      | ≥ 90%     | ≥ 85%           | Coverage for new/modified code   |
| **Duplicated Lines**       | < 3%      | < 5%            | Maximum code duplication allowed |
| **Maintainability Rating** | A         | B               | Code maintainability score       |
| **Reliability Rating**     | A         | B               | Code reliability score           |
| **Security Rating**        | A         | B               | Security vulnerability score     |
| **Cognitive Complexity**   | < 15      | < 20            | Per function complexity limit    |
| **Cyclomatic Complexity**  | < 10      | < 15            | Per function complexity limit    |

### Violation Thresholds

| Severity     | Allowed Count | New Code | Action Required        |
| ------------ | ------------- | -------- | ---------------------- |
| **Blocker**  | 0             | 0        | Immediate fix required |
| **Critical** | 0             | 0        | Same-day fix required  |
| **Major**    | < 5           | 0        | Fix within sprint      |
| **Minor**    | < 20          | < 5      | Fix next sprint        |
| **Info**     | < 50          | < 10     | Backlog item           |

## 🔧 Tools Configuration

### 1. SonarQube Setup

#### Project Configuration

```properties
# sonar-project.properties
sonar.projectKey=gasolinera-jsm-ultimate
sonar.projectName=Gasolinera JSM Ultimate
sonar.projectVersion=1.0.0
sonar.organization=gasolinera-jsm

# Source and test directories
sonar.sources=src/main
sonar.tests=src/test
sonar.java.binaries=build/classes
sonar.junit.reportPaths=build/test-results/test

# Coverage reports
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml

# Quality gate settings
sonar.qualitygate.wait=true
```

#### Quality Gate Configuration

```bash
# Create custom quality gate
curl -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/create" \
  -H "Authorization: Bearer ${SONAR_TOKEN}" \
  -d "name=Gasolinera JSM Quality Gate"

# Add conditions
curl -X POST \
  "${SONAR_HOST_URL}/api/qualitygates/create_condition" \
  -H "Authorization: Bearer ${SONAR_TOKEN}" \
  -d "gateId=${GATE_ID}&metric=coverage&op=LT&error=85"
```

### 2. Detekt Static Analysis

#### Configuration File (`config/detekt/detekt.yml`)

```yaml
complexity:
  active: true
  ComplexMethod:
    active: true
    threshold: 15
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    functionThreshold: 6
    constructorThreshold: 7

style:
  active: true
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
  MaxLineLength:
    active: true
    maxLineLength: 120

potential-bugs:
  active: true
  UnsafeCallOnNullableType:
    active: true
  UnreachableCode:
    active: true
```

#### Gradle Integration

```kotlin
// build.gradle.kts
detekt {
    toolVersion = "1.23.4"
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = true

    reports {
        html.required.set(true)
        xml.required.set(true)
        sarif.required.set(true)
    }
}
```

### 3. KtLint Code Formatting

#### Configuration

```kotlin
// build.gradle.kts
ktlint {
    version.set("1.0.1")
    debug.set(false)
    verbose.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(false)

    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
}
```

#### Usage

```bash
# Check formatting
./gradlew ktlintCheck

# Auto-fix formatting issues
./gradlew ktlintFormat

# Generate reports
./gradlew ktlintMainSourceSetCheck --continue
```

### 4. OWASP Dependency Check

#### Configuration

```kotlin
// build.gradle.kts
dependencyCheck {
    autoUpdate = true
    format = ReportGenerator.Format.ALL

    // Fail build on CVSS >= 7.0
    failBuildOnCVSS = 7.0f

    // Suppress false positives
    suppressionFile = "$rootDir/config/owasp/suppressions.xml"

    // NVD API configuration
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        delay = 16000 // 16 seconds between requests
    }
}
```

#### Suppression File Example

```xml
<!-- config/owasp/suppressions.xml -->
<suppressions xmlns="https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd">
    <suppress>
        <notes>Spring Boot false positive for CVE-2016-1000027</notes>
        <packageUrl regex="true">^pkg:maven/org\.springframework\.boot/spring-boot-starter.*@.*$</packageUrl>
        <cve>CVE-2016-1000027</cve>
    </suppress>
</suppressions>
```

### 5. JaCoCo Code Coverage

#### Configuration

```kotlin
// build.gradle.kts
jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.85".toBigDecimal() // 85% minimum
            }
        }

        rule {
            element = "CLASS"
            excludes = listOf(
                "*.config.*",
                "*.dto.*",
                "*.entity.*",
                "*.*Application*"
            )

            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
```

## 🚀 CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/quality-gates.yml
name: Quality Gates & Security Analysis

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  code-quality:
    name: Code Quality Analysis
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run Quality Checks
        run: |
          ./gradlew ktlintCheck
          ./gradlew detekt
          ./gradlew test jacocoTestReport
          ./gradlew jacocoTestCoverageVerification

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml

  security-analysis:
    name: Security Analysis
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Run OWASP Dependency Check
        env:
          NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
        run: ./gradlew dependencyCheckAnalyze

      - name: Upload Security Results
        uses: github/codeql-action/upload-sarif@v3
        with:
          sarif_file: build/reports/dependency-check-report.sarif

  sonarqube-analysis:
    name: SonarQube Analysis
    runs-on: ubuntu-latest
    needs: [code-quality]

    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Build and analyze
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: |
          ./gradlew build sonarqube \
            -Dsonar.projectKey=gasolinera-jsm-ultimate \
            -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
            -Dsonar.login=${{ secrets.SONAR_TOKEN }}
```

### Quality Gate Enforcement

```yaml
# Branch protection rules
quality-gate-check:
  name: Quality Gate Status Check
  runs-on: ubuntu-latest
  needs: [sonarqube-analysis]

  steps:
    - name: Check SonarQube Quality Gate
      uses: sonarqube-quality-gate-action@master
      timeout-minutes: 5
      env:
        SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
```

## 📊 Local Development

### Pre-commit Quality Check

```bash
#!/bin/bash
# scripts/quality-check.sh

# Run all quality checks locally
./gradlew clean
./gradlew ktlintCheck
./gradlew detekt
./gradlew test jacocoTestReport
./gradlew jacocoTestCoverageVerification
./gradlew dependencyCheckAnalyze

echo "✅ All quality checks passed!"
```

### IDE Integration

#### IntelliJ IDEA Setup

1. **Install Plugins:**
   - SonarLint
   - Detekt
   - Kotlin

2. **Configure SonarLint:**

   ```
   File → Settings → Tools → SonarLint
   - Connect to SonarQube server
   - Bind project to SonarQube project
   - Enable automatic analysis
   ```

3. **Configure Detekt:**
   ```
   File → Settings → Tools → Detekt
   - Configuration file: config/detekt/detekt.yml
   - Enable auto-correction
   ```

#### VS Code Setup

```json
// .vscode/settings.json
{
  "sonarlint.connectedMode.project": {
    "connectionId": "gasolinera-sonar",
    "projectKey": "gasolinera-jsm-ultimate"
  },
  "kotlin.compiler.jvm.target": "17",
  "java.test.config": {
    "workingDirectory": "${workspaceFolder}"
  }
}
```

### Git Hooks

#### Pre-commit Hook

```bash
#!/bin/sh
# .git/hooks/pre-commit

echo "Running quality checks..."

# Run ktlint check
if ! ./gradlew ktlintCheck; then
    echo "❌ Kotlin lint check failed"
    echo "Run './gradlew ktlintFormat' to fix formatting issues"
    exit 1
fi

# Run detekt
if ! ./gradlew detekt; then
    echo "❌ Detekt analysis failed"
    echo "Check build/reports/detekt/detekt.html for details"
    exit 1
fi

# Run tests
if ! ./gradlew test; then
    echo "❌ Tests failed"
    exit 1
fi

echo "✅ All quality checks passed"
```

## 📈 Monitoring & Reporting

### SonarQube Dashboard

#### Key Metrics to Monitor

- **Coverage Trend:** Track coverage over time
- **Technical Debt:** Monitor debt accumulation
- **Code Smells:** Track code quality issues
- **Duplications:** Monitor code duplication
- **Security Hotspots:** Track security issues

#### Custom Widgets

```javascript
// Custom widget for business metrics
{
  "name": "Business Logic Coverage",
  "description": "Coverage specifically for business logic packages",
  "query": "coverage AND component:**/domain/** OR component:**/application/**"
}
```

### Automated Reports

#### Daily Quality Report

```bash
#!/bin/bash
# scripts/generate-daily-report.sh

DATE=$(date +%Y-%m-%d)
REPORT_FILE="reports/quality-report-$DATE.html"

# Generate SonarQube report
curl -u "$SONAR_TOKEN:" \
  "$SONAR_HOST_URL/api/measures/component?component=gasolinera-jsm-ultimate&metricKeys=coverage,duplicated_lines_density,maintainability_rating,reliability_rating,security_rating" \
  > "temp-sonar-data.json"

# Generate HTML report
cat > "$REPORT_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Daily Quality Report - $DATE</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .metric { background: #f5f5f5; padding: 10px; margin: 10px 0; border-radius: 5px; }
        .passed { border-left: 5px solid #28a745; }
        .failed { border-left: 5px solid #dc3545; }
    </style>
</head>
<body>
    <h1>Daily Quality Report - $DATE</h1>
    <!-- Report content generated from SonarQube data -->
</body>
</html>
EOF

echo "Report generated: $REPORT_FILE"
```

### Slack Notifications

```bash
# Send quality gate failure notification
send_slack_notification() {
    local message="$1"
    local webhook_url="$SLACK_WEBHOOK_URL"

    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"$message\"}" \
        "$webhook_url"
}

# Usage
if [ "$QUALITY_GATE_STATUS" = "FAILED" ]; then
    send_slack_notification "🚨 Quality Gate Failed for commit $COMMIT_SHA"
fi
```

## 🔧 Troubleshooting

### Common Issues

#### 1. SonarQube Connection Issues

```bash
# Test SonarQube connectivity
curl -u "$SONAR_TOKEN:" "$SONAR_HOST_URL/api/system/status"

# Check project exists
curl -u "$SONAR_TOKEN:" "$SONAR_HOST_URL/api/projects/search?projects=gasolinera-jsm-ultimate"
```

#### 2. Coverage Report Not Found

```bash
# Verify JaCoCo report generation
./gradlew test jacocoTestReport --info

# Check report location
find . -name "jacocoTestReport.xml" -type f
```

#### 3. Detekt Configuration Issues

```bash
# Validate Detekt configuration
./gradlew detekt --dry-run

# Check configuration file
./gradlew detektGenerateConfig
```

#### 4. OWASP Dependency Check Failures

```bash
# Update NVD database
./gradlew dependencyCheckUpdate

# Check suppression file
./gradlew dependencyCheckAnalyze --info
```

### Performance Optimization

#### 1. Parallel Execution

```kotlin
// gradle.properties
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.caching=true
```

#### 2. Build Cache

```bash
# Enable build cache
./gradlew build --build-cache

# Clean cache if needed
./gradlew cleanBuildCache
```

#### 3. Incremental Analysis

```properties
# sonar-project.properties
sonar.scm.provider=git
sonar.scm.forceReloadAll=false
```

## 📚 Best Practices

### 1. Quality Gate Strategy

- **Fail Fast:** Catch issues early in development
- **Incremental:** Focus on new/changed code quality
- **Balanced:** Don't make gates too strict initially
- **Evolving:** Gradually increase standards over time

### 2. Technical Debt Management

- **Regular Reviews:** Weekly technical debt review sessions
- **Prioritization:** Focus on high-impact, low-effort fixes
- **Tracking:** Monitor debt trends over time
- **Budgeting:** Allocate 20% of sprint capacity to debt reduction

### 3. Security Scanning

- **Automated:** Run security scans on every commit
- **Comprehensive:** Include dependencies, containers, and code
- **Actionable:** Provide clear remediation guidance
- **Continuous:** Monitor for new vulnerabilities

### 4. Team Adoption

- **Training:** Provide team training on quality tools
- **Documentation:** Maintain clear documentation
- **Support:** Provide support for quality tool issues
- **Culture:** Foster a quality-first culture

---

**🛡️ Esta configuración de quality gates asegura que Gasolinera JSM mantenga los más altos estándares de calidad de código, seguridad y mantenibilidad.**

_Última actualización: Enero 2024_
