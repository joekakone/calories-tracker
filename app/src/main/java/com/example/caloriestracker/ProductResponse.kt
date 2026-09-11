package com.example.caloriestracker

import com.google.gson.annotations.SerializedName

data class ProductResponse(
    val status: Int,
    val product: ProductInfo?
)

data class ProductInfo(
    @SerializedName("product_name") val productName: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    @SerializedName("energy-kcal_100g") val calories100g: Double?,
    @SerializedName("carbohydrates_100g") val carbs100g: Double?,
    @SerializedName("proteins_100g") val proteins100g: Double?,
    @SerializedName("fat_100g") val fat100g: Double?
)
