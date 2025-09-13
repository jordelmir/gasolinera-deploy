package com.gasolinerajsm.stationservice.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("Employee Entity Tests")
class EmployeeTest {

    private val testStation = Station(
        id = 1L,
        name = "Test Station",
        address = "123 Test Street",
        latitude = BigDecimal("40.7128"),
        longitude = BigDecimal("-74.0060"),
        phoneNumber = "+1234567890"
    )

    @Nested
    @DisplayName("Employee Creation Tests")
    inner class EmployeeCreationTests {

        @Test
        @DisplayName("Should create employee with valid data")
        fun shouldCreateEmployeeWithValidData() {
            // Given
            val userId = 123L
            val role = EmployeeRole.CASHIER
            val hireDate = LocalDateTime.now().minusMonths(6)

            // When
            val employee = Employee(
                userId = userId,
                station = testStation,
                role = role,
                hireDate = hireDate
            )

            // Then
            assertEquals(userId, employee.userId)
            assertEquals(testStation, employee.station)
            assertEquals(role, employee.role)
            assertEquals(hireDate, employee.hireDate)
            assertTrue(employee.isActive)
            assertTrue(employee.canProcessRedemptions)
            assertFalse(employee.canManageInventory)
            assertFalse(employee.canViewReports)
            assertNull(employee.terminationDate)
        }

        @Test
        @DisplayName("Should create employee with all optional fields")
        fun shouldCreateEmployeeWithAllOptionalFields() {
            // Given
            val hireDate = LocalDateTime.now().minusYears(2)
            val hourlyRate = BigDecimal("15.50")

            // When
            val employee = Employee(
                userId = 456L,
                station = testStation,
                role = EmployeeRole.MANAGER,
                employeeCode = "MGR001",
                hireDate = hireDate,
                hourlyRate = hourlyRate,
                canProcessRedemptions = true,
                canManageInventory = true,
                canViewReports = true,
                emergencyContactName = "Jane Doe",
                emergencyContactPhone = "+1987654321",
                notes = "Experienced manager with excellent customer service skills"
            )

            // Then
            assertEquals("MGR001", employee.employeeCode)
            assertEquals(hourlyRate, employee.hourlyRate)
            assertTrue(employee.canManageInventory)
            assertTrue(employee.canViewReports)
            assertEquals("Jane Doe", employee.emergencyContactName)
            assertEquals("+1987654321", employee.emergencyContactPhone)
            assertEquals("Experienced manager with excellent customer service skills", employee.notes)
        }
    }

    @Nested
    @DisplayName("Employee Business Logic Tests")
    inner class EmployeeBusinessLogicTests {

        private val testEmployee = Employee(
            id = 1L,
            userId = 123L,
            station = testStation,
            role = EmployeeRole.SUPERVISOR,
            hireDate = LocalDateTime.now().minusMonths(18),
            isActive = true
        )

        @Test
        @DisplayName("Should detect currently employed status")
        fun shouldDetectCurrentlyEmployedStatus() {
            // When & Then
            assertTrue(testEmployee.isCurrentlyEmployed())
            assertFalse(testEmployee.isTerminated())
        }

        @Test
        @DisplayName("Should detect terminated employee")
        fun shouldDetectTerminatedEmployee() {
            // Given
            val terminatedEmployee = testEmployee.copy(
                terminationDate = LocalDateTime.now().minusDays(30),
                isActive = false
            )

            // When & Then
            assertFalse(terminatedEmployee.isCurrentlyEmployed())
            assertTrue(terminatedEmployee.isTerminated())
        }

        @Test
        @DisplayName("Should check management task permissions")
        fun shouldCheckManagementTaskPermissions() {
            // Given
            val manager = testEmployee.copy(role = EmployeeRole.MANAGER)
            val cashier = testEmployee.copy(role = EmployeeRole.CASHIER)

            // When & Then
            assertTrue(manager.canPerformManagementTasks())
            assertFalse(cashier.canPerformManagementTasks())
        }

        @Test
        @DisplayName("Should check transaction processing permissions")
        fun shouldCheckTransactionProcessingPermissions() {
            // Given
            val cashier = testEmployee.copy(role = EmployeeRole.CASHIER, canProcessRedemptions = true)
            val attendant = testEmployee.copy(role = EmployeeRole.ATTENDANT, canProcessRedemptions = false)

            // When & Then
            assertTrue(cashier.canProcessTransactions())
            assertFalse(attendant.canProcessTransactions())
        }

        @Test
        @DisplayName("Should calculate tenure correctly")
        fun shouldCalculateTenureCorrectly() {
            // Given
            val employee = testEmployee.copy(hireDate = LocalDateTime.now().minusMonths(24))

            // When
            val tenureMonths = employee.getTenureInMonths()
            val tenureYears = employee.getTenureInYears()

            // Then
            assertEquals(24L, tenureMonths)
            assertEquals(2.0, tenureYears, 0.1)
        }

        @Test
        @DisplayName("Should check specific permissions")
        fun shouldCheckSpecificPermissions() {
            // Given
            val employee = testEmployee.copy(
                canProcessRedemptions = true,
                canManageInventory = false,
                canViewReports = true,
                role = EmployeeRole.MANAGER
            )

            // When & Then
            assertTrue(employee.hasPermission(EmployeePermission.PROCESS_REDEMPTIONS))
            assertFalse(employee.hasPermission(EmployeePermission.MANAGE_INVENTORY))
            assertTrue(employee.hasPermission(EmployeePermission.VIEW_REPORTS))
            assertTrue(employee.hasPermission(EmployeePermission.MANAGE_EMPLOYEES))
            assertTrue(employee.hasPermission(EmployeePermission.MANAGE_STATION))
        }
    }

    @Nested
    @DisplayName("Employee State Management Tests")
    inner class EmployeeStateManagementTests {

        private val testEmployee = Employee(
            id = 1L,
            userId = 123L,
            station = testStation,
            role = EmployeeRole.CASHIER,
            hireDate = LocalDateTime.now().minusMonths(6),
            isActive = false,
            terminationDate = LocalDateTime.now().minusDays(10)
        )

        @Test
        @DisplayName("Should activate employee")
        fun shouldActivateEmployee() {
            // When
            val activatedEmployee = testEmployee.activate()

            // Then
            assertTrue(activatedEmployee.isActive)
            assertNull(activatedEmployee.terminationDate)
        }

        @Test
        @DisplayName("Should deactivate employee")
        fun shouldDeactivateEmployee() {
            // Given
            val activeEmployee = testEmployee.copy(isActive = true, terminationDate = null)

            // When
            val deactivatedEmployee = activeEmployee.deactivate()

            // Then
            assertFalse(deactivatedEmployee.isActive)
        }

        @Test
        @DisplayName("Should terminate employee")
        fun shouldTerminateEmployee() {
            // Given
            val activeEmployee = testEmployee.copy(isActive = true, terminationDate = null)
            val terminationDate = LocalDateTime.now()

            // When
            val terminatedEmployee = activeEmployee.terminate(terminationDate)

            // Then
            assertFalse(terminatedEmployee.isActive)
            assertEquals(terminationDate, terminatedEmployee.terminationDate)
        }

        @Test
        @DisplayName("Should promote employee to new role")
        fun shouldPromoteEmployeeToNewRole() {
            // Given
            val cashier = testEmployee.copy(role = EmployeeRole.CASHIER)

            // When
            val promotedEmployee = cashier.promoteToRole(EmployeeRole.SUPERVISOR)

            // Then
            assertEquals(EmployeeRole.SUPERVISOR, promotedEmployee.role)
        }

        @Test
        @DisplayName("Should update employee permissions")
        fun shouldUpdateEmployeePermissions() {
            // When
            val updatedEmployee = testEmployee.updatePermissions(
                canProcessRedemptions = false,
                canManageInventory = true,
                canViewReports = true
            )

            // Then
            assertFalse(updatedEmployee.canProcessRedemptions)
            assertTrue(updatedEmployee.canManageInventory)
            assertTrue(updatedEmployee.canViewReports)
        }
    }

    @Nested
    @DisplayName("Employee Role Tests")
    inner class EmployeeRoleTests {

        @Test
        @DisplayName("Should identify management roles")
        fun shouldIdentifyManagementRoles() {
            // When & Then
            assertFalse(EmployeeRole.CASHIER.isManagementRole())
            assertFalse(EmployeeRole.ATTENDANT.isManagementRole())
            assertFalse(EmployeeRole.SUPERVISOR.isManagementRole())
            assertTrue(EmployeeRole.ASSISTANT_MANAGER.isManagementRole())
            assertTrue(EmployeeRole.MANAGER.isManagementRole())
            assertTrue(EmployeeRole.DISTRICT_MANAGER.isManagementRole())
        }

        @Test
        @DisplayName("Should check transaction processing capabilities")
        fun shouldCheckTransactionProcessingCapabilities() {
            // When & Then
            assertTrue(EmployeeRole.CASHIER.canProcessTransactions())
            assertFalse(EmployeeRole.ATTENDANT.canProcessTransactions())
            assertTrue(EmployeeRole.SUPERVISOR.canProcessTransactions())
            assertTrue(EmployeeRole.MANAGER.canProcessTransactions())
        }

        @Test
        @DisplayName("Should check employee management capabilities")
        fun shouldCheckEmployeeManagementCapabilities() {
            // When & Then
            assertFalse(EmployeeRole.CASHIER.canManageEmployees())
            assertFalse(EmployeeRole.SUPERVISOR.canManageEmployees())
            assertTrue(EmployeeRole.MANAGER.canManageEmployees())
            assertTrue(EmployeeRole.DISTRICT_MANAGER.canManageEmployees())
        }

        @Test
        @DisplayName("Should check station management capabilities")
        fun shouldCheckStationManagementCapabilities() {
            // When & Then
            assertFalse(EmployeeRole.CASHIER.canManageStation())
            assertFalse(EmployeeRole.SUPERVISOR.canManageStation())
            assertTrue(EmployeeRole.MANAGER.canManageStation())
            assertTrue(EmployeeRole.DISTRICT_MANAGER.canManageStation())
        }

        @Test
        @DisplayName("Should check financial report access")
        fun shouldCheckFinancialReportAccess() {
            // When & Then
            assertFalse(EmployeeRole.CASHIER.canViewFinancialReports())
            assertFalse(EmployeeRole.ATTENDANT.canViewFinancialReports())
            assertTrue(EmployeeRole.SUPERVISOR.canViewFinancialReports())
            assertTrue(EmployeeRole.MANAGER.canViewFinancialReports())
        }

        @Test
        @DisplayName("Should get manageable roles")
        fun shouldGetManageableRoles() {
            // When
            val managerManageableRoles = EmployeeRole.MANAGER.getManageableRoles()
            val cashierManageableRoles = EmployeeRole.CASHIER.getManageableRoles()

            // Then
            assertTrue(managerManageableRoles.contains(EmployeeRole.CASHIER))
            assertTrue(managerManageableRoles.contains(EmployeeRole.SUPERVISOR))
            assertTrue(managerManageableRoles.contains(EmployeeRole.ASSISTANT_MANAGER))
            assertFalse(managerManageableRoles.contains(EmployeeRole.MANAGER))

            assertTrue(cashierManageableRoles.isEmpty())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Cashier", EmployeeRole.CASHIER.displayName)
            assertEquals("Fuel Attendant", EmployeeRole.ATTENDANT.displayName)
            assertEquals("Supervisor", EmployeeRole.SUPERVISOR.displayName)
            assertEquals("Assistant Manager", EmployeeRole.ASSISTANT_MANAGER.displayName)
            assertEquals("Station Manager", EmployeeRole.MANAGER.displayName)
            assertEquals("District Manager", EmployeeRole.DISTRICT_MANAGER.displayName)
        }
    }

    @Nested
    @DisplayName("Work Shift Tests")
    inner class WorkShiftTests {

        @Test
        @DisplayName("Should calculate shift duration correctly")
        fun shouldCalculateShiftDurationCorrectly() {
            // When & Then
            assertEquals(8, WorkShift.MORNING.getDurationHours())
            assertEquals(8, WorkShift.AFTERNOON.getDurationHours())
            assertEquals(8, WorkShift.NIGHT.getDurationHours())
            assertEquals(24, WorkShift.FULL_DAY.getDurationHours())
        }

        @Test
        @DisplayName("Should have correct display names")
        fun shouldHaveCorrectDisplayNames() {
            assertEquals("Morning Shift", WorkShift.MORNING.displayName)
            assertEquals("Afternoon Shift", WorkShift.AFTERNOON.displayName)
            assertEquals("Night Shift", WorkShift.NIGHT.displayName)
            assertEquals("Full Day", WorkShift.FULL_DAY.displayName)
        }
    }

    @Nested
    @DisplayName("Employee Validation Tests")
    inner class EmployeeValidationTests {

        @Test
        @DisplayName("Should validate toString method")
        fun shouldValidateToStringMethod() {
            // Given
            val employee = Employee(
                id = 1L,
                userId = 123L,
                station = testStation,
                role = EmployeeRole.CASHIER,
                hireDate = LocalDateTime.now(),
                isActive = true
            )

            // When
            val stringRepresentation = employee.toString()

            // Then
            assertTrue(stringRepresentation.contains("id=1"))
            assertTrue(stringRepresentation.contains("userId=123"))
            assertTrue(stringRepresentation.contains("stationId=1"))
            assertTrue(stringRepresentation.contains("role=CASHIER"))
            assertTrue(stringRepresentation.contains("isActive=true"))
        }
    }
}