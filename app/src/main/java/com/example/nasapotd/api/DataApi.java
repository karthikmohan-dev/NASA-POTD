package com.example.nasapotd.api;

import com.example.nasapotd.model.DataModel;
import com.example.nasapotd.model.VimeoModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DataApi {
    String api_key="hhLOEStXTcfad68lAeqOkEoVqhzNhTCyaPp5kaaO";

    @GET("apod")
    Call<DataModel> get_feed(@Query("api_key") String api_key);

    @GET("apod")
    Call<DataModel> get_feed_with_date(@Query("api_key") String api_key, @Query("date") String date);

    @GET(".")
    Call<List<VimeoModel>> get_feed_thumbnail();
}
