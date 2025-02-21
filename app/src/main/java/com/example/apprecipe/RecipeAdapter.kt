package com.example.apprecipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecipeAdapter(private val recipeList: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name_recipe)
        val timeTextView: TextView = itemView.findViewById(R.id.time)
        val imageView: ImageView = itemView.findViewById(R.id.header_image)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button) // Добавляем кнопку
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.nameTextView.text = recipe.name
        holder.timeTextView.text = recipe.time

        // Используйте Glide для загрузки изображения
        Glide.with(holder.itemView.context)
            .load(recipe.url)
            .into(holder.imageView)

        // Установите состояние кнопки "Избранное"
        updateFavoriteButtonState(holder.favoriteButton, recipe.id)

        // Обработка нажатия на кнопку "Избранное"
        holder.favoriteButton.setOnClickListener {
            recipe.id?.let { id ->
                toggleFavorite(id, holder.favoriteButton) // Передаем id только если он не null
            }
        }
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }

    private fun toggleFavorite(recipeId: String, button: ImageButton) {
        val userId = FirebaseAuth.getInstance().currentUser ?.uid
        if (userId != null) {
            val favoritesRef = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")
            favoritesRef.child(recipeId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Если рецепт уже в избранном, удаляем его
                        favoritesRef.child(recipeId).removeValue()
                    } else {
                        // Если рецепта нет в избранном, добавляем его
                        favoritesRef.child(recipeId).setValue(true)
                    }
                    updateFavoriteButtonState(button, recipeId) // Обновляем состояние кнопки
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Обработка ошибок
                }
            })
        }
    }

    private fun updateFavoriteButtonState(button: ImageButton, recipeId: String?) {
        val userId = FirebaseAuth.getInstance().currentUser ?.uid
        if (userId != null && recipeId != null) {
            val favoritesRef = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")
            favoritesRef.child(recipeId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val isFavorite = dataSnapshot.exists() // Проверка, существует ли рецепт в избранном
                    button.setImageResource(if (isFavorite) R.drawable.baseline_favorite else R.drawable.baseline_favorite_border_24) // Изменение иконки
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Обработка ошибок
                }
            })
        } else {
            // Установите иконку по умолчанию, если id или userId null
            button.setImageResource(R.drawable.baseline_favorite_border_24)
        }
    }
}