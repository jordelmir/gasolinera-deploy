package com.gasolinerajsm.performance.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.gasolinerajsm.performance.model.PerformanceMetrics
import org.springframework.stereotype.Component
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Utility class for generating performance test reports
 */
@Component
class PerformanceReporter {

    private val objectMapper = ObjectMapper().registerKotlinModule()
    private val reportDir = File("build/reports/performance")

    init {
        reportDir.mkdirs()
    }

    fun generateReport(metrics: PerformanceMetrics) {
        try {
            generateJsonReport(metrics)
            generateHtmlReport(metrics)
            generateCsvReport(metrics)
            generateSummaryReport(metrics)
        } catch (e: Exception) {
            println("Error generating performance report: ${e.message}")
        }
    }

    private fun generateJsonReport(metrics: PerformanceMetrics) {
        val jsonFile = File(reportDir, "${metrics.testName}_${getTimestamp()}.json")
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, metrics)
        println("JSON report generated: ${jsonFile.absolutePath}")
    }

    private fun generateHtmlReport(metrics: PerformanceMetrics) {
        val htmlFile = File(reportDir, "${metrics.testName}_${getTimestamp()}.html")
        val htmlContent = generateHtmlContent(metrics)
        htmlFile.writeText(htmlContent)
        println("HTML report generated: ${htmlFile.absolutePath}")
    }

    private fun generateCsvReport(metrics: PerformanceMetrics) {
        val csvFile = File(reportDir, "${metrics.testName}_${getTimestamp()}.csv")
        val csvContent = generateCsvContent(metrics)
        csvFile.writeText(csvContent)
        println("CSV report generated: ${csvFile.absolutePath}")
    }

    private fun generateSummaryReport(metrics: PerformanceMetrics) {
        val summaryFile = File(reportDir, "performance_summary.txt")
        val summaryContent = generateSummaryContent(metrics)

        // Append to summary file
        if (summaryFile.exists()) {
            summaryFile.appendText("\n\n$summaryContent")
        } else {
            summaryFile.writeText(summaryContent)
        }

        println("Summary report updated: ${summaryFile.absolutePath}")
    }

    private fun generateHtmlContent(metrics: PerformanceMetrics): String {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Performance Test Report - ${metrics.testName}</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
                    .metrics { display: flex; flex-wrap: wrap; gap: 20px; margin: 20px 0; }
                    .metric-card { background-color: #f9f9f9; padding: 15px; border-radius: 5px; min-width: 200px; }
                    .metric-title { font-weight: bold; color: #333; }
                    .metric-value { font-size: 1.2em; color: #007acc; }
                    .section { margin: 20px 0; }
                    .section-title { font-size: 1.3em; font-weight: bold; margin-bottom: 10px; }
                    table { border-collapse: collapse; width: 100%; }
                    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                    th { background-color: #f2f2f2; }
                    .success { color: #28a745; }
                    .warning { color: #ffc107; }
                    .error { color: #dc3545; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Performance Test Report</h1>
                    <h2>${metrics.testName}</h2>
                    <p><strong>Start Time:</strong> ${metrics.startTime}</p>
                    <p><strong>End Time:</strong> ${metrics.endTime}</p>
                    <p><strong>Duration:</strong> ${metrics.duration.seconds} seconds</p>
                </div>

                <div class="metrics">
                    <div class="metric-card">
                        <div class="metric-title">Total Requests</div>
                        <div class="metric-value">${metrics.totalRequests}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">Success Rate</div>
                        <div class="metric-value ${if (metrics.successRate >= 95) "success" else if (metrics.successRate >= 90) "warning" else "error"}">
                            ${String.format("%.2f", metrics.successRate)}%
                        </div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">Requests/Second</div>
                        <div class="metric-value">${String.format("%.2f", metrics.requestsPerSecond)}</div>
                    </div>
                    <div class="metric-card">
                        <div class="metric-title">Average Response Time</div>
                        <div class="metric-value">${String.format("%.2f", metrics.responseTimeMetrics.mean)}ms</div>
                    </div>
                </div>

                <div class="section">
                    <div class="section-title">Response Time Metrics</div>
                    <table>
                        <tr><th>Metric</th><th>Value (ms)</th></tr>
                        <tr><td>Minimum</td><td>${metrics.responseTimeMetrics.min}</td></tr>
                        <tr><td>Maximum</td><td>${metrics.responseTimeMetrics.max}</td></tr>
                        <tr><td>Mean</td><td>${String.format("%.2f", metrics.responseTimeMetrics.mean)}</td></tr>
                        <tr><td>Median</td><td>${metrics.responseTimeMetrics.median}</td></tr>
                        <tr><td>90th Percentile</td><td>${metrics.responseTimeMetrics.p90}</td></tr>
                        <tr><td>95th Percentile</td><td>${metrics.responseTimeMetrics.p95}</td></tr>
                        <tr><td>99th Percentile</td><td>${metrics.responseTimeMetrics.p99}</td></tr>
                        <tr><td>Standard Deviation</td><td>${String.format("%.2f", metrics.responseTimeMetrics.standardDeviation)}</td></tr>
                    </table>
                </div>

                <div class="section">
                    <div class="section-title">Resource Usage</div>
                    <table>
                        <tr><th>Resource</th><th>Average</th><th>Maximum</th></tr>
                        <tr><td>CPU Usage (%)</td><td>${String.format("%.2f", metrics.resourceMetrics.cpuUsage.averageUsage)}</td><td>${String.format("%.2f", metrics.resourceMetrics.cpuUsage.maxUsage)}</td></tr>
                        <tr><td>Memory Usage (%)</td><td>${String.format("%.2f", metrics.resourceMetrics.memoryUsage.averageUsage)}</td><td>${String.format("%.2f", metrics.resourceMetrics.memoryUsage.maxUsage)}</td></tr>
                        <tr><td>Heap Used (MB)</td><td colspan="2">${metrics.resourceMetrics.memoryUsage.heapUsed / 1024 / 1024}</td></tr>
                        <tr><td>GC Count</td><td colspan="2">${metrics.resourceMetrics.memoryUsage.gcCount}</td></tr>
                        <tr><td>GC Time (ms)</td><td colspan="2">${metrics.resourceMetrics.memoryUsage.gcTime}</td></tr>
                    </table>
                </div>

                <div class="section">
                    <div class="section-title">Database Metrics</div>
                    <table>
                        <tr><th>Metric</th><th>Value</th></tr>
                        <tr><td>Active Connections</td><td>${metrics.resourceMetrics.databaseMetrics.activeConnections}</td></tr>
                        <tr><td>Max Connections</td><td>${metrics.resourceMetrics.databaseMetrics.maxConnections}</td></tr>
                        <tr><td>Connection Pool Usage (%)</td><td>${String.format("%.2f", metrics.resourceMetrics.databaseMetrics.connectionPoolUsage)}</td></tr>
                        <tr><td>Average Query Time (ms)</td><td>${String.format("%.2f", metrics.resourceMetrics.databaseMetrics.averageQueryTime)}</td></tr>
                        <tr><td>Total Queries</td><td>${metrics.resourceMetrics.databaseMetrics.totalQueries}</td></tr>
                        <tr><td>Slow Queries</td><td>${metrics.resourceMetrics.databaseMetrics.slowQueries}</td></tr>
                    </table>
                </div>

                <div class="section">
                    <div class="section-title">Cache Metrics</div>
                    <table>
                        <tr><th>Metric</th><th>Value</th></tr>
                        <tr><td>Hit Rate (%)</td><td>${String.format("%.2f", metrics.resourceMetrics.cacheMetrics.hitRate)}</td></tr>
                        <tr><td>Miss Rate (%)</td><td>${String.format("%.2f", metrics.resourceMetrics.cacheMetrics.missRate)}</td></tr>
                        <tr><td>Eviction Count</td><td>${metrics.resourceMetrics.cacheMetrics.evictionCount}</td></tr>
                        <tr><td>Cache Size</td><td>${metrics.resourceMetrics.cacheMetrics.size}</td></tr>
                        <tr><td>Max Cache Size</td><td>${metrics.resourceMetrics.cacheMetrics.maxSize}</td></tr>
                    </table>
                </div>

                ${if (metrics.errorMetrics.errorsByType.isNotEmpty()) """
                <div class="section">
                    <div class="section-title">Error Analysis</div>
                    <table>
                        <tr><th>Error Type</th><th>Count</th></tr>
                        ${metrics.errorMetrics.errorsByType.entries.joinToString("") {
                            "<tr><td>${it.key}</td><td>${it.value}</td></tr>"
                        }}
                    </table>
                    <p><strong>Total Error Rate:</strong> ${String.format("%.2f", metrics.errorMetrics.errorRate)}%</p>
                </div>
                """ else ""}

                <div class="section">
                    <div class="section-title">Test Configuration</div>
                    <p>This report was generated automatically by the performance test framework.</p>
                    <p><strong>Generated at:</strong> ${java.time.LocalDateTime.now()}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateCsvContent(metrics: PerformanceMetrics): String {
        return """
            Test Name,Start Time,End Time,Duration (s),Total Requests,Total Errors,Success Rate (%),Requests/Second,Min Response (ms),Max Response (ms),Mean Response (ms),P95 Response (ms),P99 Response (ms),CPU Avg (%),Memory Avg (%),DB Pool Usage (%)
            ${metrics.testName},${metrics.startTime},${metrics.endTime},${metrics.duration.seconds},${metrics.totalRequests},${metrics.totalErrors},${String.format("%.2f", metrics.successRate)},${String.format("%.2f", metrics.requestsPerSecond)},${metrics.responseTimeMetrics.min},${metrics.responseTimeMetrics.max},${String.format("%.2f", metrics.responseTimeMetrics.mean)},${metrics.responseTimeMetrics.p95},${metrics.responseTimeMetrics.p99},${String.format("%.2f", metrics.resourceMetrics.cpuUsage.averageUsage)},${String.format("%.2f", metrics.resourceMetrics.memoryUsage.averageUsage)},${String.format("%.2f", metrics.resourceMetrics.databaseMetrics.connectionPoolUsage)}
        """.trimIndent()
    }

    private fun generateSummaryContent(metrics: PerformanceMetrics): String {
        return """
            ========================================
            Performance Test Summary: ${metrics.testName}
            ========================================
            Test Duration: ${metrics.duration.seconds} seconds
            Total Requests: ${metrics.totalRequests}
            Success Rate: ${String.format("%.2f", metrics.successRate)}%
            Throughput: ${String.format("%.2f", metrics.requestsPerSecond)} RPS

            Response Times:
            - Average: ${String.format("%.2f", metrics.responseTimeMetrics.mean)}ms
            - P95: ${metrics.responseTimeMetrics.p95}ms
            - P99: ${metrics.responseTimeMetrics.p99}ms

            Resource Usage:
            - CPU: ${String.format("%.2f", metrics.resourceMetrics.cpuUsage.averageUsage)}% avg, ${String.format("%.2f", metrics.resourceMetrics.cpuUsage.maxUsage)}% max
            - Memory: ${metrics.resourceMetrics.memoryUsage.heapUsed / 1024 / 1024}MB used
            - DB Pool: ${String.format("%.2f", metrics.resourceMetrics.databaseMetrics.connectionPoolUsage)}% usage

            ${if (metrics.errorMetrics.errorsByType.isNotEmpty())
                "Errors:\n${metrics.errorMetrics.errorsByType.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}"
            else "No errors detected"}

            Generated: ${java.time.LocalDateTime.now()}
        """.trimIndent()
    }

    private fun getTimestamp(): String {
        return java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    }
}