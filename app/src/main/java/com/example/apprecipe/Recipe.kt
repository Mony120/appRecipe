package com.example.apprecipe

import java.io.Serializable

data class Recipe(
    var url: String? = null,    // URL изображения
    var name: String? = null,   // Название рецепта
    var time: String? = null,   // Время приготовления
    var cooking: String? = null, // Инструкция по приготовлению
    var ingridients: String? = null, // Ингредиенты
    var kbzy: String? = null,
    val genre: List<String>? = null,
    var id: String? = null
): Serializable