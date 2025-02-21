package com.example.apprecipe

data class Recipe(
    var url: String? = null,    // URL изображения
    var name: String? = null,   // Название рецепта
    var time: String? = null,   // Время приготовления
    var cooking: String? = null, // Инструкция по приготовлению
    var ingredients: String? = null, // Ингредиенты
    var kbzy: String? = null,     // Дополнительная информация
    var id: String? = null
)