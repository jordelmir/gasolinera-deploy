package com.gasolinerajsm.raffleservice.domain.valueobject

import java.math.BigDecimal

/**
 * Value object representing the prize pool for a raffle
 */
data class PrizePool(
    val prizes: List<Prize>
) {

    init {
        require(prizes.isNotEmpty()) { "Prize pool must contain at least one prize" }
    }

    /**
     * Total value of all prizes in the pool
     */
    val totalValue: BigDecimal
        get() = prizes.sumOf { it.value }

    /**
     * Total number of prizes available
     */
    val totalPrizes: Int
        get() = prizes.sumOf { it.quantity }

    /**
     * Get prizes by tier
     */
    fun getPrizesByTier(tier: PrizeTier): List<Prize> {
        return prizes.filter { it.tier == tier }
    }

    /**
     * Check if prize pool has prizes of given tier
     */
    fun hasTier(tier: PrizeTier): Boolean {
        return prizes.any { it.tier == tier }
    }

    /**
     * Get the highest tier prize
     */
    fun getHighestTierPrize(): Prize? {
        return prizes.maxByOrNull { it.tier.level }
    }

    companion object {
        /**
         * Create a simple prize pool with one prize
         */
        fun singlePrize(value: BigDecimal, description: String): PrizePool {
            return PrizePool(
                listOf(
                    Prize(
                        id = PrizeId.generate(),
                        name = "Main Prize",
                        description = description,
                        value = value,
                        tier = PrizeTier.FIRST,
                        quantity = 1
                    )
                )
            )
        }

        /**
         * Create a prize pool with multiple tiers
         */
        fun multiTier(vararg prizes: Prize): PrizePool {
            return PrizePool(prizes.toList())
        }
    }
}

/**
 * Prize entity within the prize pool
 */
data class Prize(
    val id: PrizeId,
    val name: String,
    val description: String,
    val value: BigDecimal,
    val tier: PrizeTier,
    val quantity: Int,
    val imageUrl: String? = null,
    val category: String? = null
) {

    init {
        require(quantity > 0) { "Prize quantity must be positive" }
        require(value >= BigDecimal.ZERO) { "Prize value must be non-negative" }
    }

    /**
     * Check if this is a cash prize
     */
    fun isCashPrize(): Boolean = category == "CASH"

    /**
     * Check if this is a physical prize
     */
    fun isPhysicalPrize(): Boolean = category == "PHYSICAL"
}

/**
 * Prize tier enumeration
 */
enum class PrizeTier(val displayName: String, val level: Int) {
    FIRST("1st Prize", 1),
    SECOND("2nd Prize", 2),
    THIRD("3rd Prize", 3),
    CONSOLATION("Consolation", 4);

    companion object {
        fun fromLevel(level: Int): PrizeTier {
            return values().find { it.level == level }
                ?: throw IllegalArgumentException("Invalid prize tier level: $level")
        }
    }
}