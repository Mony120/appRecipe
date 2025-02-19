package com.example.apprecipe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecipeAdapter(private val recipeList: List<Recipe>) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.name_recipe)
        val timeTextView: TextView = itemView.findViewById(R.id.time)
        val imageView: ImageView = itemView.findViewById(R.id.header_image)
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
    }

    override fun getItemCount(): Int {
        return recipeList.size
    }
}