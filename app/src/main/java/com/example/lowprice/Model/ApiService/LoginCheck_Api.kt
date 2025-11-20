package com.example.lowprice.Model.ApiService

import com.example.lowprice.Model.LoginRequest
import com.example.lowprice.Model.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginCheck_Api {
    @POST("/login")
    fun loginUser(@Body loginRequest: LoginRequest): Call<LoginResponse>
}