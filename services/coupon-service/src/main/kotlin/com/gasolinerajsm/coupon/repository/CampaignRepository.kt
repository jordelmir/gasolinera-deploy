package com.gasolinerajsm.coupon.repository

import com.gasolinerajsm.coupon.entity.Campaign
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CampaignRepository : JpaRepository<Campaign, Long>
