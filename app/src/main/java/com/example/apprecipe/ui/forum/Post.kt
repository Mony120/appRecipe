package com.example.apprecipe.ui.forum

data class Post(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val image: String? = null,
    val timestamp: Long = 0
)