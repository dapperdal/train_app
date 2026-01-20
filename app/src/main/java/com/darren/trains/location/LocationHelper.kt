package com.darren.trains.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.darren.trains.data.model.Stations
import com.darren.trains.data.model.TravelDirection
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.*

@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(processLocation(location))
                } else {
                    // Try last known location as fallback
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            continuation.resume(processLocation(lastLocation))
                        } else {
                            continuation.resume(null)
                        }
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }

    private fun processLocation(location: Location): LocationResult {
        val distanceToLeigh = calculateDistance(
            location.latitude, location.longitude,
            Stations.LEIGH_ON_SEA_LAT, Stations.LEIGH_ON_SEA_LON
        )

        val distanceToFenchurch = calculateDistance(
            location.latitude, location.longitude,
            Stations.FENCHURCH_STREET_LAT, Stations.FENCHURCH_STREET_LON
        )

        // Determine if near a station (within threshold)
        val nearLeigh = distanceToLeigh <= NEAR_STATION_THRESHOLD_MILES
        val nearFenchurch = distanceToFenchurch <= NEAR_STATION_THRESHOLD_MILES

        val detectedDirection = when {
            nearLeigh -> TravelDirection.TO_FENCHURCH_STREET
            nearFenchurch -> TravelDirection.TO_LEIGH_ON_SEA
            else -> null // Not near either station - will need manual toggle
        }

        val nearestStation = if (distanceToLeigh <= distanceToFenchurch) {
            NearestStation(
                name = Stations.LEIGH_ON_SEA_NAME,
                crs = Stations.LEIGH_ON_SEA_CRS,
                distanceMiles = distanceToLeigh
            )
        } else {
            NearestStation(
                name = Stations.FENCHURCH_STREET_NAME,
                crs = Stations.FENCHURCH_STREET_CRS,
                distanceMiles = distanceToFenchurch
            )
        }

        return LocationResult(
            latitude = location.latitude,
            longitude = location.longitude,
            nearestStation = nearestStation,
            detectedDirection = detectedDirection,
            distanceToLeigh = distanceToLeigh,
            distanceToFenchurch = distanceToFenchurch
        )
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * @return Distance in miles
     */
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadiusMiles = 3958.8

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))

        return earthRadiusMiles * c
    }

    companion object {
        const val NEAR_STATION_THRESHOLD_MILES = 1.0
    }
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val nearestStation: NearestStation,
    val detectedDirection: TravelDirection?,
    val distanceToLeigh: Double,
    val distanceToFenchurch: Double
)

data class NearestStation(
    val name: String,
    val crs: String,
    val distanceMiles: Double
)
