package com.darren.trains.data.model

import com.google.gson.annotations.SerializedName

/**
 * Open-Meteo API response for hourly weather forecast
 */
data class WeatherResponse(
    @SerializedName("hourly") val hourly: HourlyWeather?,
    @SerializedName("hourly_units") val hourlyUnits: HourlyUnits?,
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("timezone") val timezone: String?
)

data class HourlyWeather(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int>?,
    @SerializedName("precipitation") val precipitation: List<Double>?,
    @SerializedName("weather_code") val weatherCode: List<Int>?
)

data class HourlyUnits(
    @SerializedName("precipitation") val precipitation: String?,
    @SerializedName("precipitation_probability") val precipitationProbability: String?
)

/**
 * Simplified weather data for UI display
 */
data class ArrivalWeather(
    val isRaining: Boolean,
    val precipitationProbability: Int, // 0-100
    val precipitationMm: Double,
    val description: String, // "Light rain", "Heavy rain", "Clear", etc.
    val weatherCode: Int,
    val shouldBringUmbrella: Boolean
) {
    companion object {
        /**
         * Parse WMO weather code to human-readable description
         * See: https://open-meteo.com/en/docs#weathervariables
         */
        fun parseWeatherCode(code: Int): String {
            return when (code) {
                0 -> "Clear sky"
                1 -> "Mainly clear"
                2 -> "Partly cloudy"
                3 -> "Overcast"
                45, 48 -> "Foggy"
                51 -> "Light drizzle"
                53 -> "Moderate drizzle"
                55 -> "Dense drizzle"
                56, 57 -> "Freezing drizzle"
                61 -> "Light rain"
                63 -> "Moderate rain"
                65 -> "Heavy rain"
                66, 67 -> "Freezing rain"
                71 -> "Light snow"
                73 -> "Moderate snow"
                75 -> "Heavy snow"
                77 -> "Snow grains"
                80 -> "Light rain showers"
                81 -> "Moderate rain showers"
                82 -> "Heavy rain showers"
                85, 86 -> "Snow showers"
                95 -> "Thunderstorm"
                96, 99 -> "Thunderstorm with hail"
                else -> "Unknown"
            }
        }

        /**
         * Check if weather code indicates rain
         */
        fun isRainyWeatherCode(code: Int): Boolean {
            return code in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
        }
    }
}
