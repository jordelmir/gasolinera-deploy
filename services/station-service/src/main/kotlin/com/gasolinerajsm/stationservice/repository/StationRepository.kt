package com.gasolinerajsm.stationservice.repository

import com.gasolinerajsm.stationservice.model.Station
import com.gasolinerajsm.stationservice.model.StationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StationRepository : JpaRepository<Station, Long> {

    fun findByCode(code: String): Station?

    fun existsByCode(code: String): Boolean

    fun findByStatus(status: StationStatus): List<Station>

    fun findByCity(city: String): List<Station>
}