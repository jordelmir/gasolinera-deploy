package com.gasolinerajsm.stationservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gasolinerajsm.stationservice.model.Employee
import com.gasolinerajsm.stationservice.model.EmployeeRole
import com.gasolinerajsm.stationservice.model.EmployeePermission
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Request DTO for creating a new employee
 */
data class CreateEmployeeRequest(
    @field:NotNull(message = "User ID is required")
    @JsonProperty("user_id")
    val userId: Long,

    @field:NotNull(message = "Station ID is required")
    @JsonProperty("station_id")
    val stationId: Long,

    @JsonProperty("role")
    val role: EmployeeRole,

    @field:Size(max = 20, message = "Employee code must not exceed 20 characters")
    @JsonProperty("employee_code")
    val employeeCode: String? = null,

    @field:NotNull(message = "Hire date is required")
    @JsonProperty("hire_date")
    val hireDate: LocalDateTime,

    @field:DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    @JsonProperty("hourly_rate")
    val hourlyRate: BigDecimal? = null,

    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Boolean = true,

    @JsonProperty("can_manage_inventory")
    val canManageInventory: Boolean = false,

    @JsonProperty("can_view_reports")
    val canViewReports: Boolean = false,

    @field:Size(max = 200, message = "Emergency contact name must not exceed 200 characters")
    @JsonProperty("emergency_contact_name")
    val emergencyContactName: String? = null,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Emergency contact phone must be in valid international format"
    )
    @JsonProperty("emergency_contact_phone")
    val emergencyContactPhone: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @JsonProperty("notes")
    val notes: String? = null
)

/**
 * Request DTO for updating an employee
 */
data class UpdateEmployeeRequest(
    @JsonProperty("role")
    val role: EmployeeRole,

    @field:Size(max = 20, message = "Employee code must not exceed 20 characters")
    @JsonProperty("employee_code")
    val employeeCode: String? = null,

    @field:DecimalMin(value = "0.0", message = "Hourly rate must be positive")
    @JsonProperty("hourly_rate")
    val hourlyRate: BigDecimal? = null,

    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Boolean,

    @JsonProperty("can_manage_inventory")
    val canManageInventory: Boolean,

    @JsonProperty("can_view_reports")
    val canViewReports: Boolean,

    @field:Size(max = 200, message = "Emergency contact name must not exceed 200 characters")
    @JsonProperty("emergency_contact_name")
    val emergencyContactName: String? = null,

    @field:Pattern(
        regexp = "^\\+?[1-9]\\d{1,14}$",
        message = "Emergency contact phone must be in valid international format"
    )
    @JsonProperty("emergency_contact_phone")
    val emergencyContactPhone: String? = null,

    @field:Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @JsonProperty("notes")
    val notes: String? = null
)

/**
 * Response DTO for employee information
 */
data class EmployeeResponse(
    @JsonProperty("id")
    val id: Long,

    @JsonProperty("user_id")
    val userId: Long,

    @JsonProperty("station_id")
    val stationId: Long,

    @JsonProperty("station_name")
    val stationName: String,

    @JsonProperty("role")
    val role: EmployeeRole,

    @JsonProperty("role_display_name")
    val roleDisplayName: String,

    @JsonProperty("employee_code")
    val employeeCode: String?,

    @JsonProperty("hire_date")
    val hireDate: LocalDateTime,

    @JsonProperty("termination_date")
    val terminationDate: LocalDateTime?,

    @JsonProperty("hourly_rate")
    val hourlyRate: BigDecimal?,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("is_currently_employed")
    val isCurrentlyEmployed: Boolean,

    @JsonProperty("is_terminated")
    val isTerminated: Boolean,

    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Boolean,

    @JsonProperty("can_manage_inventory")
    val canManageInventory: Boolean,

    @JsonProperty("can_view_reports")
    val canViewReports: Boolean,

    @JsonProperty("can_perform_management_tasks")
    val canPerformManagementTasks: Boolean,

    @JsonProperty("can_process_transactions")
    val canProcessTransactions: Boolean,

    @JsonProperty("emergency_contact_name")
    val emergencyContactName: String?,

    @JsonProperty("emergency_contact_phone")
    val emergencyContactPhone: String?,

    @JsonProperty("notes")
    val notes: String?,

    @JsonProperty("tenure_months")
    val tenureMonths: Long,

    @JsonProperty("tenure_years")
    val tenureYears: Double,

    @JsonProperty("permissions")
    val permissions: List<String>,

    @JsonProperty("created_at")
    val createdAt: LocalDateTime,

    @JsonProperty("updated_at")
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromEmployee(employee: Employee): EmployeeResponse {
            return EmployeeResponse(
                id = employee.id,
                userId = employee.userId,
                stationId = employee.station.id,
                stationName = employee.station.name,
                role = employee.role,
                roleDisplayName = employee.role.displayName,
                employeeCode = employee.employeeCode,
                hireDate = employee.hireDate,
                terminationDate = employee.terminationDate,
                hourlyRate = employee.hourlyRate,
                isActive = employee.isActive,
                isCurrentlyEmployed = employee.isCurrentlyEmployed(),
                isTerminated = employee.isTerminated(),
                canProcessRedemptions = employee.canProcessRedemptions,
                canManageInventory = employee.canManageInventory,
                canViewReports = employee.canViewReports,
                canPerformManagementTasks = employee.canPerformManagementTasks(),
                canProcessTransactions = employee.canProcessTransactions(),
                emergencyContactName = employee.emergencyContactName,
                emergencyContactPhone = employee.emergencyContactPhone,
                notes = employee.notes,
                tenureMonths = employee.getTenureInMonths(),
                tenureYears = employee.getTenureInYears(),
                permissions = getPermissionsList(employee),
                createdAt = employee.createdAt,
                updatedAt = employee.updatedAt
            )
        }

        private fun getPermissionsList(employee: Employee): List<String> {
            return EmployeePermission.values().filter { permission ->
                employee.hasPermission(permission)
            }.map { it.displayName }
        }
    }
}

/**
 * Request DTO for updating employee permissions
 */
data class UpdateEmployeePermissionsRequest(
    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Boolean,

    @JsonProperty("can_manage_inventory")
    val canManageInventory: Boolean,

    @JsonProperty("can_view_reports")
    val canViewReports: Boolean
)

/**
 * Request DTO for updating employee role
 */
data class UpdateEmployeeRoleRequest(
    @JsonProperty("role")
    val role: EmployeeRole
)

/**
 * Request DTO for terminating an employee
 */
data class TerminateEmployeeRequest(
    @JsonProperty("termination_date")
    val terminationDate: LocalDateTime = LocalDateTime.now(),

    @field:Size(max = 500, message = "Termination reason must not exceed 500 characters")
    @JsonProperty("termination_reason")
    val terminationReason: String? = null
)

/**
 * Request DTO for employee search
 */
data class EmployeeSearchRequest(
    @JsonProperty("station_id")
    val stationId: Long? = null,

    @JsonProperty("user_id")
    val userId: Long? = null,

    @JsonProperty("role")
    val role: EmployeeRole? = null,

    @JsonProperty("is_active")
    val isActive: Boolean? = null,

    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Boolean? = null,

    @JsonProperty("employee_code")
    val employeeCode: String? = null,

    @JsonProperty("hire_date_from")
    val hireDateFrom: LocalDateTime? = null,

    @JsonProperty("hire_date_to")
    val hireDateTo: LocalDateTime? = null
)

/**
 * Response DTO for employee statistics
 */
data class EmployeeStatisticsResponse(
    @JsonProperty("total_employees")
    val totalEmployees: Long,

    @JsonProperty("active_employees")
    val activeEmployees: Long,

    @JsonProperty("terminated_employees")
    val terminatedEmployees: Long,

    @JsonProperty("managers")
    val managers: Long,

    @JsonProperty("supervisors")
    val supervisors: Long,

    @JsonProperty("cashiers")
    val cashiers: Long,

    @JsonProperty("can_process_redemptions")
    val canProcessRedemptions: Long,

    @JsonProperty("average_hourly_rate")
    val averageHourlyRate: BigDecimal?
)

/**
 * Response DTO for employee role information
 */
data class EmployeeRoleResponse(
    @JsonProperty("role")
    val role: EmployeeRole,

    @JsonProperty("display_name")
    val displayName: String,

    @JsonProperty("description")
    val description: String,

    @JsonProperty("level")
    val level: Int,

    @JsonProperty("is_management_role")
    val isManagementRole: Boolean,

    @JsonProperty("can_process_transactions")
    val canProcessTransactions: Boolean,

    @JsonProperty("can_manage_employees")
    val canManageEmployees: Boolean,

    @JsonProperty("can_manage_station")
    val canManageStation: Boolean,

    @JsonProperty("can_view_financial_reports")
    val canViewFinancialReports: Boolean,

    @JsonProperty("manageable_roles")
    val manageableRoles: List<EmployeeRole>
) {
    companion object {
        fun fromEmployeeRole(role: EmployeeRole): EmployeeRoleResponse {
            return EmployeeRoleResponse(
                role = role,
                displayName = role.displayName,
                description = role.description,
                level = role.level,
                isManagementRole = role.isManagementRole(),
                canProcessTransactions = role.canProcessTransactions(),
                canManageEmployees = role.canManageEmployees(),
                canManageStation = role.canManageStation(),
                canViewFinancialReports = role.canViewFinancialReports(),
                manageableRoles = role.getManageableRoles()
            )
        }
    }
}

/**
 * Response DTO for employee assignment to station
 */
data class EmployeeAssignmentResponse(
    @JsonProperty("employee_id")
    val employeeId: Long,

    @JsonProperty("user_id")
    val userId: Long,

    @JsonProperty("station_id")
    val stationId: Long,

    @JsonProperty("station_name")
    val stationName: String,

    @JsonProperty("role")
    val role: EmployeeRole,

    @JsonProperty("assignment_date")
    val assignmentDate: LocalDateTime,

    @JsonProperty("is_active")
    val isActive: Boolean,

    @JsonProperty("message")
    val message: String
) {
    companion object {
        fun fromEmployee(employee: Employee, message: String): EmployeeAssignmentResponse {
            return EmployeeAssignmentResponse(
                employeeId = employee.id,
                userId = employee.userId,
                stationId = employee.station.id,
                stationName = employee.station.name,
                role = employee.role,
                assignmentDate = employee.hireDate,
                isActive = employee.isActive,
                message = message
            )
        }
    }
}