package com.gasolinerajsm.redemptionservice.repository

import com.gasolinerajsm.redemptionservice.model.RedemptionItem
import com.gasolinerajsm.redemptionservice.model.RedemptionItemType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Repository interface for RedemptionItem entity operations
 */
@Repository
interface RedemptionItemRepository : JpaRepository<RedemptionItem, Long> {

    /**
     * Find all items by redemption ID
     */
    fun findByRedemptionIdOrderByLineNumber(redemptionId: Long): List<RedemptionItem>

    /**
     * Find items by product code
     */
    fun findByProductCodeOrderByCreatedAtDesc(productCode: String): List<RedemptionItem>

    /**
     * Find items by product name (case-insensitive)
     */
    fun findByProductNameContainingIgnoreCaseOrderByCreatedAtDesc(productName: String): List<RedemptionItem>

    /**
     * Find items by item type
     */
    fun findByItemTypeOrderByCreatedAtDesc(itemType: RedemptionItemType): List<RedemptionItem>

    /**
     * Find items by category
     */
    fun findByCategoryOrderByCreatedAtDesc(category: String): List<RedemptionItem>

    /**
     * Find items by brand
     */
    fun findByBrandOrderByCreatedAtDesc(brand: String): List<RedemptionItem>

    /**
     * Find free items
     */
    fun findByIsFreeItemTrueOrderByCreatedAtDesc(): List<RedemptionItem>

    /**
     * Find discounted items
     */
    fun findByIsDiscountedTrueOrderByCreatedAtDesc(): List<RedemptionItem>

    /**
     * Find items requiring age verification
     */
    fun findByRequiresAgeVerificationTrueOrderByCreatedAtDesc(): List<RedemptionItem>

    /**
     * Find items that need age verification
     */
    fun findByRequiresAgeVerificationTrueAndAgeVerifiedFalseOrderByCreatedAtDesc(): List<RedemptionItem>

    /**
     * Find items by barcode
     */
    fun findByBarcodeOrderByCreatedAtDesc(barcode: String): List<RedemptionItem>

    /**
     * Find items by serial number
     */
    fun findBySerialNumber(serialNumber: String): RedemptionItem?

    /**
     * Find expired items
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.expiryDate IS NOT NULL AND ri.expiryDate < :currentTime ORDER BY ri.expiryDate ASC")
    fun findExpiredItems(@Param("currentTime") currentTime: LocalDateTime): List<RedemptionItem>

    /**
     * Find items expiring soon
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.expiryDate IS NOT NULL AND ri.expiryDate BETWEEN :currentTime AND :futureTime ORDER BY ri.expiryDate ASC")
    fun findItemsExpiringSoon(
        @Param("currentTime") currentTime: LocalDateTime,
        @Param("futureTime") futureTime: LocalDateTime
    ): List<RedemptionItem>

    /**
     * Find items by manufacturer
     */
    fun findByManufacturerOrderByCreatedAtDesc(manufacturer: String): List<RedemptionItem>

    /**
     * Find items by supplier code
     */
    fun findBySupplierCodeOrderByCreatedAtDesc(supplierCode: String): List<RedemptionItem>

    /**
     * Find items by promotion code
     */
    fun findByPromotionCodeOrderByCreatedAtDesc(promotionCode: String): List<RedemptionItem>

    /**
     * Find items by campaign ID
     */
    fun findByCampaignIdOrderByCreatedAtDesc(campaignId: Long): List<RedemptionItem>

    /**
     * Find items by date range
     */
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<RedemptionItem>

    /**
     * Find items by date range with pagination
     */
    fun findByCreatedAtBetweenOrderByCreatedAtDesc(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable
    ): Page<RedemptionItem>

    /**
     * Count items by redemption ID
     */
    fun countByRedemptionId(redemptionId: Long): Long

    /**
     * Count items by product code
     */
    fun countByProductCode(productCode: String): Long

    /**
     * Count items by item type
     */
    fun countByItemType(itemType: RedemptionItemType): Long

    /**
     * Count free items
     */
    fun countByIsFreeItemTrue(): Long

    /**
     * Count discounted items
     */
    fun countByIsDiscountedTrue(): Long

    /**
     * Sum total quantity by redemption ID
     */
    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM RedemptionItem ri WHERE ri.redemption.id = :redemptionId")
    fun sumQuantityByRedemptionId(@Param("redemptionId") redemptionId: Long): Int

    /**
     * Sum total price by redemption ID
     */
    @Query("SELECT COALESCE(SUM(ri.totalPrice), 0) FROM RedemptionItem ri WHERE ri.redemption.id = :redemptionId")
    fun sumTotalPriceByRedemptionId(@Param("redemptionId") redemptionId: Long): BigDecimal

    /**
     * Sum total discount by redemption ID
     */
    @Query("SELECT COALESCE(SUM(ri.discountAmount), 0) FROM RedemptionItem ri WHERE ri.redemption.id = :redemptionId AND ri.discountAmount IS NOT NULL")
    fun sumDiscountAmountByRedemptionId(@Param("redemptionId") redemptionId: Long): BigDecimal

    /**
     * Sum total tax by redemption ID
     */
    @Query("SELECT COALESCE(SUM(ri.taxAmount), 0) FROM RedemptionItem ri WHERE ri.redemption.id = :redemptionId AND ri.taxAmount IS NOT NULL")
    fun sumTaxAmountByRedemptionId(@Param("redemptionId") redemptionId: Long): BigDecimal

    /**
     * Find top-selling products by quantity
     */
    @Query("""
        SELECT ri.productCode, ri.productName, SUM(ri.quantity) as totalQuantity
        FROM RedemptionItem ri
        WHERE ri.createdAt BETWEEN :startDate AND :endDate
        AND ri.productCode IS NOT NULL
        GROUP BY ri.productCode, ri.productName
        ORDER BY totalQuantity DESC
    """)
    fun findTopSellingProductsByQuantity(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Array<Any>>

    /**
     * Find top-selling products by revenue
     */
    @Query("""
        SELECT ri.productCode, ri.productName, SUM(ri.totalPrice) as totalRevenue
        FROM RedemptionItem ri
        WHERE ri.createdAt BETWEEN :startDate AND :endDate
        AND ri.productCode IS NOT NULL
        GROUP BY ri.productCode, ri.productName
        ORDER BY totalRevenue DESC
    """)
    fun findTopSellingProductsByRevenue(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): Page<Array<Any>>

    /**
     * Find items by price range
     */
    fun findByUnitPriceBetweenOrderByUnitPriceDesc(
        minPrice: BigDecimal,
        maxPrice: BigDecimal
    ): List<RedemptionItem>

    /**
     * Find items with loyalty points
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.loyaltyPointsEarned IS NOT NULL AND ri.loyaltyPointsEarned > 0 ORDER BY ri.loyaltyPointsEarned DESC")
    fun findItemsWithLoyaltyPoints(): List<RedemptionItem>

    /**
     * Sum loyalty points by redemption ID
     */
    @Query("SELECT COALESCE(SUM(ri.loyaltyPointsEarned), 0) FROM RedemptionItem ri WHERE ri.redemption.id = :redemptionId AND ri.loyaltyPointsEarned IS NOT NULL")
    fun sumLoyaltyPointsByRedemptionId(@Param("redemptionId") redemptionId: Long): Int

    /**
     * Find items by weight range
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.weightGrams IS NOT NULL AND ri.weightGrams BETWEEN :minWeight AND :maxWeight ORDER BY ri.weightGrams DESC")
    fun findItemsByWeightRange(
        @Param("minWeight") minWeight: Int,
        @Param("maxWeight") maxWeight: Int
    ): List<RedemptionItem>

    /**
     * Find items by volume range
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.volumeMl IS NOT NULL AND ri.volumeMl BETWEEN :minVolume AND :maxVolume ORDER BY ri.volumeMl DESC")
    fun findItemsByVolumeRange(
        @Param("minVolume") minVolume: Int,
        @Param("maxVolume") maxVolume: Int
    ): List<RedemptionItem>

    /**
     * Find items by inventory location
     */
    fun findByInventoryLocationOrderByCreatedAtDesc(inventoryLocation: String): List<RedemptionItem>

    /**
     * Find items with notes
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.notes IS NOT NULL AND ri.notes != '' ORDER BY ri.createdAt DESC")
    fun findItemsWithNotes(): List<RedemptionItem>

    /**
     * Find items by lot number
     */
    fun findByLotNumberOrderByCreatedAtDesc(lotNumber: String): List<RedemptionItem>

    /**
     * Get category statistics
     */
    @Query("""
        SELECT ri.category, COUNT(ri) as itemCount, SUM(ri.quantity) as totalQuantity, SUM(ri.totalPrice) as totalRevenue
        FROM RedemptionItem ri
        WHERE ri.createdAt BETWEEN :startDate AND :endDate
        AND ri.category IS NOT NULL
        GROUP BY ri.category
        ORDER BY totalRevenue DESC
    """)
    fun getCategoryStatistics(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Get brand statistics
     */
    @Query("""
        SELECT ri.brand, COUNT(ri) as itemCount, SUM(ri.quantity) as totalQuantity, SUM(ri.totalPrice) as totalRevenue
        FROM RedemptionItem ri
        WHERE ri.createdAt BETWEEN :startDate AND :endDate
        AND ri.brand IS NOT NULL
        GROUP BY ri.brand
        ORDER BY totalRevenue DESC
    """)
    fun getBrandStatistics(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    /**
     * Find items needing inventory update
     */
    @Query("SELECT ri FROM RedemptionItem ri WHERE ri.itemType IN :trackableTypes ORDER BY ri.createdAt DESC")
    fun findItemsNeedingInventoryUpdate(@Param("trackableTypes") trackableTypes: List<RedemptionItemType>): List<RedemptionItem>

    /**
     * Check if product exists in redemptions
     */
    fun existsByProductCode(productCode: String): Boolean

    /**
     * Find duplicate items in same redemption
     */
    @Query("""
        SELECT ri FROM RedemptionItem ri
        WHERE ri.redemption.id = :redemptionId
        AND ri.productCode = :productCode
        AND ri.id != :excludeId
    """)
    fun findDuplicateItemsInRedemption(
        @Param("redemptionId") redemptionId: Long,
        @Param("productCode") productCode: String,
        @Param("excludeId") excludeId: Long
    ): List<RedemptionItem>
}