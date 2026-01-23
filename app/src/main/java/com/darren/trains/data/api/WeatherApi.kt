package com.darren.trains.data.api

import com.darren.trains.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for Open-Meteo weather API
 * Free API, no key required
 * https://open-meteo.com/en/docs
 */
interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getHourlyForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "precipitation_probability,precipitation,weather_code",
        @Query("timezone") timezone: String = "Europe/London",
        @Query("forecast_days") forecastDays: Int = 1
    ): WeatherResponse

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
}
