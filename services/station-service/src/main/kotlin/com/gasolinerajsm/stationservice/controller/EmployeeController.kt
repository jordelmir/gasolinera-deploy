package com.gasolinerajsm.stationservice.controller

import com.gasolinerajsm.stationservice.dto.*
import com.gasolinerajsm.stationservice.model.EmployeeRole
import com.gasolinerajsm.stationservice.service.EmployeeService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * REST Controller for Employee management operations
 */
@RestController
@RequestMapping("/api/v1/employees")
@CrossOrigin(origins = ["*"])
class EmployeeController(
    private val employeeService: EmployeeService
) {

    /**
     * Create a new employee (assign user to station)
     */
    @PostMapping
    fun createEmployee(@Valid @RequestBody request: CreateEmployeeRequest): ResponseEntity<EmployeeAssignmentResponse> {
        val employee = employeeService.createEmployee(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(employee)
    }

    /**
     * Get employee by ID
     */
    @GetMapping("/{id}")
    fun getEmployeeById(@PathVariable id: Long): ResponseEntity<EmployeeResponse> {
        val employee = employeeService.getEmployeeById(id)
        return ResponseEntity.ok(employee)
    }

    /**
     * Get employee by user ID
     */
    @GetMapping("/user/{userId}")
    fun getEmployeeByUserId(@PathVariable userId: Long): ResponseEntity<EmployeeResponse> {
        val employee = employeeService.getEmployeeByUserId(userId)
        return ResponseEntity.ok(employee)
    }

    /**
     * Get all employees with pagination
     */
    @GetMapping
    fun getAllEmployees(pageable: Pageable): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getAllEmployees(pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Update employee information
     */
    @PutMapping("/{id}")
    fun updateEmployee(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEmployeeRequest
    ): ResponseEntity<EmployeeResponse> {
        val updatedEmployee = employeeService.updateEmployee(id, request)
        return ResponseEntity.ok(updatedEmployee)
    }

    /**
     * Delete employee (soft delete - set to inactive)
     */
    @DeleteMapping("/{id}")
    fun deleteEmployee(@PathVariable id: Long): ResponseEntity<Void> {
        employeeService.deleteEmployee(id)
        return ResponseEntity.noContent().build()
    }

    /**
     * Activate employee
     */
    @PostMapping("/{id}/activate")
    fun activateEmployee(@PathVariable id: Long): ResponseEntity<EmployeeResponse> {
        val activatedEmployee = employeeService.activateEmployee(id)
        return ResponseEntity.ok(activatedEmployee)
    }

    /**
     * Deactivate employee
     */
    @PostMapping("/{id}/deactivate")
    fun deactivateEmployee(@PathVariable id: Long): ResponseEntity<EmployeeResponse> {
        val deactivatedEmployee = employeeService.deactivateEmployee(id)
        return ResponseEntity.ok(deactivatedEmployee)
    }

    /**
     * Terminate employee
     */
    @PostMapping("/{id}/terminate")
    fun terminateEmployee(
        @PathVariable id: Long,
        @Valid @RequestBody request: TerminateEmployeeRequest
    ): ResponseEntity<EmployeeResponse> {
        val terminatedEmployee = employeeService.terminateEmployee(id, request)
        return ResponseEntity.ok(terminatedEmployee)
    }

    /**
     * Update employee role
     */
    @PatchMapping("/{id}/role")
    fun updateEmployeeRole(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEmployeeRoleRequest
    ): ResponseEntity<EmployeeResponse> {
        val updatedEmployee = employeeService.updateEmployeeRole(id, request.role)
        return ResponseEntity.ok(updatedEmployee)
    }

    /**
     * Update employee permissions
     */
    @PatchMapping("/{id}/permissions")
    fun updateEmployeePermissions(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEmployeePermissionsRequest
    ): ResponseEntity<EmployeeResponse> {
        val updatedEmployee = employeeService.updateEmployeePermissions(id, request)
        return ResponseEntity.ok(updatedEmployee)
    }

    /**
     * Update employee hourly rate
     */
    @PatchMapping("/{id}/hourly-rate")
    fun updateEmployeeHourlyRate(
        @PathVariable id: Long,
        @RequestParam hourlyRate: BigDecimal
    ): ResponseEntity<EmployeeResponse> {
        val updatedEmployee = employeeService.updateEmployeeHourlyRate(id, hourlyRate)
        return ResponseEntity.ok(updatedEmployee)
    }

    /**
     * Get employees by station
     */
    @GetMapping("/station/{stationId}")
    fun getEmployeesByStation(
        @PathVariable stationId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesByStation(stationId, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get active employees by station
     */
    @GetMapping("/station/{stationId}/active")
    fun getActiveEmployeesByStation(
        @PathVariable stationId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getActiveEmployeesByStation(stationId, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees by role
     */
    @GetMapping("/role/{role}")
    fun getEmployeesByRole(
        @PathVariable role: EmployeeRole,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesByRole(role, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees by role and station
     */
    @GetMapping("/station/{stationId}/role/{role}")
    fun getEmployeesByRoleAndStation(
        @PathVariable stationId: Long,
        @PathVariable role: EmployeeRole,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesByRoleAndStation(stationId, role, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees who can process redemptions
     */
    @GetMapping("/can-process-redemptions")
    fun getEmployeesWhoCanProcessRedemptions(pageable: Pageable): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesWhoCanProcessRedemptions(pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees who can process redemptions by station
     */
    @GetMapping("/station/{stationId}/can-process-redemptions")
    fun getEmployeesWhoCanProcessRedemptionsByStation(
        @PathVariable stationId: Long,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesWhoCanProcessRedemptionsByStation(stationId, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get terminated employees
     */
    @GetMapping("/terminated")
    fun getTerminatedEmployees(pageable: Pageable): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getTerminatedEmployees(pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get long-term employees (hired before specific date)
     */
    @GetMapping("/long-term")
    fun getLongTermEmployees(
        @RequestParam yearsAgo: Int,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getLongTermEmployees(yearsAgo, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Search employees by criteria
     */
    @PostMapping("/search")
    fun searchEmployees(
        @Valid @RequestBody request: EmployeeSearchRequest,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.searchEmployees(request, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employee by employee code
     */
    @GetMapping("/code/{employeeCode}")
    fun getEmployeeByCode(@PathVariable employeeCode: String): ResponseEntity<EmployeeResponse> {
        val employee = employeeService.getEmployeeByCode(employeeCode)
        return ResponseEntity.ok(employee)
    }

    /**
     * Get employees hired within date range
     */
    @GetMapping("/hired-between")
    fun getEmployeesHiredBetween(
        @RequestParam startDate: LocalDateTime,
        @RequestParam endDate: LocalDateTime,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesHiredBetween(startDate, endDate, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees with hourly rate range
     */
    @GetMapping("/hourly-rate-range")
    fun getEmployeesWithHourlyRateRange(
        @RequestParam minRate: BigDecimal,
        @RequestParam maxRate: BigDecimal,
        pageable: Pageable
    ): ResponseEntity<Page<EmployeeResponse>> {
        val employees = employeeService.getEmployeesWithHourlyRateRange(minRate, maxRate, pageable)
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employee statistics
     */
    @GetMapping("/statistics")
    fun getEmployeeStatistics(): ResponseEntity<EmployeeStatisticsResponse> {
        val statistics = employeeService.getEmployeeStatistics()
        return ResponseEntity.ok(statistics)
    }

    /**
     * Get employee statistics by station
     */
    @GetMapping("/station/{stationId}/statistics")
    fun getEmployeeStatisticsByStation(@PathVariable stationId: Long): ResponseEntity<EmployeeStatisticsResponse> {
        val statistics = employeeService.getEmployeeStatisticsByStation(stationId)
        return ResponseEntity.ok(statistics)
    }

    /**
     * Get employees without emergency contact
     */
    @GetMapping("/no-emergency-contact")
    fun getEmployeesWithoutEmergencyContact(): ResponseEntity<List<EmployeeResponse>> {
        val employees = employeeService.getEmployeesWithoutEmergencyContact()
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees with duplicate codes
     */
    @GetMapping("/duplicate-codes")
    fun getEmployeesWithDuplicateCodes(): ResponseEntity<List<EmployeeResponse>> {
        val employees = employeeService.getEmployeesWithDuplicateCodes()
        return ResponseEntity.ok(employees)
    }

    /**
     * Get employees needing performance review
     */
    @GetMapping("/performance-review-needed")
    fun getEmployeesNeedingPerformanceReview(): ResponseEntity<List<EmployeeResponse>> {
        val employees = employeeService.getEmployeesNeedingPerformanceReview()
        return ResponseEntity.ok(employees)
    }

    /**
     * Get all employee roles with their capabilities
     */
    @GetMapping("/roles")
    fun getAllEmployeeRoles(): ResponseEntity<List<EmployeeRoleResponse>> {
        val roles = employeeService.getAllEmployeeRoles()
        return ResponseEntity.ok(roles)
    }

    /**
     * Get employee role information
     */
    @GetMapping("/roles/{role}")
    fun getEmployeeRoleInfo(@PathVariable role: EmployeeRole): ResponseEntity<EmployeeRoleResponse> {
        val roleInfo = employeeService.getEmployeeRoleInfo(role)
        return ResponseEntity.ok(roleInfo)
    }

    /**
     * Check if user is already an employee
     */
    @GetMapping("/user/{userId}/exists")
    fun checkIfUserIsEmployee(@PathVariable userId: Long): ResponseEntity<Map<String, Boolean>> {
        val exists = employeeService.isUserAlreadyEmployee(userId)
        val response = mapOf("is_employee" to exists)
        return ResponseEntity.ok(response)
    }

    /**
     * Check if employee code exists
     */
    @GetMapping("/code/{employeeCode}/exists")
    fun checkIfEmployeeCodeExists(@PathVariable employeeCode: String): ResponseEntity<Map<String, Boolean>> {
        val exists = employeeService.employeeCodeExists(employeeCode)
        val response = mapOf("code_exists" to exists)
        return ResponseEntity.ok(response)
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        val response = mapOf(
            "status" to "UP",
            "service" to "Employee Service",
            "timestamp" to java.time.Instant.now().toString()
        )
        return ResponseEntity.ok(response)
    }
}