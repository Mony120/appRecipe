package com.example.apprecipe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecipeAdapter(
    private var recipeList: MutableList<Recipe>, // Изменено на MutableList
    private val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    // Добавлен метод для обновления списка
    fun updateList(newList: List<Recipe>) {
        recipeList.clear()
        recipeList.addAll(newList)
        notifyDataSetChanged()
    }

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name_recipe)
        val timeTextView: TextView = itemView.findViewById(R.id.time)
        val imageView: ImageView = itemView.findViewById(R.id.header_image)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.favorite_button)
    }

    interface OnItemClickListener {
        fun onItemClick(recipe: Recipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.nameTextView.text = recipe.name
        holder.timeTextView.text = recipe.time

        // Загрузка изображения с обработкой ошибок
        Glide.with(holder.itemView.context)
            .load(recipe.url)
            .transition(DrawableTransitionOptions.withCrossFade())
            //.placeholder(R.drawable.placeholder_image)
            //.error(R.drawable.error_image)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(recipe)
        }

        // Обновление состояния кнопки с проверкой контекста
        if (holder.itemView.context != null) {
            updateFavoriteButtonState(holder.favoriteButton, recipe.id)
        }

        holder.favoriteButton.setOnClickListener {
            recipe.id?.let { id ->
                toggleFavorite(id, holder.favoriteButton)
            }
        }
    }

    override fun getItemCount(): Int = recipeList.size

    private fun toggleFavorite(recipeId: String, button: ImageButton) {
        val userId = auth.currentUser?.uid ?: return
        val favoritesRef = database.getReference("users/$userId/favorites")

        favoritesRef.child(recipeId).addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        favoritesRef.child(recipeId).removeValue()
                    } else {
                        favoritesRef.child(recipeId).setValue(true)
                    }
                    updateFavoriteButtonState(button, recipeId)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("RecipeAdapter", "Database error: ${databaseError.message}")
                }
            })
    }

    private fun updateFavoriteButtonState(button: ImageButton, recipeId: String?) {
        val userId = auth.currentUser?.uid ?: run {
            button.setImageResource(R.drawable.baseline_favorite_border_24)
            return
        }

        recipeId ?: run {
            button.setImageResource(R.drawable.baseline_favorite_border_24)
            return
        }

        database.getReference("users/$userId/favorites/$recipeId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val isFavorite = dataSnapshot.exists()
                    val icon = if (isFavorite) {
                        R.drawable.baseline_favorite
                    } else {
                        R.drawable.baseline_favorite_border_24
                    }

                    // Безопасное обновление иконки
                    button.post {
                        button.setImageResource(icon)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("RecipeAdapter", "Favorite check failed: ${databaseError.message}")
                }
            })
    }
}