package com.gasolinerajsm.stationservice.service

import com.gasolinerajsm.stationservice.model.Employee
import com.gasolinerajsm.stationservice.model.EmployeeRole
import com.gasolinerajsm.stationservice.model.EmploymentType
import com.gasolinerajsm.stationservice.model.WorkShift
import com.gasolinerajsm.stationservice.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Service for managing station employees
 */
@Service
@Transactional
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val stationService: StationService
) {

    private val logger = LoggerFactory.getLogger(EmployeeService::class.java)

    /**
     * Create a new employee
     */
    fun createEmployee(employee: Employee, createdBy: String? = null): Employee {
        return try {
            // Validate employee data
            validateEmployeeData(employee)

            // Check if employee number already exists
            if (employeeRepository.existsByEmployeeNumber(employee.employeeNumber)) {
                throw IllegalArgumentException("Employee number '${employee.employeeNumber}' already exists")
            }

            // Check if user is already an employee at this station
            if (employeeRepository.existsByUserIdAndStationId(employee.userId, employee.station.id)) {
                throw IllegalArgumentException("User is already an employee at this station")
            }

            // Verify station exists and is operational
            val station = stationService.getStationById(employee.station.id)
            if (!station.isOperational()) {
                throw IllegalArgumentException("Cannot assign employee to non-operational station")
            }

            val employeeToSave = employee.copy(
                createdBy = createdBy,
                updatedBy = createdBy
            )

            val savedEmployee = employeeRepository.save(employeeToSave)
            logger.info("Created new employee: {} for station: {}", savedEmployee.employeeNumber, station.name)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error creating employee: {}", e.message, e)
            throw EmployeeServiceException("Failed to create employee: ${e.message}", e)
        }
    }

    /**
     * Update an existing employee
     */
    fun updateEmployee(employeeId: Long, updatedEmployee: Employee, updatedBy: String? = null): Employee {
        return try {
            val existingEmployee = getEmployeeById(employeeId)

            // Check if employee number is being changed and if new number already exists
            if (existingEmployee.employeeNumber != updatedEmployee.employeeNumber &&
                employeeRepository.existsByEmployeeNumber(updatedEmployee.employeeNumber)) {
                throw IllegalArgumentException("Employee number '${updatedEmployee.employeeNumber}' already exists")
            }

            val employeeToSave = updatedEmployee.copy(
                id = employeeId,
                createdAt = existingEmployee.createdAt,
                createdBy = existingEmployee.createdBy,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )

            val savedEmployee = employeeRepository.save(employeeToSave)
            logger.info("Updated employee: {} (ID: {})", savedEmployee.employeeNumber, savedEmployee.id)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error updating employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to update employee: ${e.message}", e)
        }
    }

    /**
     * Get employee by ID
     */
    @Transactional(readOnly = true)
    fun getEmployeeById(employeeId: Long): Employee {
        return employeeRepository.findById(employeeId).orElseThrow {
            EmployeeNotFoundException("Employee not found with ID: $employeeId")
        }
    }

    /**
     * Get employee by employee number
     */
    @Transactional(readOnly = true)
    fun getEmployeeByNumber(employeeNumber: String): Employee? {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
    }

    /**
     * Get employee by user ID
     */
    @Transactional(readOnly = true)
    fun getEmployeeByUserId(userId: Long): Employee? {
        return employeeRepository.findByUserId(userId)
    }

    /**
     * Get employees by station
     */
    @Transactional(readOnly = true)
    fun getEmployeesByStation(stationId: Long, pageable: Pageable? = null): List<Employee> {
        return if (pageable != null) {
            employeeRepository.findByStationId(stationId, pageable).content
        } else {
            employeeRepository.findByStationId(stationId)
        }
    }

    /**
     * Get active employees by station
     */
    @Transactional(readOnly = true)
    fun getActiveEmployeesByStation(stationId: Long): List<Employee> {
        return employeeRepository.findByStationIdAndIsActive(stationId, true)
    }

    /**
     * Get employees by role
     */
    @Transactional(readOnly = true)
    fun getEmployeesByRole(role: EmployeeRole): List<Employee> {
        return employeeRepository.findByRole(role)
    }

    /**
     * Get employees by role and station
     */
    @Transactional(readOnly = true)
    fun getEmployeesByRoleAndStation(role: EmployeeRole, stationId: Long, activeOnly: Boolean = true): List<Employee> {
        return if (activeOnly) {
            employeeRepository.findByRoleAndStationIdAndIsActive(role, stationId, true)
        } else {
            employeeRepository.findByRoleAndStationId(role, stationId)
        }
    }

    /**
     * Get employees by employment type
     */
    @Transactional(readOnly = true)
    fun getEmployeesByEmploymentType(employmentType: EmploymentType): List<Employee> {
        return employeeRepository.findByEmploymentType(employmentType)
    }

    /**
     * Get employees by shift
     */
    @Transactional(readOnly = true)
    fun getEmployeesByShift(shift: WorkShift, stationId: Long? = null): List<Employee> {
        return if (stationId != null) {
            employeeRepository.findByShiftAndStationId(shift, stationId)
        } else {
            employeeRepository.findByShift(shift)
        }
    }

    /**
     * Search employees by multiple criteria
     */
    @Transactional(readOnly = true)
    fun searchEmployees(
        stationId: Long? = null,
        role: EmployeeRole? = null,
        employmentType: EmploymentType? = null,
        shift: WorkShift? = null,
        isActive: Boolean? = null,
        trainingCompleted: Boolean? = null,
        pageable: Pageable
    ): Page<Employee> {
        return employeeRepository.searchEmployees(
            stationId, role, employmentType, shift, isActive, trainingCompleted, pageable
        )
    }

    /**
     * Get employees with transaction processing capabilities
     */
    @Transactional(readOnly = true)
    fun getEmployeesWithTransactionCapabilities(stationId: Long? = null): List<Employee> {
        val employees = employeeRepository.findByCanProcessTransactionsAndIsActive(true, true)
        return if (stationId != null) {
            employees.filter { it.station.id == stationId }
        } else {
            employees
        }
    }

    /**
     * Get employees with cash handling capabilities
     */
    @Transactional(readOnly = true)
    fun getEmployeesWithCashHandling(stationId: Long? = null): List<Employee> {
        val employees = employeeRepository.findByCanHandleCashAndIsActive(true, true)
        return if (stationId != null) {
            employees.filter { it.station.id == stationId }
        } else {
            employees
        }
    }

    /**
     * Get employees with supervision capabilities
     */
    @Transactional(readOnly = true)
    fun getEmployeesWithSupervision(stationId: Long? = null): List<Employee> {
        val employees = employeeRepository.findByCanSuperviseAndIsActive(true, true)
        return if (stationId != null) {
            employees.filter { it.station.id == stationId }
        } else {
            employees
        }
    }

    /**
     * Get trained employees
     */
    @Transactional(readOnly = true)
    fun getTrainedEmployees(stationId: Long? = null): List<Employee> {
        return if (stationId != null) {
            employeeRepository.findByTrainingCompletedAndStationIdAndIsActive(true, stationId, true)
        } else {
            employeeRepository.findByTrainingCompletedAndIsActive(true, true)
        }
    }

    /**
     * Activate employee
     */
    fun activateEmployee(employeeId: Long, updatedBy: String? = null): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val activatedEmployee = employee.activate(updatedBy)
            val savedEmployee = employeeRepository.save(activatedEmployee)
            logger.info("Activated employee: {}", employeeId)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error activating employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to activate employee: ${e.message}", e)
        }
    }

    /**
     * Deactivate employee
     */
    fun deactivateEmployee(employeeId: Long, updatedBy: String? = null): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val deactivatedEmployee = employee.deactivate(updatedBy)
            val savedEmployee = employeeRepository.save(deactivatedEmployee)
            logger.info("Deactivated employee: {}", employeeId)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error deactivating employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to deactivate employee: ${e.message}", e)
        }
    }

    /**
     * Terminate employee
     */
    fun terminateEmployee(employeeId: Long, terminationDate: LocalDate, updatedBy: String? = null): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val terminatedEmployee = employee.terminate(terminationDate, updatedBy)
            val savedEmployee = employeeRepository.save(terminatedEmployee)
            logger.info("Terminated employee: {} on {}", employeeId, terminationDate)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error terminating employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to terminate employee: ${e.message}", e)
        }
    }

    /**
     * Complete employee training
     */
    fun completeEmployeeTraining(
        employeeId: Long,
        completionDate: LocalDate = LocalDate.now(),
        updatedBy: String? = null
    ): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val trainedEmployee = employee.completeTraining(completionDate, updatedBy)
            val savedEmployee = employeeRepository.save(trainedEmployee)
            logger.info("Completed training for employee: {} on {}", employeeId, completionDate)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error completing training for employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to complete employee training: ${e.message}", e)
        }
    }

    /**
     * Update employee performance rating
     */
    fun updatePerformanceRating(
        employeeId: Long,
        rating: Int,
        reviewDate: LocalDate = LocalDate.now(),
        updatedBy: String? = null
    ): Employee {
        return try {
            require(rating in 1..5) { "Performance rating must be between 1 and 5" }

            val employee = getEmployeeById(employeeId)
            val reviewedEmployee = employee.updatePerformanceRating(rating, reviewDate, updatedBy)
            val savedEmployee = employeeRepository.save(reviewedEmployee)
            logger.info("Updated performance rating for employee: {} to {}", employeeId, rating)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error updating performance rating for employee {}: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to update performance rating: ${e.message}", e)
        }
    }

    /**
     * Update employee role
     */
    fun updateEmployeeRole(employeeId: Long, newRole: EmployeeRole, updatedBy: String? = null): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val updatedEmployee = employee.copy(
                role = newRole,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )
            val savedEmployee = employeeRepository.save(updatedEmployee)
            logger.info("Updated employee {} role to: {}", employeeId, newRole)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error updating employee {} role: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to update employee role: ${e.message}", e)
        }
    }

    /**
     * Update employee shift
     */
    fun updateEmployeeShift(employeeId: Long, newShift: WorkShift, updatedBy: String? = null): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val updatedEmployee = employee.copy(
                shift = newShift,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )
            val savedEmployee = employeeRepository.save(updatedEmployee)
            logger.info("Updated employee {} shift to: {}", employeeId, newShift)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error updating employee {} shift: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to update employee shift: ${e.message}", e)
        }
    }

    /**
     * Update employee permissions
     */
    fun updateEmployeePermissions(
        employeeId: Long,
        canProcessTransactions: Boolean? = null,
        canHandleCash: Boolean? = null,
        canSupervise: Boolean? = null,
        updatedBy: String? = null
    ): Employee {
        return try {
            val employee = getEmployeeById(employeeId)
            val updatedEmployee = employee.copy(
                canProcessTransactions = canProcessTransactions ?: employee.canProcessTransactions,
                canHandleCash = canHandleCash ?: employee.canHandleCash,
                canSupervise = canSupervise ?: employee.canSupervise,
                updatedBy = updatedBy,
                updatedAt = LocalDateTime.now()
            )
            val savedEmployee = employeeRepository.save(updatedEmployee)
            logger.info("Updated employee {} permissions", employeeId)
            savedEmployee
        } catch (e: Exception) {
            logger.error("Error updating employee {} permissions: {}", employeeId, e.message, e)
            throw EmployeeServiceException("Failed to update employee permissions: ${e.message}", e)
        }
    }

    /**
     * Get employees with expiring certifications
     */
    @Transactional(readOnly = true)
    fun getEmployeesWithExpiringCertifications(daysThreshold: Long = 30): List<Employee> {
        val expiryDate = LocalDate.now().plusDays(daysThreshold)
        return employeeRepository.findEmployeesWithExpiringCertifications(expiryDate)
    }

    /**
     * Get employees due for performance review
     */
    @Transactional(readOnly = true)
    fun getEmployeesDueForReview(monthsThreshold: Long = 12): List<Employee> {
        val reviewDate = LocalDate.now().minusMonths(monthsThreshold)
        return employeeRepository.findEmployeesDueForReview(reviewDate)
    }

    /**
     * Get employees by performance rating
     */
    @Transactional(readOnly = true)
    fun getEmployeesByPerformanceRating(minRating: Int): List<Employee> {
        require(minRating in 1..5) { "Performance rating must be between 1 and 5" }
        return employeeRepository.findEmployeesWithMinimumRating(minRating)
    }

    /**
     * Get long-term employees
     */
    @Transactional(readOnly = true)
    fun getLongTermEmployees(yearsThreshold: Int = 5): List<Employee> {
        val days = yearsThreshold * 365L
        return employeeRepository.findLongTermEmployees(days)
    }

    /**
     * Get employee statistics
     */
    @Transactional(readOnly = true)
    fun getEmployeeStatistics(): EmployeeStatistics {
        return try {
            val stats = employeeRepository.getEmployeeStatistics()
            EmployeeStatistics(
                totalEmployees = (stats["totalEmployees"] as? Number)?.toLong() ?: 0L,
                activeEmployees = (stats["activeEmployees"] as? Number)?.toLong() ?: 0L,
                trainedEmployees = (stats["trainedEmployees"] as? Number)?.toLong() ?: 0L,
                managers = (stats["managers"] as? Number)?.toLong() ?: 0L,
                supervisors = (stats["supervisors"] as? Number)?.toLong() ?: 0L,
                fullTimeEmployees = (stats["fullTimeEmployees"] as? Number)?.toLong() ?: 0L,
                averagePerformanceRating = (stats["averagePerformanceRating"] as? Number)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            logger.error("Error getting employee statistics: {}", e.message, e)
            EmployeeStatistics()
        }
    }

    /**
     * Get employee statistics by station
     */
    @Transactional(readOnly = true)
    fun getEmployeeStatisticsByStation(stationId: Long): EmployeeStatistics {
        return try {
            val stats = employeeRepository.getEmployeeStatisticsByStation(stationId)
            EmployeeStatistics(
                totalEmployees = (stats["totalEmployees"] as? Number)?.toLong() ?: 0L,
                activeEmployees = (stats["activeEmployees"] as? Number)?.toLong() ?: 0L,
                trainedEmployees = (stats["trainedEmployees"] as? Number)?.toLong() ?: 0L,
                averagePerformanceRating = (stats["averagePerformanceRating"] as? Number)?.toDouble() ?: 0.0
            )
        } catch (e: Exception) {
            logger.error("Error getting employee statistics for station {}: {}", stationId, e.message, e)
            EmployeeStatistics()
        }
    }

    /**
     * Delete employee (soft delete by deactivating)
     */
    fun deleteEmployee(employeeId: Long, updatedBy: String? = null): Boolean {
        return try {
            deactivateEmployee(employeeId, updatedBy)
            logger.info("Soft deleted employee: {}", employeeId)
            true
        } catch (e: Exception) {
            logger.error("Error deleting employee {}: {}", employeeId, e.message, e)
            false
        }
    }

    /**
     * Validate employee data
     */
    private fun validateEmployeeData(employee: Employee) {
        require(employee.userId > 0) { "Valid user ID is required" }
        require(employee.employeeNumber.isNotBlank()) { "Employee number cannot be blank" }
        require(employee.employeeNumber.matches(Regex("^EMP[0-9]{6,15}$"))) {
            "Employee number must start with 'EMP' followed by 6-15 digits"
        }
        require(employee.hireDate <= LocalDate.now()) { "Hire date cannot be in the future" }

        employee.hourlyRate?.let { rate ->
            require(rate >= BigDecimal.ZERO) { "Hourly rate cannot be negative" }
        }

        employee.monthlySalary?.let { salary ->
            require(salary >= BigDecimal.ZERO) { "Monthly salary cannot be negative" }
        }

        employee.performanceRating?.let { rating ->
            require(rating in 1..5) { "Performance rating must be between 1 and 5" }
        }
    }
}

/**
 * Data class for employee statistics
 */
data class EmployeeStatistics(
    val totalEmployees: Long = 0L,
    val activeEmployees: Long = 0L,
    val trainedEmployees: Long = 0L,
    val managers: Long = 0L,
    val supervisors: Long = 0L,
    val fullTimeEmployees: Long = 0L,
    val averagePerformanceRating: Double = 0.0
)

/**
 * Custom exceptions for employee service
 */
class EmployeeServiceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class EmployeeNotFoundException(message: String) : RuntimeException(message)