package com.karthik.nasapotd.api

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.karthik.nasapotd.model.DataModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
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

    @POST("translate")
    fun getTranslate(
        @Query("key") key: String?,
        @Query("lang") lang: String?,
        @Query("text") text: String?
    ): Call<TransModel>?

    class TransModel {
        @SerializedName("text")
        @Expose
        val texter: List<String>? = null
    }

    class VimeoModel {
        @SerializedName("thumbnail_large")
        @Expose
        val thumbnailLarge: String? = null
    }

    companion object {
        const val api_key = "hhLOEStXTcfad68lAeqOkEoVqhzNhTCyaPp5kaaO"
        const val trans_api_key = "trnsl.1.1.20170425T085917Z.580fec9ed721d387.9caf5bd2514ca1581b95de321974dce826ef5064"
        const val ad_id = "ca-app-pub-2747296886141297/7705354849"
    }
}