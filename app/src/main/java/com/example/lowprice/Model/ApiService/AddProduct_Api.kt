package com.example.lowprice.Model.ApiService

import com.example.lowprice.Model.Product_Add
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
interface AddProduct_Api {
    @POST("/products")
    fun addProduct(@Body productAdd: Product_Add): Call<Void>

    @GET("/products")
    fun getProducts(): Call<List<Product_Add>>

}
