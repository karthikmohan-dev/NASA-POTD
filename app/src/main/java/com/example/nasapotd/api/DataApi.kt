package com.example.nasapotd.api

import com.example.nasapotd.model.DataModel
import com.example.nasapotd.model.VimeoModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DataApi {
    @GET("apod")
    fun getFeed(@Query("api_key") api_key: String?): Call<DataModel>?

    @GET("apod")
    fun getFeedWithDate(
        @Query("api_key") api_key: String?,
        @Query("date") date: String?
    ): Call<DataModel>?

    @GET(".")
    fun getFeedThumbnail(): Call<List<VimeoModel>>?

    companion object {
        const val api_key = "hhLOEStXTcfad68lAeqOkEoVqhzNhTCyaPp5kaaO"
    }
}