package com.gasolinerajsm.redemptionservice.model

import jakarta.persistence.*
import jakarta.validation.constraints.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * RedemptionItem entity representing individual items in a redemption transaction
 */
@Entity
@Table(
    name = "redemption_items",
    schema = "redemption_schema",
    indexes = [
        Index(name = "idx_redemption_items_redemption_id", columnList = "redemption_id"),
        Index(name = "idx_redemption_items_product_code", columnList = "product_code"),
        Index(name = "idx_redemption_items_category", columnList = "category"),
        Index(name = "idx_redemption_items_item_type", columnList = "item_type"),
        Index(name = "idx_redemption_items_created_at", columnList = "created_at")
    ]
)
data class RedemptionItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redemption_id", nullable = false)
    @field:NotNull(message = "Redemption is required")
    val redemption: Redemption,

    @Column(name = "product_code", length = 50)
    val productCode: String? = null,

    @Column(name = "product_name", nullable = false, length = 200)
    @field:NotBlank(message = "Product name is required")
    @field:Size(min = 1, max = 200, message = "Product name must be between 1 and 200 characters")
    val productName: String,

    @Column(name = "description", length = 500)
    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    val itemType: RedemptionItemType,

    @Column(name = "category", length = 100)
    val category: String? = null,

    @Column(name = "subcategory", length = 100)
    val subcategory: String? = null,

    @Column(name = "brand", length = 100)
    val brand: String? = null,

    @Column(name = "size_or_volume", length = 50)
    val sizeOrVolume: String? = null,

    @Column(name = "unit_of_measure", length = 20)
    val unitOfMeasure: String? = null,

    @Column(name = "quantity", nullable = false)
    @field:NotNull(message = "Quantity is required")
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int,

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    @field:NotNull(message = "Unit price is required")
    @field:DecimalMin(value = "0.0", message = "Unit price must be positive")
    val unitPrice: BigDecimal,

    @Column(name = "original_unit_price", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Original unit price must be positive")
    val originalUnitPrice: BigDecimal? = null,

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Discount amount must be positive")
    val discountAmount: BigDecimal? = null,

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Discount percentage must be non-negative")
    @field:DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    val discountPercentage: BigDecimal? = null,

    @Column(name = "tax_amount", precision = 10, scale = 2)
    @field:DecimalMin(value = "0.0", message = "Tax amount must be positive")
    val taxAmount: BigDecimal? = null,

    @Column(name = "tax_rate", precision = 5, scale = 4)
    @field:DecimalMin(value = "0.0", message = "Tax rate must be non-negative")
    val taxRate: BigDecimal? = null,

    @Column(name = "total_price", precision = 10, scale = 2, nullable = false)
    @field:NotNull(message = "Total price is required")
    @field:DecimalMin(value = "0.0", message = "Total price must be positive")
    val totalPrice: BigDecimal,

    @Column(name = "is_free_item", nullable = false)
    val isFreeItem: Boolean = false,

    @Column(name = "is_discounted", nullable = false)
    val isDiscounted: Boolean = false,

    @Column(name = "is_taxable", nullable = false)
    val isTaxable: Boolean = true,

    @Column(name = "requires_age_verification", nullable = false)
    val requiresAgeVerification: Boolean = false,

    @Column(name = "age_verified", nullable = false)
    val ageVerified: Boolean = false,

    @Column(name = "barcode", length = 50)
    val barcode: String? = null,

    @Column(name = "serial_number", length = 100)
    val serialNumber: String? = null,

    @Column(name = "lot_number", length = 50)
    val lotNumber: String? = null,

    @Column(name = "expiry_date")
    val expiryDate: LocalDateTime? = null,

    @Column(name = "manufacturer", length = 100)
    val manufacturer: String? = null,

    @Column(name = "supplier_code", length = 50)
    val supplierCode: String? = null,

    @Column(name = "inventory_location", length = 100)
    val inventoryLocation: String? = null,

    @Column(name = "weight_grams")
    @field:Min(value = 0, message = "Weight must be non-negative")
    val weightGrams: Int? = null,

    @Column(name = "volume_ml")
    @field:Min(value = 0, message = "Volume must be non-negative")
    val volumeMl: Int? = null,

    @Column(name = "loyalty_points_earned")
    @field:Min(value = 0, message = "Loyalty points must be non-negative")
    val loyaltyPointsEarned: Int? = null,

    @Column(name = "promotion_code", length = 50)
    val promotionCode: String? = null,

    @Column(name = "campaign_id")
    val campaignId: Long? = null,

    @Column(name = "line_number", nullable = false)
    @field:Min(value = 1, message = "Line number must be at least 1")
    val lineNumber: Int,

    @Column(name = "notes", length = 500)
    @field:Size(max = 500, message = "Notes must not exceed 500 characters")
    val notes: String? = null,

    @Column(name = "metadata", length = 1000)
    val metadata: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Calculate the total discount amount for this item
     */
    fun getTotalDiscountAmount(): BigDecimal {
        return discountAmount ?: BigDecimal.ZERO
    }

    /**
     * Calculate the effective discount percentage
     */
    fun getEffectiveDiscountPercentage(): BigDecimal? {
        return if (originalUnitPrice != null && originalUnitPrice > BigDecimal.ZERO) {
            val discount = originalUnitPrice.subtract(unitPrice)
            discount.divide(originalUnitPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
        } else discountPercentage
    }

    /**
     * Calculate savings per item
     */
    fun getSavingsPerItem(): BigDecimal {
        return originalUnitPrice?.subtract(unitPrice) ?: BigDecimal.ZERO
    }

    /**
     * Calculate total savings for all quantities
     */
    fun getTotalSavings(): BigDecimal {
        return getSavingsPerItem().multiply(BigDecimal(quantity))
    }

    /**
     * Get the subtotal before tax
     */
    fun getSubtotal(): BigDecimal {
        return unitPrice.multiply(BigDecimal(quantity))
    }

    /**
     * Get the total tax amount
     */
    fun getTotalTaxAmount(): BigDecimal {
        return taxAmount ?: BigDecimal.ZERO
    }

    /**
     * Calculate effective tax rate
     */
    fun getEffectiveTaxRate(): BigDecimal? {
        return if (isTaxable && taxAmount != null && getSubtotal() > BigDecimal.ZERO) {
            taxAmount.divide(getSubtotal(), 4, java.math.RoundingMode.HALF_UP)
        } else taxRate
    }

    /**
     * Check if item is expired
     */
    fun isExpired(): Boolean {
        return expiryDate?.isBefore(LocalDateTime.now()) == true
    }

    /**
     * Check if item requires special handling
     */
    fun requiresSpecialHandling(): Boolean {
        return requiresAgeVerification || isExpired() || itemType == RedemptionItemType.RESTRICTED
    }

    /**
     * Get item weight in kilograms
     */
    fun getWeightKg(): BigDecimal? {
        return weightGrams?.let { BigDecimal(it).divide(BigDecimal(1000), 3, java.math.RoundingMode.HALF_UP) }
    }

    /**
     * Get item volume in liters
     */
    fun getVolumeLiters(): BigDecimal? {
        return volumeMl?.let { BigDecimal(it).divide(BigDecimal(1000), 3, java.math.RoundingMode.HALF_UP) }
    }

    /**
     * Get formatted product display name
     */
    fun getFormattedProductName(): String {
        return buildString {
            append(productName)
            brand?.let { append(" - $it") }
            sizeOrVolume?.let { append(" ($it)") }
        }
    }

    /**
     * Get item category hierarchy
     */
    fun getCategoryHierarchy(): String {
        return buildString {
            category?.let { append(it) }
            subcategory?.let {
                if (isNotEmpty()) append(" > ")
                append(it)
            }
        }
    }

    /**
     * Check if item qualifies for loyalty points
     */
    fun qualifiesForLoyaltyPoints(): Boolean {
        return loyaltyPointsEarned != null && loyaltyPointsEarned > 0 && !isFreeItem
    }

    /**
     * Apply discount to the item
     */
    fun applyDiscount(discountAmount: BigDecimal): RedemptionItem {
        val newUnitPrice = unitPrice.subtract(discountAmount.divide(BigDecimal(quantity), 2, java.math.RoundingMode.HALF_UP))
        val newTotalPrice = newUnitPrice.multiply(BigDecimal(quantity))

        return this.copy(
            discountAmount = discountAmount,
            unitPrice = newUnitPrice,
            totalPrice = newTotalPrice,
            isDiscounted = true,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Apply percentage discount
     */
    fun applyPercentageDiscount(percentage: BigDecimal): RedemptionItem {
        val discountAmount = getSubtotal().multiply(percentage.divide(BigDecimal(100), 4, java.math.RoundingMode.HALF_UP))
        return applyDiscount(discountAmount).copy(discountPercentage = percentage)
    }

    /**
     * Mark as free item
     */
    fun markAsFree(): RedemptionItem {
        return this.copy(
            unitPrice = BigDecimal.ZERO,
            totalPrice = BigDecimal.ZERO,
            isFreeItem = true,
            isDiscounted = true,
            discountAmount = originalUnitPrice?.multiply(BigDecimal(quantity)),
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Verify age for restricted items
     */
    fun verifyAge(): RedemptionItem {
        return this.copy(
            ageVerified = true,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Add notes to the item
     */
    fun addNotes(additionalNotes: String): RedemptionItem {
        val newNotes = if (notes.isNullOrBlank()) {
            additionalNotes
        } else {
            "$notes\n$additionalNotes"
        }
        return this.copy(
            notes = newNotes,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * Update metadata
     */
    fun updateMetadata(newMetadata: String): RedemptionItem {
        return this.copy(
            metadata = newMetadata,
            updatedAt = LocalDateTime.now()
        )
    }

    override fun toString(): String {
        return "RedemptionItem(id=$id, productName='$productName', quantity=$quantity, unitPrice=$unitPrice, totalPrice=$totalPrice)"
    }
}

/**
 * Redemption item type enumeration
 */
enum class RedemptionItemType(val displayName: String, val description: String) {
    FUEL("Fuel", "Gasoline, diesel, or other fuel products"),
    FOOD("Food", "Food items and beverages"),
    MERCHANDISE("Merchandise", "General merchandise and retail items"),
    SERVICE("Service", "Services like car wash, oil change, etc."),
    GIFT_CARD("Gift Card", "Gift cards and prepaid cards"),
    TOBACCO("Tobacco", "Tobacco products requiring age verification"),
    ALCOHOL("Alcohol", "Alcoholic beverages requiring age verification"),
    LOTTERY("Lottery", "Lottery tickets and gambling products"),
    RESTRICTED("Restricted", "Items with special restrictions or regulations"),
    PROMOTIONAL("Promotional", "Promotional items and samples"),
    DIGITAL("Digital", "Digital products and downloads"),
    SUBSCRIPTION("Subscription", "Subscription services and memberships");

    /**
     * Check if item type requires age verification
     */
    fun requiresAgeVerification(): Boolean {
        return this == TOBACCO || this == ALCOHOL || this == LOTTERY
    }

    /**
     * Check if item type is restricted
     */
    fun isRestricted(): Boolean {
        return this == TOBACCO || this == ALCOHOL || this == LOTTERY || this == RESTRICTED
    }

    /**
     * Check if item type is taxable by default
     */
    fun isTaxableByDefault(): Boolean {
        return this != GIFT_CARD && this != DIGITAL && this != SUBSCRIPTION
    }

    /**
     * Check if item type supports loyalty points
     */
    fun supportsLoyaltyPoints(): Boolean {
        return this != GIFT_CARD && this != LOTTERY && this != RESTRICTED
    }

    /**
     * Check if item type is physical
     */
    fun isPhysical(): Boolean {
        return this != DIGITAL && this != SUBSCRIPTION && this != SERVICE
    }

    /**
     * Check if item type requires inventory tracking
     */
    fun requiresInventoryTracking(): Boolean {
        return isPhysical() && this != SERVICE
    }
}