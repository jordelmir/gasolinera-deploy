package com.gasolinerajsm.stationservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Employee entity representing a station employee
 */
@Entity
@Table(
    name = "employees",
    schema = "station_schema",
    indexes = [
        Index(name = "idx_employees_station_id", columnList = "station_id"),
        Index(name = "idx_employees_user_id", columnList = "user_id"),
        Index(name = "idx_employees_role", columnList = "role"),
        Index(name = "idx_employees_active", columnList = "is_active"),
        Index(name = "idx_employees_employee_number", columnList = "employee_number"),
        Index(name = "idx_employees_hire_date", columnList = "hire_date")
    ],
    uniqueConstraints = [
        UniqueConstraint(name = "uk_employees_user_station", columnNames = ["user_id", "station_id"]),
        UniqueConstraint(name = "uk_employees_number", columnNames = ["employee_number"])
    ]
)
data class Employee(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    @field:NotNull(message = "User ID is required")
    val userId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    @field:NotNull(message = "Station is required")
    val station: Station,

    @Column(name = "employee_number", unique = true, nullable = false, length = 20)
    @field:NotBlank(message = "Employee number is required")
    @field:Pattern(
        regexp = "^EMP[0-9]{6,15}$",
        message = "Employee number must start with 'EMP' followed by 6-15 digits"
    )
    val employeeNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    val role: EmployeeRole,

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    val employmentType: EmploymentType = EmploymentType.FULL_TIME,

    @Enumerated(EnumType.STRING)
    @Column(name = "shift", nullable = false, length = 20)
    val shift: WorkShift = WorkShift.DAY,

    @Column(name = "hire_date", nullable = false)
    @field:NotNull(message = "Hire date is required")
    @field:PastOrPresent(message = "Hire date cannot be in the future")
    val hireDate: LocalDate,

    @Column(name = "termination_date")
    val terminationDate: LocalDate? = null,

    @Column(name = "hourly_rate", precision = 8, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Hourly rate cannot be negative")
    @field:DecimalMax(value = "999999.99", message = "Hourly rate is too high")
    val hourlyRate: BigDecimal? = null,

    @Column(name = "monthly_salary", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Monthly salary cannot be negative")
    @field:DecimalMax(value = "99999999.99", message = "Monthly salary is too high")
    val monthlySalary: BigDecimal? = null,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "can_process_transactions", nullable = false)
    val canProcessTransactions: Boolean = true,

    @Column(name = "can_handle_cash", nullable = false)
    val canHandleCash: Boolean = false,

    @Column(name = "can_supervise", nullable = false)
    val canSupervise: Boolean = false,

    @Column(name = "emergency_contact_name", length = 200)
    @field:Size(max = 200, message = "Emergency contact name must not exceed 200 characters")
    val emergencyContactName: String? = null,

    @Column(name = "emergency_contact_phone", length = 20)
    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Emergency contact phone must be in valid international format"
    )
    val emergencyContactPhone: String? = null,

    @Column(name = "notes", length = 1000)
    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    val notes: String? = null,

    @Column(name = "training_completed", nullable = false)
    val trainingCompleted: Boolean = false,

    @Column(name = "training_completion_date")
    val trainingCompletionDate: LocalDate? = null,

    @Column(name = "certification_expiry_date")
    val certificationExpiryDate: LocalDate? = null,

    @Column(name = "performance_rating")
    @field:Min(value = 1, message = "Performance rating must be between 1 and 5")
    @field:Max(value = 5, message = "Performance rating must be between 1 and 5")
    val performanceRating: Int? = null,

    @Column(name = "last_performance_review")
    val lastPerformanceReview: LocalDate? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "created_by", length = 100)
    val createdBy: String? = null,

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null
) {

    /**
     * Check if employee is currently employed
     */
    fun isCurrentlyEmployed(): Boolean {
        return isActive && terminationDate == null
    }

    /**
     * Check if employee can work at the station
     */
    fun canWorkAtStation(): Boolean {
        return isActive && station.isOperational() && trainingCompleted
    }

    /**
     * Check if employee has management privileges
     */
    fun hasManagementPrivileges(): Boolean {
        return role.isManagement() || canSupervise
    }

    /**
     * Check if employee can handle financial transactions
     */
    fun canHandleFinancialTransactions(): Boolean {
        return canProcessTransactions && trainingCompleted
    }

    /**
     * Check if certification is expired or expiring soon
     */
    fun isCertificationExpiring(daysThreshold: Long = 30): Boolean {
        return certificationExpiryDate?.let { expiryDate ->
            val thresholdDate = LocalDate.now().plusDays(daysThreshold)
            expiryDate.isBefore(thresholdDate)
        } ?: false
    }

    /**
     * Check if performance review is due
     */
    fun isPerformanceReviewDue(monthsThreshold: Long = 12): Boolean {
        return lastPerformanceReview?.let { reviewDate ->
            val thresholdDate = LocalDate.now().minusMonths(monthsThreshold)
            reviewDate.isBefore(thresholdDate)
        } ?: true // If never reviewed, it's due
    }

    /**
     * Get years of service
     */
    fun getYearsOfService(): Int {
        val endDate = terminationDate ?: LocalDate.now()
        return endDate.year - hireDate.year
    }

    /**
     * Get employment duration in days
     */
    fun getEmploymentDurationDays(): Long {
        val endDate = terminationDate ?: LocalDate.now()
        return java.time.temporal.ChronoUnit.DAYS.between(hireDate, endDate)
    }

    /**
     * Activate employee
     */
    fun activate(updatedBy: String? = null): Employee {
        return this.copy(
            isActive = true,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Deactivate employee
     */
    fun deactivate(updatedBy: String? = null): Employee {
        return this.copy(
            isActive = false,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Terminate employment
     */
    fun terminate(terminationDate: LocalDate, updatedBy: String? = null): Employee {
        return this.copy(
            isActive = false,
            terminationDate = terminationDate,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Complete training
     */
    fun completeTraining(completionDate: LocalDate = LocalDate.now(), updatedBy: String? = null): Employee {
        return this.copy(
            trainingCompleted = true,
            trainingCompletionDate = completionDate,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update performance rating
     */
    fun updatePerformanceRating(
        rating: Int,
        reviewDate: LocalDate = LocalDate.now(),
        updatedBy: String? = null
    ): Employee {
        require(rating in 1..5) { "Performance rating must be between 1 and 5" }
        return this.copy(
            performanceRating = rating,
            lastPerformanceReview = reviewDate,
            updatedBy = updatedBy,
            updatedAt = LocalDateTime.now()
        )
    }

    override fun toString(): String {
        return "Employee(id=$id, employeeNumber='$employeeNumber', userId=$userId, role=$role, station=${station.name}, isActive=$isActive)"
    }
}

/**
 * Employee role enumeration
 */
enum class EmployeeRole(
    val displayName: String,
    val description: String,
    val permissions: Set<String>
) {
    CASHIER(
        "Cashier",
        "Handles customer transactions and payments",
        setOf("process_transactions", "handle_payments", "view_station_data")
    ),
    ATTENDANT(
        "Fuel Attendant",
        "Provides fuel service to customers",
        setOf("fuel_service", "customer_service", "view_station_data")
    ),
    SUPERVISOR(
        "Supervisor",
        "Supervises daily operations and staff",
        setOf(
            "process_transactions", "handle_payments", "fuel_service", "customer_service",
            "supervise_staff", "view_reports", "manage_shifts", "view_station_data"
        )
    ),
    ASSISTANT_MANAGER(
        "Assistant Manager",
        "Assists in station management and operations",
        setOf(
            "process_transactions", "handle_payments", "fuel_service", "customer_service",
            "supervise_staff", "view_reports", "manage_shifts", "manage_inventory",
            "handle_complaints", "view_station_data", "edit_station_data"
        )
    ),
    MANAGER(
        "Station Manager",
        "Manages all station operations and staff",
        setOf(
            "process_transactions", "handle_payments", "fuel_service", "customer_service",
            "supervise_staff", "view_reports", "manage_shifts", "manage_inventory",
            "handle_complaints", "manage_employees", "view_financials", "manage_station",
            "view_station_data", "edit_station_data", "delete_station_data"
        )
    ),
    MAINTENANCE(
        "Maintenance Technician",
        "Handles equipment maintenance and repairs",
        setOf("equipment_maintenance", "safety_checks", "view_station_data")
    ),
    SECURITY(
        "Security Guard",
        "Provides security services for the station",
        setOf("security_monitoring", "incident_reporting", "view_station_data")
    );

    /**
     * Check if this role has a specific permission
     */
    fun hasPermission(permission: String): Boolean {
        return permissions.contains(permission)
    }

    /**
     * Check if this role is a management role
     */
    fun isManagement(): Boolean {
        return this == MANAGER || this == ASSISTANT_MANAGER
    }

    /**
     * Check if this role can supervise others
     */
    fun canSupervise(): Boolean {
        return this == SUPERVISOR || isManagement()
    }

    /**
     * Check if this role can handle transactions
     */
    fun canHandleTransactions(): Boolean {
        return hasPermission("process_transactions")
    }

    /**
     * Check if this role can provide fuel service
     */
    fun canProvideFuelService(): Boolean {
        return hasPermission("fuel_service")
    }
}

/**
 * Employment type enumeration
 */
enum class EmploymentType(val displayName: String, val description: String) {
    FULL_TIME("Full Time", "Full-time employee working 40+ hours per week"),
    PART_TIME("Part Time", "Part-time employee working less than 40 hours per week"),
    CONTRACT("Contract", "Contract employee for specific duration"),
    TEMPORARY("Temporary", "Temporary employee for short-term needs"),
    INTERN("Intern", "Intern or trainee employee");

    fun isRegularEmployee(): Boolean {
        return this == FULL_TIME || this == PART_TIME
    }

    fun isTemporaryEmployee(): Boolean {
        return this == CONTRACT || this == TEMPORARY || this == INTERN
    }
}

/**
 * Work shift enumeration
 */
enum class WorkShift(
    val displayName: String,
    val startHour: Int,
    val endHour: Int,
    val description: String
) {
    DAY("Day Shift", 6, 14, "Morning to afternoon shift (6 AM - 2 PM)"),
    EVENING("Evening Shift", 14, 22, "Afternoon to evening shift (2 PM - 10 PM)"),
    NIGHT("Night Shift", 22, 6, "Night shift (10 PM - 6 AM)"),
    ROTATING("Rotating Shift", 0, 24, "Rotating between different shifts"),
    FLEXIBLE("Flexible Hours", 0, 24, "Flexible working hours");

    /**
     * Check if this is a night shift
     */
    fun isNightShift(): Boolean {
        return this == NIGHT
    }

    /**
     * Check if this shift covers specific hour
     */
    fun coversHour(hour: Int): Boolean {
        return when (this) {
            NIGHT -> hour >= startHour || hour < endHour
            ROTATING, FLEXIBLE -> true
            else -> hour >= startHour && hour < endHour
        }
    }

    /**
     * Get shift duration in hours
     */
    fun getDurationHours(): Int {
        return when (this) {
            NIGHT -> (24 - startHour) + endHour
            ROTATING, FLEXIBLE -> 8 // Default assumption
            else -> endHour - startHour
        }
    }
}