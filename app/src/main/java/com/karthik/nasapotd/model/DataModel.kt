package com.karthik.nasapotd.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DataModel {
    @SerializedName("date")
    @Expose
    var date: String? = null

    @SerializedName("explanation")
    @Expose
    val explanation: String? = null

    @SerializedName("hdurl")
    @Expose
    val hdurl: String? = null

    @SerializedName("media_type")
    @Expose
    val mediaType: String? = null

    @SerializedName("title")
    @Expose
    val title: String? = null

    @SerializedName("url")
    @Expose
    val url: String? = null

}