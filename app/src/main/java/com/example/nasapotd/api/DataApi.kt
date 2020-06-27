package com.example.nasapotd.api

import com.example.nasapotd.model.DataModel
import com.example.nasapotd.model.VimeoModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DataApi {
    @GET("apod")
    fun get_feed(@Query("api_key") api_key: String?): Call<DataModel>?

    @GET("apod")
    fun get_feed_with_date(
        @Query("api_key") api_key: String?,
        @Query("date") date: String?
    ): Call<DataModel>?

    @GET(".")
    fun get_feed_thumbnail(): Call<List<VimeoModel>>?

    companion object {
        const val api_key = "hhLOEStXTcfad68lAeqOkEoVqhzNhTCyaPp5kaaO"
    }
}