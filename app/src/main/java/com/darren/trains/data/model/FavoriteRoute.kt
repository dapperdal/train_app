package com.darren.trains.data.model

/**
 * Represents a saved favorite route that user can quickly switch between
 */
data class FavoriteRoute(
    val id: String,                    // Unique identifier (e.g., "home_to_work")
    val name: String,                  // User-friendly name (e.g., "Home to Work")
    val fromStationCrs: String,        // Origin station code
    val toStationCrs: String,          // Destination station code
    val fromStationName: String = "",  // Cached station name
    val toStationName: String = "",    // Cached station name
    val isDefault: Boolean = false,    // Is this the primary route?
    val color: String = "#1A73E8"      // Color for UI
)

/**
 * Extended user preferences with support for multiple favorite routes
 */
data class ExtendedRoutePreference(
    val primaryRoute: RoutePreference,              // Main route for auto-detection
    val favoriteRoutes: List<FavoriteRoute> = emptyList(),
    val filterLakesideLoop: Boolean = true,        // Override primary setting
    val autoDetectLocation: Boolean = true,        // Use GPS detection
    val notifyOnDelay: Boolean = false,            // Opt-in disruption notifications
    val preferExpressTrains: Boolean = false,      // Skip trains with many stops
    val preferredOperator: String? = null,         // E.g., "C2C" only
    val maxChangeTime: Int = 15                    // Max connection time in minutes
)
