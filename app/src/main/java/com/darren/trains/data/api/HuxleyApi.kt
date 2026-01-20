package com.darren.trains.data.api

import com.darren.trains.data.model.DeparturesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Huxley2 API interface for National Rail Darwin data
 * Base URL: https://huxley2.azurewebsites.net/
 */
interface HuxleyApi {

    /**
     * Get departures from a station filtered by destination
     * @param fromCrs The CRS code of the departure station (e.g., "LOS" for Leigh-on-Sea)
     * @param toCrs The CRS code of the destination station (e.g., "FST" for Fenchurch Street)
     * @param numRows Number of results to return (max 150)
     * @param expand Include calling points in response
     */
    @GET("departures/{fromCrs}/to/{toCrs}")
    suspend fun getDepartures(
        @Path("fromCrs") fromCrs: String,
        @Path("toCrs") toCrs: String,
        @Query("numRows") numRows: Int = 10,
        @Query("expand") expand: Boolean = true
    ): DeparturesResponse

    /**
     * Get all departures from a station (without destination filter)
     */
    @GET("departures/{crs}")
    suspend fun getAllDepartures(
        @Path("crs") crs: String,
        @Query("numRows") numRows: Int = 10,
        @Query("expand") expand: Boolean = true
    ): DeparturesResponse

    /**
     * Get service details by service ID
     */
    @GET("service/{serviceId}")
    suspend fun getServiceDetails(
        @Path("serviceId") serviceId: String
    ): DeparturesResponse

    companion object {
        const val BASE_URL = "https://national-rail-api.davwheat.dev/"
    }
}
