package com.gasolinerajsm.stationservice.infrastructure.persistence

import com.gasolinerajsm.stationservice.domain.model.Station
import com.gasolinerajsm.stationservice.domain.model.StationStatus
import com.gasolinerajsm.stationservice.domain.repository.StationRepository
import com.gasolinerajsm.stationservice.domain.valueobject.Location
import com.gasolinerajsm.stationservice.domain.valueobject.StationId
import com.gasolinerajsm.stationservice.infrastructure.persistence.entity.*
import com.gasolinerajsm.stationservice.infrastructure.persistence.jpa.StationJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * JPA Repository Adapter implementing StationRepository port
 */
@Repository
@Transactional
class StationJpaRepositoryAdapter(
    private val jpaRepository: StationJpaRepository
) : StationRepository {

    override suspend fun save(station: Station): Result<Station> {
        return try {
            // Convert domain to entity
            val stationEntity = StationEntity.fromDomain(station)

            // Create related entities
            val savedEntity = jpaRepository.save(stationEntity)

            // Save operating hours
            val operatingHoursEntities = station.operatingHours.schedule.map { (day, schedule) ->
                OperatingHoursEntity(
                    station = savedEntity,
                    dayOfWeek = day,
                    openTime = schedule.openTime,
                    closeTime = schedule.closeTime,
                    isOpen = schedule.isOpen
                )
            }

            // Save fuel prices
            val fuelPriceEntities = station.fuelPrices.map { (fuelType, fuelPrice) ->
                FuelPriceEntity(
                    station = savedEntity,
                    fuelType = fuelType,
                    price = fuelPrice.amount,
                    lastUpdated = fuelPrice.lastUpdated
                )
            }

            // Save amenities
            val amenityEntities = station.amenities.map { amenity ->
                StationAmenityEntity(
                    station = savedEntity,
                    amenity = amenity
                )
            }

            // Create complete entity with relationships
            val completeEntity = savedEntity.copy(
                operatingHours = operatingHoursEntities.toSet(),
                fuelPrices = fuelPriceEntities.toSet(),
                amenities = amenityEntities.toSet()
            )

            Result.success(completeEntity.toDomain())

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findById(id: StationId): Result<Station?> {
        return try {
            val entity = jpaRepository.findById(id.value).orElse(null)
            Result.success(entity?.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findAllActive(): Result<List<Station>> {
        return try {
            val entities = jpaRepository.findByIsActiveTrue()
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByStatus(status: StationStatus): Result<List<Station>> {
        return try {
            val entities = jpaRepository.findByStatus(status)
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByManagerId(managerId: String): Result<List<Station>> {
        return try {
            val entities = jpaRepository.findByManagerId(managerId)
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findWithinRadius(
        location: Location,
        radiusKm: Double
    ): Result<List<Station>> {
        return try {
            val entities = jpaRepository.findWithinRadius(
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = radiusKm
            )
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findNearestStations(
        location: Location,
        limit: Int
    ): Result<List<Station>> {
        return try {
            val entities = jpaRepository.findNearestStations(
                latitude = location.latitude,
                longitude = location.longitude
            ).take(limit)
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchByNameOrAddress(query: String): Result<List<Station>> {
        return try {
            val entities = jpaRepository.searchByNameOrAddress(query)
            val stations = entities.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun findByAmenities(amenities: Set<String>): Result<List<Station>> {
        return try {
            // This would require a more complex query - simplified for now
            val allStations = jpaRepository.findByIsActiveTrue()
            val filteredStations = allStations.filter { entity ->
                val stationAmenities = entity.amenities.map { it.amenity.name }.toSet()
                amenities.all { stationAmenities.contains(it) }
            }
            val stations = filteredStations.map { it.toDomain() }
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun existsByNameAndLocation(
        name: String,
        location: Location,
        radiusKm: Double
    ): Result<Boolean> {
        return try {
            val exists = jpaRepository.existsByNameAndLocation(
                name = name,
                latitude = location.latitude,
                longitude = location.longitude,
                radiusKm = radiusKm
            )
            Result.success(exists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteById(id: StationId): Result<Unit> {
        return try {
            jpaRepository.deleteById(id.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun count(): Result<Long> {
        return try {
            val count = jpaRepository.count()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countActive(): Result<Long> {
        return try {
            val count = jpaRepository.countByIsActiveTrue()
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun countByStatus(status: StationStatus): Result<Long> {
        return try {
            val count = jpaRepository.countByStatus(status)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}