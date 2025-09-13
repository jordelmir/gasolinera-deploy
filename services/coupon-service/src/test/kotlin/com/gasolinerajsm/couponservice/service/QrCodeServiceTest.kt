package com.gasolinerajsm.couponservice.service

import com.gasolinerajsm.couponservice.model.Campaign
import com.gasolinerajsm.couponservice.model.Coupon
import com.gasolinerajsm.couponservice.model.CampaignType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("QR Code Service Tests")
class QrCodeServiceTest {

    private lateinit var qrCodeService: QrCodeService
    private lateinit var testCampaign: Campaign
    private lateinit var testCoupon: Coupon

    @BeforeEach
    fun setUp() {
        qrCodeService = QrCodeService()

        testCampaign = Campaign(
            id = 123L,
            name = "Test Campaign",
            campaignType = CampaignType.DISCOUNT,
            startDate = LocalDateTime.now().minusDays(1),
            endDate = LocalDateTime.now().plusDays(30)
        )

        testCoupon = Coupon(
            id = 1L,
            campaign = testCampaign,
            qrCode = "GSL_v1_000123_20240315120000_ABC12345_COUP001",
            qrSignature = "test-signature",
            couponCode = "COUP001",
            validFrom = LocalDateTime.now().minusDays(1),
            validUntil = LocalDateTime.now().plusDays(30),
            discountAmount = BigDecimal("10.00"),
            raffleTickets = 5
        )
    }

    @Nested
    @DisplayName("QR Code Generation Tests")
    inner class QrCodeGenerationTests {

        @Test
        @DisplayName("Should generate valid QR code format")
        fun shouldGenerateValidQrCodeFormat() {
            // When
            val qrCode = qrCodeService.generateQrCode(testCampaign, "COUP001")

            // Then
            assertTrue(qrCode.startsWith("GSL_v1_"))
            assertTrue(qrCode.contains("000123")) // Campaign ID padded
            assertTrue(qrCode.endsWith("_COUP001"))

            // Verify format
            assertTrue(qrCodeService.isValidQrCodeFormat(qrCode))
        }

        @Test
        @DisplayName("Should generate unique QR codes")
        fun shouldGenerateUniqueQrCodes() {
            // When
            val qrCode1 = qrCodeService.generateQrCode(testCampaign, "COUP001")
            val qrCode2 = qrCodeService.generateQrCode(testCampaign, "COUP001")

            // Then
            assertNotEquals(qrCode1, qrCode2)
        }

        @Test
        @DisplayName("Should include campaign ID in QR code")
        fun shouldIncludeCampaignIdInQrCode() {
            // When
            val qrCode = qrCodeService.generateQrCode(testCampaign, "COUP001")

            // Then
            assertTrue(qrCode.contains("000123")) // Campaign ID 123 padded to 6 digits
        }

        @Test
        @DisplayName("Should include coupon code in QR code")
        fun shouldIncludeCouponCodeInQrCode() {
            // When
            val qrCode = qrCodeService.generateQrCode(testCampaign, "TESTCOUP")

            // Then
            assertTrue(qrCode.endsWith("_TESTCOUP"))
        }
    }

    @Nested
    @DisplayName("QR Code Signature Tests")
    inner class QrCodeSignatureTests {

        @Test
        @DisplayName("Should generate QR signature")
        fun shouldGenerateQrSignature() {
            // When
            val signature = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)

            // Then
            assertNotNull(signature)
            assertTrue(signature.isNotEmpty())
        }

        @Test
        @DisplayName("Should generate consistent signatures for same data")
        fun shouldGenerateConsistentSignaturesForSameData() {
            // When
            val signature1 = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)
            val signature2 = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)

            // Then
            assertEquals(signature1, signature2)
        }

        @Test
        @DisplayName("Should generate different signatures for different data")
        fun shouldGenerateDifferentSignaturesForDifferentData() {
            // Given
            val modifiedCoupon = testCoupon.copy(discountAmount = BigDecimal("20.00"))

            // When
            val signature1 = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)
            val signature2 = qrCodeService.generateQrSignature(testCoupon.qrCode, modifiedCoupon)

            // Then
            assertNotEquals(signature1, signature2)
        }

        @Test
        @DisplayName("Should validate correct QR signature")
        fun shouldValidateCorrectQrSignature() {
            // Given
            val signature = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)

            // When
            val isValid = qrCodeService.validateQrSignature(testCoupon.qrCode, signature, testCoupon)

            // Then
            assertTrue(isValid)
        }

        @Test
        @DisplayName("Should reject invalid QR signature")
        fun shouldRejectInvalidQrSignature() {
            // Given
            val invalidSignature = "invalid-signature"

            // When
            val isValid = qrCodeService.validateQrSignature(testCoupon.qrCode, invalidSignature, testCoupon)

            // Then
            assertFalse(isValid)
        }

        @Test
        @DisplayName("Should reject tampered QR code")
        fun shouldRejectTamperedQrCode() {
            // Given
            val signature = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)
            val tamperedQrCode = testCoupon.qrCode.replace("COUP001", "COUP999")

            // When
            val isValid = qrCodeService.validateQrSignature(tamperedQrCode, signature, testCoupon)

            // Then
            assertFalse(isValid)
        }
    }

    @Nested
    @DisplayName("QR Code Parsing Tests")
    inner class QrCodeParsingTests {

        @Test
        @DisplayName("Should parse valid QR code components")
        fun shouldParseValidQrCodeComponents() {
            // Given
            val qrCode = "GSL_v1_000123_20240315120000_ABC12345_COUP001"

            // When
            val components = qrCodeService.parseQrCode(qrCode)

            // Then
            assertNotNull(components)
            assertEquals("GSL", components!!.prefix)
            assertEquals("v1", components.version)
            assertEquals(123L, components.campaignId)
            assertEquals("20240315120000", components.timestamp)
            assertEquals("ABC12345", components.randomComponent)
            assertEquals("COUP001", components.couponCode)
        }

        @Test
        @DisplayName("Should return null for invalid QR code format")
        fun shouldReturnNullForInvalidQrCodeFormat() {
            // Given
            val invalidQrCode = "INVALID_QR_CODE"

            // When
            val components = qrCodeService.parseQrCode(invalidQrCode)

            // Then
            assertNull(components)
        }

        @Test
        @DisplayName("Should return null for QR code with wrong prefix")
        fun shouldReturnNullForQrCodeWithWrongPrefix() {
            // Given
            val wrongPrefixQrCode = "WRONG_v1_000123_20240315120000_ABC12345_COUP001"

            // When
            val components = qrCodeService.parseQrCode(wrongPrefixQrCode)

            // Then
            assertNull(components)
        }

        @Test
        @DisplayName("Should return null for QR code with wrong version")
        fun shouldReturnNullForQrCodeWithWrongVersion() {
            // Given
            val wrongVersionQrCode = "GSL_v2_000123_20240315120000_ABC12345_COUP001"

            // When
            val components = qrCodeService.parseQrCode(wrongVersionQrCode)

            // Then
            assertNull(components)
        }

        @Test
        @DisplayName("Should return null for QR code with invalid campaign ID")
        fun shouldReturnNullForQrCodeWithInvalidCampaignId() {
            // Given
            val invalidCampaignIdQrCode = "GSL_v1_INVALID_20240315120000_ABC12345_COUP001"

            // When
            val components = qrCodeService.parseQrCode(invalidCampaignIdQrCode)

            // Then
            assertNull(components)
        }
    }

    @Nested
    @DisplayName("QR Code Validation Tests")
    inner class QrCodeValidationTests {

        @Test
        @DisplayName("Should validate correct QR code format")
        fun shouldValidateCorrectQrCodeFormat() {
            // Given
            val validQrCode = "GSL_v1_000123_20240315120000_ABC12345_COUP001"

            // When
            val isValid = qrCodeService.isValidQrCodeFormat(validQrCode)

            // Then
            assertTrue(isValid)
        }

        @Test
        @DisplayName("Should reject invalid QR code format")
        fun shouldRejectInvalidQrCodeFormat() {
            // Given
            val invalidQrCode = "INVALID_FORMAT"

            // When
            val isValid = qrCodeService.isValidQrCodeFormat(invalidQrCode)

            // Then
            assertFalse(isValid)
        }

        @Test
        @DisplayName("Should detect expired QR code by timestamp")
        fun shouldDetectExpiredQrCodeByTimestamp() {
            // Given - QR code with old timestamp (more than 24 hours ago)
            val oldTimestamp = LocalDateTime.now().minusHours(25).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val expiredQrCode = "GSL_v1_000123_${oldTimestamp}_ABC12345_COUP001"

            // When
            val isExpired = qrCodeService.isQrCodeExpiredByTimestamp(expiredQrCode)

            // Then
            assertTrue(isExpired)
        }

        @Test
        @DisplayName("Should not detect recent QR code as expired")
        fun shouldNotDetectRecentQrCodeAsExpired() {
            // Given - QR code with recent timestamp
            val recentTimestamp = LocalDateTime.now().minusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            val recentQrCode = "GSL_v1_000123_${recentTimestamp}_ABC12345_COUP001"

            // When
            val isExpired = qrCodeService.isQrCodeExpiredByTimestamp(recentQrCode)

            // Then
            assertFalse(isExpired)
        }
    }

    @Nested
    @DisplayName("QR Code Data Generation Tests")
    inner class QrCodeDataGenerationTests {

        @Test
        @DisplayName("Should generate complete QR code data")
        fun shouldGenerateCompleteQrCodeData() {
            // When
            val qrCodeData = qrCodeService.generateQrCodeData(testCoupon)

            // Then
            assertEquals(testCoupon.qrCode, qrCodeData.qrCode)
            assertEquals(testCoupon.qrSignature, qrCodeData.signature)
            assertEquals(testCoupon.couponCode, qrCodeData.couponCode)
            assertEquals(testCoupon.campaign.id, qrCodeData.campaignId)
            assertEquals(testCoupon.campaign.name, qrCodeData.campaignName)
            assertEquals(testCoupon.discountAmount, qrCodeData.discountAmount)
            assertEquals(testCoupon.discountPercentage, qrCodeData.discountPercentage)
            assertEquals(testCoupon.raffleTickets, qrCodeData.raffleTickets)
            assertEquals(testCoupon.validFrom, qrCodeData.validFrom)
            assertEquals(testCoupon.validUntil, qrCodeData.validUntil)
            assertEquals(testCoupon.termsAndConditions, qrCodeData.termsAndConditions)
        }

        @Test
        @DisplayName("Should validate QR code data successfully")
        fun shouldValidateQrCodeDataSuccessfully() {
            // Given
            val qrCodeData = qrCodeService.generateQrCodeData(testCoupon)
            val validSignature = qrCodeService.generateQrSignature(testCoupon.qrCode, testCoupon)
            val validQrCodeData = qrCodeData.copy(signature = validSignature)

            // When
            val validationResult = qrCodeService.validateQrCodeData(validQrCodeData, testCoupon)

            // Then
            assertTrue(validationResult.isValid)
            assertTrue(validationResult.errors.isEmpty())
        }

        @Test
        @DisplayName("Should detect QR code data tampering")
        fun shouldDetectQrCodeDataTampering() {
            // Given
            val qrCodeData = qrCodeService.generateQrCodeData(testCoupon)
            val tamperedData = qrCodeData.copy(couponCode = "TAMPERED")

            // When
            val validationResult = qrCodeService.validateQrCodeData(tamperedData, testCoupon)

            // Then
            assertFalse(validationResult.isValid)
            assertTrue(validationResult.errors.contains("Coupon code mismatch"))
        }
    }

    @Nested
    @DisplayName("QR Code Security Tests")
    inner class QrCodeSecurityTests {

        @Test
        @DisplayName("Should generate QR code hash")
        fun shouldGenerateQrCodeHash() {
            // When
            val hash = qrCodeService.generateQrCodeHash(testCoupon.qrCode)

            // Then
            assertNotNull(hash)
            assertTrue(hash.isNotEmpty())
        }

        @Test
        @DisplayName("Should generate consistent hashes for same QR code")
        fun shouldGenerateConsistentHashesForSameQrCode() {
            // When
            val hash1 = qrCodeService.generateQrCodeHash(testCoupon.qrCode)
            val hash2 = qrCodeService.generateQrCodeHash(testCoupon.qrCode)

            // Then
            assertEquals(hash1, hash2)
        }

        @Test
        @DisplayName("Should generate different hashes for different QR codes")
        fun shouldGenerateDifferentHashesForDifferentQrCodes() {
            // Given
            val qrCode1 = "GSL_v1_000123_20240315120000_ABC12345_COUP001"
            val qrCode2 = "GSL_v1_000123_20240315120000_ABC12345_COUP002"

            // When
            val hash1 = qrCodeService.generateQrCodeHash(qrCode1)
            val hash2 = qrCodeService.generateQrCodeHash(qrCode2)

            // Then
            assertNotEquals(hash1, hash2)
        }

        @Test
        @DisplayName("Should verify QR code integrity successfully")
        fun shouldVerifyQrCodeIntegritySuccessfully() {
            // Given
            val hash = qrCodeService.generateQrCodeHash(testCoupon.qrCode)

            // When
            val isIntact = qrCodeService.verifyQrCodeIntegrity(testCoupon.qrCode, hash)

            // Then
            assertTrue(isIntact)
        }

        @Test
        @DisplayName("Should detect QR code integrity violation")
        fun shouldDetectQrCodeIntegrityViolation() {
            // Given
            val originalHash = qrCodeService.generateQrCodeHash(testCoupon.qrCode)
            val tamperedQrCode = testCoupon.qrCode.replace("COUP001", "COUP999")

            // When
            val isIntact = qrCodeService.verifyQrCodeIntegrity(tamperedQrCode, originalHash)

            // Then
            assertFalse(isIntact)
        }
    }
}