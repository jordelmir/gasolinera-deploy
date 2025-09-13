package com.gasolinerajsm.stationservice.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gasolinerajsm.stationservice.dto.*
import com.gasolinerajsm.stationservice.model.*
import com.gasolinerajsm.stationservice.service.EmployeeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(EmployeeController::class)
@DisplayName("Employee Controller Tests")
class EmployeeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var employeeService: EmployeeService

    private lateinit var testStation: Station
    private lateinit var testEmployee: Employee
    private lateinit var testEmployeeResponse: EmployeeResponse
    private lateinit var createEmployeeRequest: CreateEmployeeRequest

    @BeforeEach
    fun setUp() {
        testStation = Station(
            id = 1L,
            name = "Test Station",
            address = "123 Test Street",
            latitude = BigDecimal("40.7128"),
            longitude = BigDecimal("-74.0060"),
            phoneNumber = "+1234567890"
        )

        testEmployee = Employee(
            id = 1L,
            userId = 123L,
            station = testStation,
            role = EmployeeRole.CASHIER,
            hireDate = LocalDateTime.now().minusMonths(6),
            hourlyRate = BigDecimal("15.50"),
            isActive = true
        )

        testEmployeeResponse = EmployeeResponse.fromEmployee(testEmployee)

        createEmployeeRequest = CreateEmployeeRequest(
            userId = 456L,
            stationId = 1L,
            role = EmployeeRole.SUPERVISOR,
            hireDate = LocalDateTime.now(),
            hourlyRate = BigDecimal("18.00"),
            canProcessRedemptions = true,
            canManageInventory = false,
            canViewReports = true
        )
    }

    @Nested
    @DisplayName("Employee CRUD Operations")
    inner class EmployeeCrudOperations {

        @Test
        @DisplayName("Should create employee successfully")
        fun shouldCreateEmployeeSuccessfully() {
            // Given
            val assignmentResponse = EmployeeAssignmentResponse.fromEmployee(
                testEmployee,
                "Employee successfully assigned to station"
            )
            whenever(employeeService.createEmployee(any<CreateEmployeeRequest>())).thenReturn(assignmentResponse)

            // When & Then
            mockMvc.perform(
                post("/api/v1/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createEmployeeRequest))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.user_id").value(testEmployee.userId))
                .andExpect(jsonPath("$.station_name").value(testStation.name))
                .andExpect(jsonPath("$.role").value(testEmployee.role.name))

            verify(employeeService).createEmployee(any<CreateEmployeeRequest>())
        }

        @Test
        @DisplayName("Should return validation error for invalid employee data")
        fun shouldReturnValidationErrorForInvalidEmployeeData() {
            // Given
            val invalidRequest = createEmployeeRequest.copy(
                userId = -1L, // Invalid: negative user ID
                hourlyRate = BigDecimal("-5.00") // Invalid: negative rate
            )

            // When & Then
            mockMvc.perform(
                post("/api/v1/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest))
            )
                .andExpect(status().isBadRequest)

            verify(employeeService, never()).createEmployee(any())
        }

        @Test
        @DisplayName("Should get employee by ID successfully")
        fun shouldGetEmployeeByIdSuccessfully() {
            // Given
            whenever(employeeService.getEmployeeById(1L)).thenReturn(testEmployeeResponse)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.user_id").value(testEmployee.userId))
                .andExpect(jsonPath("$.role").value(testEmployee.role.name))

            verify(employeeService).getEmployeeById(1L)
        }

        @Test
        @DisplayName("Should get employee by user ID successfully")
        fun shouldGetEmployeeByUserIdSuccessfully() {
            // Given
            whenever(employeeService.getEmployeeByUserId(123L)).thenReturn(testEmployeeResponse)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/user/123"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.user_id").value(123))

            verify(employeeService).getEmployeeByUserId(123L)
        }

        @Test
        @DisplayName("Should get all employees with pagination")
        fun shouldGetAllEmployeesWithPagination() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val employeesPage = PageImpl(listOf(testEmployeeResponse), pageable, 1)
            whenever(employeeService.getAllEmployees(any())).thenReturn(employeesPage)

            // When & Then
            mockMvc.perform(
                get("/api/v1/employees")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content").isArray)
                .andExpect(jsonPath("$.content[0].user_id").value(testEmployee.userId))
                .andExpect(jsonPath("$.totalElements").value(1))

            verify(employeeService).getAllEmployees(any())
        }

        @Test
        @DisplayName("Should update employee successfully")
        fun shouldUpdateEmployeeSuccessfully() {
            // Given
            val updateRequest = UpdateEmployeeRequest(
                role = EmployeeRole.SUPERVISOR,
                hourlyRate = BigDecimal("20.00"),
                canProcessRedemptions = true,
                canManageInventory = true,
                canViewReports = true
            )
            val updatedResponse = testEmployeeResponse.copy(
                role = EmployeeRole.SUPERVISOR,
                hourlyRate = BigDecimal("20.00")
            )
            whenever(employeeService.updateEmployee(eq(1L), any<UpdateEmployeeRequest>())).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                put("/api/v1/employees/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.role").value("SUPERVISOR"))
                .andExpect(jsonPath("$.hourly_rate").value(20.00))

            verify(employeeService).updateEmployee(eq(1L), any<UpdateEmployeeRequest>())
        }

        @Test
        @DisplayName("Should delete employee successfully")
        fun shouldDeleteEmployeeSuccessfully() {
            // Given
            doNothing().whenever(employeeService).deleteEmployee(1L)

            // When & Then
            mockMvc.perform(delete("/api/v1/employees/1"))
                .andExpect(status().isNoContent)

            verify(employeeService).deleteEmployee(1L)
        }
    }

    @Nested
    @DisplayName("Employee Status Management")
    inner class EmployeeStatusManagement {

        @Test
        @DisplayName("Should activate employee successfully")
        fun shouldActivateEmployeeSuccessfully() {
            // Given
            val activatedResponse = testEmployeeResponse.copy(isActive = true)
            whenever(employeeService.activateEmployee(1L)).thenReturn(activatedResponse)

            // When & Then
            mockMvc.perform(post("/api/v1/employees/1/activate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.is_active").value(true))

            verify(employeeService).activateEmployee(1L)
        }

        @Test
        @DisplayName("Should deactivate employee successfully")
        fun shouldDeactivateEmployeeSuccessfully() {
            // Given
            val deactivatedResponse = testEmployeeResponse.copy(isActive = false)
            whenever(employeeService.deactivateEmployee(1L)).thenReturn(deactivatedResponse)

            // When & Then
            mockMvc.perform(post("/api/v1/employees/1/deactivate"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.is_active").value(false))

            verify(employeeService).deactivateEmployee(1L)
        }

        @Test
        @DisplayName("Should terminate employee successfully")
        fun shouldTerminateEmployeeSuccessfully() {
            // Given
            val terminationRequest = TerminateEmployeeRequest(
                terminationDate = LocalDateTime.now(),
                terminationReason = "Resignation"
            )
            val terminatedResponse = testEmployeeResponse.copy(
                isActive = false,
                terminationDate = terminationRequest.terminationDate
            )
            whenever(employeeService.terminateEmployee(eq(1L), any<TerminateEmployeeRequest>()))
                .thenReturn(terminatedResponse)

            // When & Then
            mockMvc.perform(
                post("/api/v1/employees/1/terminate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(terminationRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.is_active").value(false))
                .andExpect(jsonPath("$.termination_date").exists())

            verify(employeeService).terminateEmployee(eq(1L), any<TerminateEmployeeRequest>())
        }
    }

    @Nested
    @DisplayName("Employee Role and Permissions Management")
    inner class EmployeeRoleAndPermissionsManagement {

        @Test
        @DisplayName("Should update employee role successfully")
        fun shouldUpdateEmployeeRoleSuccessfully() {
            // Given
            val roleRequest = UpdateEmployeeRoleRequest(EmployeeRole.MANAGER)
            val updatedResponse = testEmployeeResponse.copy(role = EmployeeRole.MANAGER)
            whenever(employeeService.updateEmployeeRole(1L, EmployeeRole.MANAGER)).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/employees/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(roleRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.role").value("MANAGER"))

            verify(employeeService).updateEmployeeRole(1L, EmployeeRole.MANAGER)
        }

        @Test
        @DisplayName("Should update employee permissions successfully")
        fun shouldUpdateEmployeePermissionsSuccessfully() {
            // Given
            val permissionsRequest = UpdateEmployeePermissionsRequest(
                canProcessRedemptions = true,
                canManageInventory = true,
                canViewReports = false
            )
            val updatedResponse = testEmployeeResponse.copy(
                canProcessRedemptions = true,
                canManageInventory = true,
                canViewReports = false
            )
            whenever(employeeService.updateEmployeePermissions(eq(1L), any<UpdateEmployeePermissionsRequest>()))
                .thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/employees/1/permissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(permissionsRequest))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.can_process_redemptions").value(true))
                .andExpect(jsonPath("$.can_manage_inventory").value(true))
                .andExpect(jsonPath("$.can_view_reports").value(false))

            verify(employeeService).updateEmployeePermissions(eq(1L), any<UpdateEmployeePermissionsRequest>())
        }

        @Test
        @DisplayName("Should update employee hourly rate successfully")
        fun shouldUpdateEmployeeHourlyRateSuccessfully() {
            // Given
            val newRate = BigDecimal("22.50")
            val updatedResponse = testEmployeeResponse.copy(hourlyRate = newRate)
            whenever(employeeService.updateEmployeeHourlyRate(1L, newRate)).thenReturn(updatedResponse)

            // When & Then
            mockMvc.perform(
                patch("/api/v1/employees/1/hourly-rate")
                    .param("hourlyRate", newRate.toString())
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.hourly_rate").value(22.50))

            verify(employeeService).updateEmployeeHourlyRate(1L, newRate)
        }
    }

    @Nested
    @DisplayName("Employee Query Operations")
    inner class EmployeeQueryOperations {

        @Test
        @DisplayName("Should get employees by station successfully")
        fun shouldGetEmployeesByStationSuccessfully() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val employeesPage = PageImpl(listOf(testEmployeeResponse), pageable, 1)
            whenever(employeeService.getEmployeesByStation(eq(1L), any())).thenReturn(employeesPage)

            // When & Then
            mockMvc.perform(
                get("/api/v1/employees/station/1")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].station_id").value(1))

            verify(employeeService).getEmployeesByStation(eq(1L), any())
        }

        @Test
        @DisplayName("Should get employees by role successfully")
        fun shouldGetEmployeesByRoleSuccessfully() {
            // Given
            val pageable = PageRequest.of(0, 10)
            val employeesPage = PageImpl(listOf(testEmployeeResponse), pageable, 1)
            whenever(employeeService.getEmployeesByRole(eq(EmployeeRole.CASHIER), any())).thenReturn(employeesPage)

            // When & Then
            mockMvc.perform(
                get("/api/v1/employees/role/CASHIER")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].role").value("CASHIER"))

            verify(employeeService).getEmployeesByRole(eq(EmployeeRole.CASHIER), any())
        }

        @Test
        @DisplayName("Should search employees by criteria successfully")
        fun shouldSearchEmployeesByCriteriaSuccessfully() {
            // Given
            val searchRequest = EmployeeSearchRequest(
                stationId = 1L,
                role = EmployeeRole.CASHIER,
                isActive = true
            )
            val pageable = PageRequest.of(0, 10)
            val searchResults = PageImpl(listOf(testEmployeeResponse), pageable, 1)
            whenever(employeeService.searchEmployees(any<EmployeeSearchRequest>(), any())).thenReturn(searchResults)

            // When & Then
            mockMvc.perform(
                post("/api/v1/employees/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(searchRequest))
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content[0].station_id").value(1))
                .andExpect(jsonPath("$.content[0].role").value("CASHIER"))

            verify(employeeService).searchEmployees(any<EmployeeSearchRequest>(), any())
        }

        @Test
        @DisplayName("Should get employee by code successfully")
        fun shouldGetEmployeeByCodeSuccessfully() {
            // Given
            whenever(employeeService.getEmployeeByCode("EMP001")).thenReturn(testEmployeeResponse)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/code/EMP001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.user_id").value(testEmployee.userId))

            verify(employeeService).getEmployeeByCode("EMP001")
        }
    }

    @Nested
    @DisplayName("Employee Statistics and Utility Operations")
    inner class EmployeeStatisticsAndUtilityOperations {

        @Test
        @DisplayName("Should get employee statistics successfully")
        fun shouldGetEmployeeStatisticsSuccessfully() {
            // Given
            val statistics = EmployeeStatisticsResponse(
                totalEmployees = 50,
                activeEmployees = 45,
                terminatedEmployees = 5,
                managers = 3,
                supervisors = 8,
                cashiers = 25,
                canProcessRedemptions = 40,
                averageHourlyRate = BigDecimal("16.75")
            )
            whenever(employeeService.getEmployeeStatistics()).thenReturn(statistics)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/statistics"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.total_employees").value(50))
                .andExpect(jsonPath("$.active_employees").value(45))
                .andExpect(jsonPath("$.average_hourly_rate").value(16.75))

            verify(employeeService).getEmployeeStatistics()
        }

        @Test
        @DisplayName("Should get all employee roles successfully")
        fun shouldGetAllEmployeeRolesSuccessfully() {
            // Given
            val roles = EmployeeRole.values().map { EmployeeRoleResponse.fromEmployeeRole(it) }
            whenever(employeeService.getAllEmployeeRoles()).thenReturn(roles)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/roles"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(EmployeeRole.values().size))

            verify(employeeService).getAllEmployeeRoles()
        }

        @Test
        @DisplayName("Should check if user is employee successfully")
        fun shouldCheckIfUserIsEmployeeSuccessfully() {
            // Given
            whenever(employeeService.isUserAlreadyEmployee(123L)).thenReturn(true)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/user/123/exists"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.is_employee").value(true))

            verify(employeeService).isUserAlreadyEmployee(123L)
        }

        @Test
        @DisplayName("Should check if employee code exists successfully")
        fun shouldCheckIfEmployeeCodeExistsSuccessfully() {
            // Given
            whenever(employeeService.employeeCodeExists("EMP001")).thenReturn(false)

            // When & Then
            mockMvc.perform(get("/api/v1/employees/code/EMP001/exists"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.code_exists").value(false))

            verify(employeeService).employeeCodeExists("EMP001")
        }

        @Test
        @DisplayName("Should get health check successfully")
        fun shouldGetHealthCheckSuccessfully() {
            // When & Then
            mockMvc.perform(get("/api/v1/employees/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("Employee Service"))
        }
    }
}