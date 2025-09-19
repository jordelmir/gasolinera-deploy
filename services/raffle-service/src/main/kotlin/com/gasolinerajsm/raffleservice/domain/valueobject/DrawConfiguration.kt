package com.gasolinerajsm.raffleservice.domain.valueobject

/**
 * Value object representing draw configuration for a raffle
 */
data class DrawConfiguration(
    val winnerSelectionMethod: WinnerSelectionMethod,
    val seedSource: SeedSource,
    val allowMultipleWins: Boolean = false,
    val maxWinnersPerUser: Int = 1,
    val drawAlgorithm: DrawAlgorithm = DrawAlgorithm.RANDOM
) {

    init {
        require(maxWinnersPerUser > 0) { "Max winners per user must be positive" }
    }

    companion object {
        /**
         * Create standard random draw configuration
         */
        fun random(): DrawConfiguration {
            return DrawConfiguration(
                winnerSelectionMethod = WinnerSelectionMethod.RANDOM,
                seedSource = SeedSource.BLOCKCHAIN,
                allowMultipleWins = false,
                maxWinnersPerUser = 1,
                drawAlgorithm = DrawAlgorithm.RANDOM
            )
        }

        /**
         * Create weighted draw configuration
         */
        fun weighted(): DrawConfiguration {
            return DrawConfiguration(
                winnerSelectionMethod = WinnerSelectionMethod.WEIGHTED,
                seedSource = SeedSource.BLOCKCHAIN,
                allowMultipleWins = false,
                maxWinnersPerUser = 1,
                drawAlgorithm = DrawAlgorithm.WEIGHTED_RANDOM
            )
        }
    }
}

/**
 * Winner selection method enumeration
 */
enum class WinnerSelectionMethod {
    RANDOM,
    WEIGHTED,
    PROBABILITY_BASED,
    FIRST_COME_FIRST_SERVED
}

/**
 * Seed source for draw randomization
 */
enum class SeedSource {
    BLOCKCHAIN,
    TIMESTAMP,
    EXTERNAL_API,
    COMBINED
}

/**
 * Draw algorithm enumeration
 */
enum class DrawAlgorithm {
    RANDOM,
    WEIGHTED_RANDOM,
    PROBABILITY_BASED,
    MERKLE_TREE
}