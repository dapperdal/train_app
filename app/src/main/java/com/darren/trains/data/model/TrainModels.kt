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
    // All C2C stations in order from FST to SOU/LES
    val C2C_STATIONS = listOf(
        Station("FST", "Fenchurch Street", 51.5118, -0.0786, isTerminus = true),
        Station("SWL", "Shadwell", 51.5079, -0.0415),
        Station("WPL", "Wapping", 51.5040, -0.0559),
        Station("RTE", "Rotherhithe", 51.5015, -0.0544),
        Station("CWC", "Canada Water", 51.4974, -0.0167),
        Station("CWF", "Canary Wharf", 51.5037, -0.0206),
        Station("WFE", "Westferry", 51.5077, -0.0149),
        Station("BCN", "Bank/Monument", 51.5133, -0.0886),
        Station("LST", "Liverpool Street", 51.5176, -0.0826),
        Station("SRA", "Stratford", 51.5421, -0.0117),
        Station("UPM", "Upminster", 51.5555, 0.1044, isTerminus = true),
        Station("UPB", "Upminster Bridge", 51.5473, 0.1201),
        Station("OCK", "Ockendon", 51.5418, 0.1597),
        Station("CFH", "Chafford Hundred", 51.4869, 0.3057),
        Station("GRY", "Grays", 51.4570, 0.3658),  // Lakeside loop connection
        Station("TIL", "Tilbury Town", 51.4542, 0.3795),
        Station("SOU", "Southend Victoria", 51.5393, 0.7083, isTerminus = true),
        Station("PRL", "Prittlewell", 51.5358, 0.6996),
        Station("WCF", "Westcliff", 51.5304, 0.6988),
        Station("TBY", "Thorpe Bay", 51.5236, 0.6886),
        Station("SEN", "Southend Central", 51.5356, 0.7157),
        Station("SOE", "Southend East", 51.5251, 0.7261),
        Station("LES", "Leigh-on-Sea", 51.5420, 0.6530, isTerminus = true)
    )

    // Build a map for quick lookup by CRS
    val STATION_MAP = C2C_STATIONS.associateBy { it.crs }

    // Legacy constants for backward compatibility
    const val LEIGH_ON_SEA_CRS = "LES"
    const val LEIGH_ON_SEA_NAME = "Leigh-on-Sea"
    const val FENCHURCH_STREET_CRS = "FST"
    const val FENCHURCH_STREET_NAME = "Fenchurch Street"
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
