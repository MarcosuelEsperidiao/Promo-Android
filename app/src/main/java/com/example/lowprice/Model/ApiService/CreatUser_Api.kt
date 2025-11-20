package com.example.lowprice.Model.ApiService

import com.example.lowprice.Model.CreatUser
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface CreatUser_Api {
    @POST("/users")  // Altere a URL para o endpoint correto
    fun createUser(@Body CreatUser: CreatUser): Call<Void>

}