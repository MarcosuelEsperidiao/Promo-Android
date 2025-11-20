package com.example.lowprice.Model

data class Product_Add(
    val location: String,
    val locario: String,
    val price: Float,
    val image: String?,
    val userName: String?,  // Novo campo para o nome de usuário
    val profileImage: String?,  // Novo campo para a imagem de perfil
    val description: String,
    val timestamp: String
)

