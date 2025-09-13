package com.gasolinerajsm.stationservice.repository

import com.gasolinerajsm.stationservice.model.Employee
import com.gasolinerajsm.stationservice.model.EmployeeRole
import com.gasolinerajsm.stationservice.model.EmploymentType
import com.gasolinerajsm.stationservice.model.WorkShift
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface EmployeeRepository : JpaRepository<Employee, Long> {

    /**
     * Find employee by employee number
     */
    fun findByEmployeeNumber(employeeNumber: String): Employee?

    /**
     * Find employee by user ID
     */
    fun findByUserId(userId: Long): Employee?

    /**
     * Find employee by user ID and station ID
     */
    fun findByUserIdAndStationId(userId: Long, stationId: Long): Employee?

    /**
     * Check if employee number exists
     */
    fun existsByEmployeeNumber(employeeNumber: String): Boolean

    /**
     * Check if user is already an employee at any station
     */
    fun existsByUserId(userId: Long): Boolean

    /**
     * Check if user is already an employee at specific station
     */
    fun existsByUserIdAndStationId(userId: Long, stationId: Long): Boolean

    /**
     * Find employees by station ID
     */
    fun findByStationId(stationId: Long): List<Employee>

    /**
     * Find employees by station ID with pagination
     */
    fun findByStationId(stationId: Long, pageable: Pageable): Page<Employee>

    /**
     * Find active employees by station ID
     */
    fun findByStationIdAndIsActive(stationId: Long, isActive: Boolean): List<Employee>

    /**
     * Find employees by role
     */
    fun findByRole(role: EmployeeRole): List<Employee>

    /**
     * Find employees by role and station
     */
    fun findByRoleAndStationId(role: EmployeeRole, stationId: Long): List<Employee>

    /**
     * Find active employees by role and station
     */
    fun findByRoleAndStationIdAndIsActive(role: EmployeeRole, stationId: Long, isActive: Boolean): List<Employee>

    /**
     * Find employees by employment type
     */
    fun findByEmploymentType(employmentType: EmploymentType): List<Employee>

    /**
     * Find employees by shift
     */
    fun findByShift(shift: WorkShift): List<Employee>

    /**
     * Find employees by shift and station
     */
    fun findByShiftAndStationId(shift: WorkShift, stationId: Long): List<Employee>

    /**
     * Find active employees
     */
    fun findByIsActive(isActive: Boolean): List<Employee>

    /**
     * Find employees hired within date range
     */
    fun findByHireDateBetween(startDate: LocalDate, endDate: LocalDate): List<Employee>

    /**
     * Find employees hired on specific date
     */
    fun findByHireDate(hireDate: LocalDate): List<Employee>

    /**
     * Find terminated employees
     */
    fun findByTerminationDateIsNotNull(): List<Employee>

    /**
     * Find employees terminated within date range
     */
    fun findByTerminationDateBetween(startDate: LocalDate, endDate: LocalDate): List<Employee>

    /**
     * Find employees who can process transactions
     */
    fun findByCanProcessTransactionsAndIsActive(canProcessTransactions: Boolean, isActive: Boolean): List<Employee>

    /**
     * Find employees who can handle cash
     */
    fun findByCanHandleCashAndIsActive(canHandleCash: Boolean, isActive: Boolean): List<Employee>

    /**
     * Find employees who can supervise
     */
    fun findByCanSuperviseAndIsActive(canSupervise: Boolean, isActive: Boolean): List<Employee>

    /**
     * Find employees with completed training
     */
    fun findByTrainingCompletedAndIsActive(trainingCompleted: Boolean, isActive: Boolean): List<Employee>

    /**
     * Find employees with training completed by station
     */
    fun findByTrainingCompletedAndStationIdAndIsActive(
        trainingCompleted: Boolean,
        stationId: Long,
        isActive: Boolean
    ): List<Employee>

    /**
     * Find employees with expiring certifications
     */
    @Query("SELECT e FROM Employee e WHERE e.certificationExpiryDate <= :expiryDate AND e.isActive = true")
    fun findEmployeesWithExpiringCertifications(@Param("expiryDate") expiryDate: LocalDate): List<Employee>

    /**
     * Find employees due for performance review
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE (e.lastPerformanceReview IS NULL OR e.lastPerformanceReview <= :reviewDate)
        AND e.isActive = true
    """)
    fun findEmployeesDueForReview(@Param("reviewDate") reviewDate: LocalDate): List<Employee>

    /**
     * Find employees by performance rating
     */
    fun findByPerformanceRatingAndIsActive(performanceRating: Int, isActive: Boolean): List<Employee>

    /**
     * Find employees with performance rating above threshold
     */
    @Query("SELECT e FROM Employee e WHERE e.performanceRating >= :minRating AND e.isActive = true")
    fun findEmployeesWithMinimumRating(@Param("minRating") minRating: Int): List<Employee>

    /**
     * Search employees by multiple criteria
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE (:stationId IS NULL OR e.station.id = :stationId)
        AND (:role IS NULL OR e.role = :role)
        AND (:employmentType IS NULL OR e.employmentType = :employmentType)
        AND (:shift IS NULL OR e.shift = :shift)
        AND (:isActive IS NULL OR e.isActive = :isActive)
        AND (:trainingCompleted IS NULL OR e.trainingCompleted = :trainingCompleted)
    """)
    fun searchEmployees(
        @Param("stationId") stationId: Long?,
        @Param("role") role: EmployeeRole?,
        @Param("employmentType") employmentType: EmploymentType?,
        @Param("shift") shift: WorkShift?,
        @Param("isActive") isActive: Boolean?,
        @Param("trainingCompleted") trainingCompleted: Boolean?,
        pageable: Pageable
    ): Page<Employee>

    /**
     * Count employees by station
     */
    fun countByStationId(stationId: Long): Long

    /**
     * Count active employees by station
     */
    fun countByStationIdAndIsActive(stationId: Long, isActive: Boolean): Long

    /**
     * Count employees by role
     */
    fun countByRole(role: EmployeeRole): Long

    /**
     * Count employees by role and station
     */
    fun countByRoleAndStationId(role: EmployeeRole, stationId: Long): Long

    /**
     * Count employees by employment type
     */
    fun countByEmploymentType(employmentType: EmploymentType): Long

    /**
     * Count employees by shift
     */
    fun countByShift(shift: WorkShift): Long

    /**
     * Count employees with completed training
     */
    fun countByTrainingCompletedAndIsActive(trainingCompleted: Boolean, isActive: Boolean): Long

    /**
     * Get employees hired in specific year
     */
    @Query("SELECT e FROM Employee e WHERE YEAR(e.hireDate) = :year")
    fun findEmployeesHiredInYear(@Param("year") year: Int): List<Employee>

    /**
     * Get employees with years of service
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE YEAR(CURRENT_DATE) - YEAR(e.hireDate) >= :years
        AND e.isActive = true
    """)
    fun findEmployeesWithMinimumYearsOfService(@Param("years") years: Int): List<Employee>

    /**
     * Update employee status
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.isActive = :isActive, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun updateEmployeeStatus(
        @Param("employeeId") employeeId: Long,
        @Param("isActive") isActive: Boolean,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update employee role
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.role = :role, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun updateEmployeeRole(
        @Param("employeeId") employeeId: Long,
        @Param("role") role: EmployeeRole,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update employee shift
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.shift = :shift, e.updatedAt = :updatedAt, e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun updateEmployeeShift(
        @Param("employeeId") employeeId: Long,
        @Param("shift") shift: WorkShift,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update employee permissions
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.canProcessTransactions = :canProcessTransactions,
            e.canHandleCash = :canHandleCash,
            e.canSupervise = :canSupervise,
            e.updatedAt = :updatedAt,
            e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun updateEmployeePermissions(
        @Param("employeeId") employeeId: Long,
        @Param("canProcessTransactions") canProcessTransactions: Boolean,
        @Param("canHandleCash") canHandleCash: Boolean,
        @Param("canSupervise") canSupervise: Boolean,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Complete employee training
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.trainingCompleted = true,
            e.trainingCompletionDate = :completionDate,
            e.updatedAt = :updatedAt,
            e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun completeEmployeeTraining(
        @Param("employeeId") employeeId: Long,
        @Param("completionDate") completionDate: LocalDate,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Update performance rating
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.performanceRating = :rating,
            e.lastPerformanceReview = :reviewDate,
            e.updatedAt = :updatedAt,
            e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun updatePerformanceRating(
        @Param("employeeId") employeeId: Long,
        @Param("rating") rating: Int,
        @Param("reviewDate") reviewDate: LocalDate,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Terminate employee
     */
    @Modifying
    @Query("""
        UPDATE Employee e
        SET e.isActive = false,
            e.terminationDate = :terminationDate,
            e.updatedAt = :updatedAt,
            e.updatedBy = :updatedBy
        WHERE e.id = :employeeId
    """)
    fun terminateEmployee(
        @Param("employeeId") employeeId: Long,
        @Param("terminationDate") terminationDate: LocalDate,
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("updatedBy") updatedBy: String?
    ): Int

    /**
     * Get employee statistics
     */
    @Query("""
        SELECT
            COUNT(e) as totalEmployees,
            COUNT(CASE WHEN e.isActive = true THEN 1 END) as activeEmployees,
            COUNT(CASE WHEN e.trainingCompleted = true THEN 1 END) as trainedEmployees,
            COUNT(CASE WHEN e.role = 'MANAGER' THEN 1 END) as managers,
            COUNT(CASE WHEN e.role = 'SUPERVISOR' THEN 1 END) as supervisors,
            COUNT(CASE WHEN e.employmentType = 'FULL_TIME' THEN 1 END) as fullTimeEmployees,
            AVG(e.performanceRating) as averagePerformanceRating
        FROM Employee e
    """)
    fun getEmployeeStatistics(): Map<String, Any>

    /**
     * Get employee statistics by station
     */
    @Query("""
        SELECT
            COUNT(e) as totalEmployees,
            COUNT(CASE WHEN e.isActive = true THEN 1 END) as activeEmployees,
            COUNT(CASE WHEN e.trainingCompleted = true THEN 1 END) as trainedEmployees,
            AVG(e.performanceRating) as averagePerformanceRating
        FROM Employee e
        WHERE e.station.id = :stationId
    """)
    fun getEmployeeStatisticsByStation(@Param("stationId") stationId: Long): Map<String, Any>

    /**
     * Find employees with upcoming certification expiry
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE e.certificationExpiryDate BETWEEN :startDate AND :endDate
        AND e.isActive = true
        ORDER BY e.certificationExpiryDate ASC
    """)
    fun findEmployeesWithUpcomingCertificationExpiry(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<Employee>

    /**
     * Find long-term employees (by years of service)
     */
    @Query("""
        SELECT e FROM Employee e
        WHERE DATEDIFF(CURRENT_DATE, e.hireDate) >= :days
        AND e.isActive = true
        ORDER BY e.hireDate ASC
    """)
    fun findLongTermEmployees(@Param("days") days: Long): List<Employee>

    /**
     * Find employees by station and multiple roles
     */
    @Query("SELECT e FROM Employee e WHERE e.station.id = :stationId AND e.role IN :roles AND e.isActive = true")
    fun findByStationIdAndRoleIn(
        @Param("stationId") stationId: Long,
        @Param("roles") roles: List<EmployeeRole>
    ): List<Employee>
}