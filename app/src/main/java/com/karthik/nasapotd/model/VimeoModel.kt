package com.karthik.nasapotd.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class VimeoModel {
    @SerializedName("thumbnail_large")
    @Expose
    val thumbnail_large: String? = null

    @SerializedName("thumbnail_medium")
    @Expose
    val thumbnail_medium: String? = null

    @SerializedName("thumbnail_small")
    @Expose
    val thumbnail_small: String? = null

}