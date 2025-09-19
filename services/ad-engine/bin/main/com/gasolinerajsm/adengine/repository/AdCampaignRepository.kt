package com.gasolinerajsm.adengine.repository

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Entity
@Table(name = "ad_campaigns")
data class AdCampaign(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var advertiserId: String, // Corresponds to the ID from the JWT principal
    var name: String,
    var adUrl: String,
    var startDate: LocalDate,
    var endDate: LocalDate,
    var budget: Double,
    var active: Boolean = true
)

@Repository
interface AdCampaignRepository : JpaRepository<AdCampaign, Long> {
    fun findByAdvertiserId(advertiserId: String): List<AdCampaign>
}