package com.darren.trains.data.repository

import com.darren.trains.data.api.WeatherApi
import com.darren.trains.data.model.ArrivalWeather
import com.darren.trains.data.model.Stations
import com.darren.trains.data.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {
    /**
     * Get weather forecast for Fenchurch Street at the estimated arrival time
     * @param arrivalTime Time in HH:mm format (e.g., "15:07")
     */
    suspend fun getWeatherAtFenchurchStreet(arrivalTime: String): Result<ArrivalWeather> =
        withContext(Dispatchers.IO) {
            try {
                val response = weatherApi.getHourlyForecast(
                    latitude = Stations.FENCHURCH_STREET_LAT,
                    longitude = Stations.FENCHURCH_STREET_LON
                )

                val weather = findWeatherAtTime(response, arrivalTime)
                    ?: return@withContext Result.failure(Exception("Weather data not available for arrival time"))

                Result.success(weather)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Find the weather data for the hour closest to the arrival time
     */
    private fun findWeatherAtTime(response: WeatherResponse, arrivalTime: String): ArrivalWeather? {
        val hourly = response.hourly ?: return null
        val times = hourly.time ?: return null
        val precipProbabilities = hourly.precipitationProbability ?: return null
        val precipitations = hourly.precipitation ?: return null
        val weatherCodes = hourly.weatherCode ?: return null

        // Parse the arrival time to get the hour
        val arrivalParts = arrivalTime.split(":")
        if (arrivalParts.size != 2) return null
        val arrivalHour = arrivalParts[0].toIntOrNull() ?: return null

        // Get today's date to match with the API response
        val today = LocalDateTime.now()
        val targetDateTimeStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) +
            "T${String.format("%02d", arrivalHour)}:00"

        // Find the index for the target hour
        val index = times.indexOfFirst { it == targetDateTimeStr }
        if (index == -1 || index >= precipProbabilities.size) {
            // Fallback: try to find closest hour
            val fallbackIndex = times.indexOfFirst {
                it.contains("T${String.format("%02d", arrivalHour)}:")
            }
            if (fallbackIndex == -1 || fallbackIndex >= precipProbabilities.size) return null
            return createArrivalWeather(
                precipProbabilities[fallbackIndex],
                precipitations.getOrNull(fallbackIndex) ?: 0.0,
                weatherCodes.getOrNull(fallbackIndex) ?: 0
            )
        }

        return createArrivalWeather(
            precipProbabilities[index],
            precipitations.getOrNull(index) ?: 0.0,
            weatherCodes.getOrNull(index) ?: 0
        )
    }

    private fun createArrivalWeather(
        precipProbability: Int,
        precipMm: Double,
        weatherCode: Int
    ): ArrivalWeather {
        val isRaining = ArrivalWeather.isRainyWeatherCode(weatherCode)
        val description = ArrivalWeather.parseWeatherCode(weatherCode)

        // Recommend umbrella if >40% chance of rain or already raining
        val shouldBringUmbrella = precipProbability > 40 || isRaining

        return ArrivalWeather(
            isRaining = isRaining,
            precipitationProbability = precipProbability,
            precipitationMm = precipMm,
            description = description,
            weatherCode = weatherCode,
            shouldBringUmbrella = shouldBringUmbrella
        )
    }
}
