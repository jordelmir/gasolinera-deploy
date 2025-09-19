package com.gasolinerajsm.stationservice.model

import jakarta.persistence.Embeddable
import java.io.Serializable

/**
 * Value object representing a Station ID
 */
@Embeddable
data class StationId(
    val value: Long
) : Serializable {

    init {
        require(value > 0) { "Station ID must be positive" }
    }

    override fun toString(): String = value.toString()

    companion object {
        fun of(value: Long): StationId {
            return StationId(value)
        }
    }
}