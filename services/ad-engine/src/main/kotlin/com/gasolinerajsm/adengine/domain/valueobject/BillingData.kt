package com.gasolinerajsm.adengine.domain.valueobject

import java.math.BigDecimal

/**
 * Value object representing billing data for ad campaigns
 */
data class BillingData(
    val campaignId: String,
    val totalBudget: BigDecimal,
    val spentAmount: BigDecimal = BigDecimal.ZERO,
    val remainingBudget: BigDecimal = BigDecimal.ZERO,
    val costPerImpression: BigDecimal = BigDecimal.ZERO,
    val costPerClick: BigDecimal = BigDecimal.ZERO,
    val currency: String = "USD",
    val billingCycle: String = "MONTHLY"
) {
    init {
        require(campaignId.isNotBlank()) { "Campaign ID cannot be blank" }
        require(totalBudget >= BigDecimal.ZERO) { "Total budget cannot be negative" }
        require(spentAmount >= BigDecimal.ZERO) { "Spent amount cannot be negative" }
        require(costPerImpression >= BigDecimal.ZERO) { "Cost per impression cannot be negative" }
        require(costPerClick >= BigDecimal.ZERO) { "Cost per click cannot be negative" }
    }

    fun isOverBudget(): Boolean = spentAmount > totalBudget

    fun budgetUtilization(): Double {
        return if (totalBudget > BigDecimal.ZERO) {
            spentAmount.divide(totalBudget, 4, java.math.RoundingMode.HALF_UP).toDouble()
        } else 0.0
    }

    fun canAfford(cost: BigDecimal): Boolean {
        return (spentAmount + cost) <= totalBudget
    }

    fun addExpense(amount: BigDecimal): BillingData {
        require(amount >= BigDecimal.ZERO) { "Expense amount cannot be negative" }
        val newSpent = spentAmount + amount
        return copy(
            spentAmount = newSpent,
            remainingBudget = totalBudget - newSpent
        )
    }

    companion object {
        fun create(campaignId: String, budget: BigDecimal): BillingData {
            return BillingData(
                campaignId = campaignId,
                totalBudget = budget,
                remainingBudget = budget
            )
        }
    }
}