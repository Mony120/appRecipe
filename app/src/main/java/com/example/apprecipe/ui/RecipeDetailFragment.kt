package com.example.apprecipe.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.apprecipe.MainActivity
import com.example.apprecipe.R
import com.example.apprecipe.Recipe
import com.example.apprecipe.databinding.FragmentRecipeDetailBinding


class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity as? MainActivity)?.hideBottomNavigation()

        arguments?.getSerializable("selected_recipe")?.let { recipe ->
            val selectedRecipe = recipe as? Recipe ?: return
            updateUI(selectedRecipe)



            // Устанавливаем обработчик для кнопки "Назад"
            //binding.backButton.setOnClickListener { navigateBack() }
        }
    }
    private fun updateUI(recipe: Recipe) {
        Glide.with(this)
            .load(recipe.url)
            .into(binding.imageView)

        binding.nameRecipe.text = recipe.name
        //binding.author.text = book.author
        //  binding.description.text = book.description

    }
}