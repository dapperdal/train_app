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
    @SerializedName(value = "xhtmlMessage", alternate = ["value"]) val xhtmlMessage: String?
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

// Station data class
data class Station(
    val crs: String,              // 3-letter code (e.g., "LES", "FST")
    val name: String,             // Full station name
    val latitude: Double,         // For location detection
    val longitude: Double,        // For location detection
    val isTerminus: Boolean = false  // Whether it's an endpoint
)

// C2C Network stations (Fenchurch Street to Southend/Leigh-on-Sea)
object Stations {
    // The 26 core c2c stations. Liverpool Street and Stratford are limited-service
    // alternatives and are intentionally not offered as a regular locked commute.
    val C2C_STATIONS = listOf(
        Station("FST", "London Fenchurch Street", 51.5118, -0.0786, isTerminus = true),
        Station("LHS", "Limehouse", 51.5124, -0.0397),
        Station("WHM", "West Ham", 51.5285, 0.0053),
        Station("BKG", "Barking", 51.5393, 0.0817),
        Station("DDK", "Dagenham Dock", 51.5261, 0.1461),
        Station("RNM", "Rainham (Essex)", 51.5167, 0.1908),
        Station("PFL", "Purfleet", 51.4810, 0.2368),
        Station("CFH", "Chafford Hundred", 51.4856, 0.2874),
        Station("GRY", "Grays", 51.4762, 0.3218),
        Station("OCK", "Ockendon", 51.5210, 0.2906),
        Station("UPM", "Upminster", 51.5590, 0.2510),
        Station("TIL", "Tilbury Town", 51.4624, 0.3530),
        Station("ETL", "East Tilbury", 51.4840, 0.4129),
        Station("SOH", "Stanford-le-Hope", 51.5144, 0.4231),
        Station("WHR", "West Horndon", 51.5679, 0.3407),
        Station("LAI", "Laindon", 51.5675, 0.4243),
        Station("BSO", "Basildon", 51.5681, 0.4568),
        Station("PSE", "Pitsea", 51.5604, 0.5063),
        Station("BEF", "Benfleet", 51.5439, 0.5617),
        Station("LES", "Leigh-on-Sea", 51.5413, 0.6404),
        Station("CHW", "Chalkwell", 51.5387, 0.6706),
        Station("WCF", "Westcliff", 51.5373, 0.6915),
        Station("SOC", "Southend Central", 51.5370, 0.7117),
        Station("SOE", "Southend East", 51.5389, 0.7318),
        Station("TPB", "Thorpe Bay", 51.5376, 0.7616),
        Station("SRY", "Shoeburyness", 51.5310, 0.7954, isTerminus = true)
    )

    // Build a map for quick lookup by CRS
    val STATION_MAP = C2C_STATIONS.associateBy { it.crs }

    // Legacy constants for backward compatibility
    const val LEIGH_ON_SEA_CRS = "LES"
    const val LEIGH_ON_SEA_NAME = "Leigh-on-Sea"
    const val FENCHURCH_STREET_CRS = "FST"
    const val FENCHURCH_STREET_NAME = "London Fenchurch Street"
    const val FENCHURCH_STREET_LAT = 51.5118
    const val FENCHURCH_STREET_LON = -0.0786
    const val GRAYS_CRS = "GRY"
    const val LAKESIDE_LOOP_STATIONS = "GRY,TIL"  // Stations in the Lakeside loop

    // Get station by CRS code
    fun getStation(crs: String): Station? = STATION_MAP[crs]

    // Get station name by CRS code
    fun getStationName(crs: String): String = getStation(crs)?.name ?: crs
}

enum class TravelDirection {
    TO_FENCHURCH_STREET,
    TO_LEIGH_ON_SEA
}
