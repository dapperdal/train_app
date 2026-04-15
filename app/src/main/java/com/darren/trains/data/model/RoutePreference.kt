package com.darren.trains.data.model

/**
 * User's route preferences - configured once in setup
 */
data class RoutePreference(
    val homeStationCrs: String,      // e.g., "LES"
    val workStationCrs: String,      // e.g., "FST"
    val filterLakesideLoop: Boolean = true,  // Filter out Lakeside loop trains by default
    val autoDetectLocation: Boolean = true   // Use GPS to detect current station
)
