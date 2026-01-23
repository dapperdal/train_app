package com.darren.trains.data.model

import com.google.gson.annotations.SerializedName

/**
 * Response from Huxley2 departures endpoint
 */
data class DeparturesResponse(
    @SerializedName("trainServices") val trainServices: List<TrainService>?,
    @SerializedName("busServices") val busServices: List<Any>?,
    @SerializedName("ferryServices") val ferryServices: List<Any>?,
    @SerializedName("generatedAt") val generatedAt: String?,
    @SerializedName("locationName") val locationName: String?,
    @SerializedName("crs") val crs: String?,
    @SerializedName("filterLocationName") val filterLocationName: String?,
    @SerializedName("filtercrs") val filterCrs: String?,
    @SerializedName("nrccMessages") val nrccMessages: List<NrccMessage>?,
    @SerializedName("platformAvailable") val platformAvailable: Boolean?
)

data class TrainService(
    @SerializedName("std") val scheduledDeparture: String?, // Scheduled Time of Departure
    @SerializedName("etd") val estimatedDeparture: String?, // Estimated Time of Departure (e.g., "On time", "14:35", "Delayed")
    @SerializedName("sta") val scheduledArrival: String?, // Scheduled Time of Arrival (at destination)
    @SerializedName("eta") val estimatedArrival: String?, // Estimated Time of Arrival
    @SerializedName("platform") val platform: String?,
    @SerializedName("operator") val operator: String?,
    @SerializedName("operatorCode") val operatorCode: String?,
    @SerializedName("serviceType") val serviceType: Int?,
    @SerializedName("serviceID") val serviceId: String?,
    @SerializedName("rsid") val rsid: String?,
    @SerializedName("origin") val origin: List<StationInfo>?,
    @SerializedName("destination") val destination: List<StationInfo>?,
    @SerializedName("subsequentCallingPoints") val subsequentCallingPoints: List<CallingPointList>?,
    @SerializedName("previousCallingPoints") val previousCallingPoints: List<CallingPointList>?,
    @SerializedName("isCancelled") val isCancelled: Boolean?,
    @SerializedName("cancelReason") val cancelReason: String?,
    @SerializedName("delayReason") val delayReason: String?,
    @SerializedName("length") val length: Int?
)

data class StationInfo(
    @SerializedName("locationName") val locationName: String?,
    @SerializedName("crs") val crs: String?,
    @SerializedName("via") val via: String?,
    @SerializedName("futureChangeTo") val futureChangeTo: String?
)

data class CallingPointList(
    @SerializedName("callingPoint") val callingPoints: List<CallingPoint>?,
    @SerializedName("serviceType") val serviceType: Int?,
    @SerializedName("serviceChangeRequired") val serviceChangeRequired: Boolean?
)

data class CallingPoint(
    @SerializedName("locationName") val locationName: String?,
    @SerializedName("crs") val crs: String?,
    @SerializedName("st") val scheduledTime: String?,
    @SerializedName("et") val estimatedTime: String?,
    @SerializedName("at") val actualTime: String?,
    @SerializedName("isCancelled") val isCancelled: Boolean?,
    @SerializedName("length") val length: Int?
)

data class NrccMessage(
    @SerializedName("category") val category: String?,
    @SerializedName("severity") val severity: Int?,
    @SerializedName("xhtmlMessage") val xhtmlMessage: String?
)

// UI models

data class TrainDeparture(
    val serviceId: String,
    val departureTime: String,
    val estimatedTime: String,
    val platform: String,
    val destination: String,
    val journeyTimeMinutes: Int?,
    val status: TrainStatus,
    val delayMinutes: Int,
    val isCancelled: Boolean,
    val callingPoints: List<CallingPoint> = emptyList()
)

enum class TrainStatus {
    ON_TIME,
    DELAYED,
    CANCELLED
}

data class Disruption(
    val message: String,
    val severity: DisruptionSeverity
)

enum class DisruptionSeverity {
    MINOR,
    MAJOR,
    SEVERE
}

// Station constants
object Stations {
    const val LEIGH_ON_SEA_CRS = "LES"
    const val LEIGH_ON_SEA_NAME = "Leigh-on-Sea"
    const val FENCHURCH_STREET_CRS = "FST"
    const val FENCHURCH_STREET_NAME = "Fenchurch Street"
    const val GRAYS_CRS = "GRY"

    // Coordinates
    val LEIGH_ON_SEA_LAT = 51.5420
    val LEIGH_ON_SEA_LON = 0.6530
    val FENCHURCH_STREET_LAT = 51.5118
    val FENCHURCH_STREET_LON = -0.0786
}

enum class TravelDirection {
    TO_FENCHURCH_STREET,
    TO_LEIGH_ON_SEA
}
