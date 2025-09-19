package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Value object representing advertisement budget configuration
 */
data class AdBudget(
    val totalBudget: BigDecimal,
    val dailyBudget: BigDecimal? = null,
    val currency: String = "CRC",
    val biddingStrategy: BiddingStrategy = BiddingStrategy.CPM,
    val maxCpm: BigDecimal? = null,
    val maxCpc: BigDecimal? = null,
    val totalSpend: BigDecimal = BigDecimal.ZERO,
    val dailySpend: BigDecimal = BigDecimal.ZERO,
    val lastSpendUpdate: LocalDateTime = LocalDateTime.now()
) {

    init {
        require(totalBudget >= BigDecimal.ZERO) { "Total budget must be non-negative" }
        dailyBudget?.let {
            require(it >= BigDecimal.ZERO) { "Daily budget must be non-negative" }
        }
        require(totalSpend >= BigDecimal.ZERO) { "Total spend must be non-negative" }
        require(dailySpend >= BigDecimal.ZERO) { "Daily spend must be non-negative" }
        require(totalSpend <= totalBudget) { "Total spend cannot exceed total budget" }
    }

    /**
     * Check if budget has remaining funds
     */
    fun hasRemainingBudget(): Boolean {
        return totalSpend < totalBudget
    }

    /**
     * Get remaining budget
     */
    fun getRemainingBudget(): BigDecimal {
        return totalBudget.subtract(totalSpend)
    }

    /**
     * Check if daily budget limit is reached
     */
    fun isDailyLimitReached(): Boolean {
        return dailyBudget?.let { dailySpend >= it } ?: false
    }

    /**
     * Get remaining daily budget
     */
    fun getRemainingDailyBudget(): BigDecimal? {
        return dailyBudget?.subtract(dailySpend)
    }

    /**
     * Calculate if we can afford a bid
     */
    fun canAfford(cost: BigDecimal): Boolean {
        if (!hasRemainingBudget()) return false

        // Check daily limit
        if (isDailyLimitReached()) return false

        // Check if this specific cost would exceed limits
        val newTotalSpend = totalSpend.add(cost)
        if (newTotalSpend > totalBudget) return false

        dailyBudget?.let {
            val newDailySpend = dailySpend.add(cost)
            if (newDailySpend > it) return false
        }

        return true
    }

    /**
     * Add spend to the budget
     */
    fun addSpend(cost: BigDecimal): AdBudget {
        require(cost >= BigDecimal.ZERO) { "Cost must be non-negative" }
        require(canAfford(cost)) { "Cannot afford this cost" }

        val newTotalSpend = totalSpend.add(cost)
        val newDailySpend = dailySpend.add(cost)

        return this.copy(
            totalSpend = newTotalSpend,
            dailySpend = newDailySpend,
            lastSpendUpdate = LocalDateTime.now()
        )
    }

    /**
     * Reset daily spend (for daily budget reset)
     */
    fun resetDailySpend(): AdBudget {
        return this.copy(
            dailySpend = BigDecimal.ZERO,
            lastSpendUpdate = LocalDateTime.now()
        )
    }

    /**
     * Get budget utilization percentage
     */
    fun getBudgetUtilizationPercentage(): BigDecimal {
        if (totalBudget == BigDecimal.ZERO) return BigDecimal.ZERO
        return totalSpend.divide(totalBudget, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
    }

    /**
     * Get daily budget utilization percentage
     */
    fun getDailyBudgetUtilizationPercentage(): BigDecimal? {
        return dailyBudget?.let {
            if (it == BigDecimal.ZERO) BigDecimal.ZERO
            else dailySpend.divide(it, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
        }
    }

    /**
     * Calculate effective CPM based on current spend
     */
    fun getEffectiveCpm(impressions: Long): BigDecimal {
        if (impressions <= 0) return BigDecimal.ZERO
        return totalSpend.divide(BigDecimal.valueOf(impressions), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(1000))
    }

    /**
     * Calculate effective CPC based on current spend
     */
    fun getEffectiveCpc(clicks: Long): BigDecimal {
        if (clicks <= 0) return BigDecimal.ZERO
        return totalSpend.divide(BigDecimal.valueOf(clicks), 4, RoundingMode.HALF_UP)
    }

    /**
     * Validate budget configuration
     */
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (totalBudget < BigDecimal.ZERO) {
            errors.add("Total budget cannot be negative")
        }

        dailyBudget?.let {
            if (it < BigDecimal.ZERO) {
                errors.add("Daily budget cannot be negative")
            }
            if (it > totalBudget) {
                errors.add("Daily budget cannot exceed total budget")
            }
        }

        if (totalSpend < BigDecimal.ZERO) {
            errors.add("Total spend cannot be negative")
        }

        if (dailySpend < BigDecimal.ZERO) {
            errors.add("Daily spend cannot be negative")
        }

        if (totalSpend > totalBudget) {
            errors.add("Total spend cannot exceed total budget")
        }

        maxCpm?.let {
            if (it <= BigDecimal.ZERO) {
                errors.add("Max CPM must be positive")
            }
        }

        maxCpc?.let {
            if (it <= BigDecimal.ZERO) {
                errors.add("Max CPC must be positive")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.success("Budget is valid")
        } else {
            ValidationResult.failure(errors.joinToString("; "))
        }
    }

    /**
     * Get budget status summary
     */
    fun getBudgetStatus(): BudgetStatus {
        val remaining = getRemainingBudget()
        val utilization = getBudgetUtilizationPercentage()

        return when {
            remaining <= BigDecimal.ZERO -> BudgetStatus.EXHAUSTED
            utilization >= BigDecimal.valueOf(90) -> BudgetStatus.CRITICAL
            utilization >= BigDecimal.valueOf(75) -> BudgetStatus.WARNING
            else -> BudgetStatus.HEALTHY
        }
    }

    companion object {
        fun create(
            totalBudget: BigDecimal,
            dailyBudget: BigDecimal? = null,
            currency: String = "CRC",
            biddingStrategy: BiddingStrategy = BiddingStrategy.CPM
        ): AdBudget {
            return AdBudget(
                totalBudget = totalBudget,
                dailyBudget = dailyBudget,
                currency = currency,
                biddingStrategy = biddingStrategy
            )
        }

        fun unlimited(): AdBudget {
            return AdBudget(
                totalBudget = BigDecimal.valueOf(Long.MAX_VALUE),
                biddingStrategy = BiddingStrategy.CPM
            )
        }
    }
}

/**
 * Bidding strategy enumeration
 */
enum class BiddingStrategy(val displayName: String, val description: String) {
    CPM("Cost Per Mille", "Pay per 1000 impressions"),
    CPC("Cost Per Click", "Pay per click"),
    CPA("Cost Per Action", "Pay per desired action"),
    FLAT("Flat Rate", "Fixed cost per period");

    fun isPerformanceBased(): Boolean {
        return this in listOf(CPC, CPA)
    }
}

/**
 * Budget status enumeration
 */
enum class BudgetStatus(val displayName: String, val color: String) {
    HEALTHY("Healthy", "green"),
    WARNING("Warning", "yellow"),
    CRITICAL("Critical", "orange"),
    EXHAUSTED("Exhausted", "red")
}