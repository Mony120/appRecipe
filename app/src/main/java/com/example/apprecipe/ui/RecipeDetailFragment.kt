package com.example.apprecipe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.fragment.app.Fragment
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.apprecipe.MainActivity
import com.example.apprecipe.Recipe
import com.example.apprecipe.databinding.FragmentRecipeDetailBinding

class RecipeDetailFragment : Fragment() {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация ViewBinding
        _binding = FragmentRecipeDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Скрываем нижнюю навигацию в MainActivity
        (activity as? MainActivity)?.hideBottomNavigation()

        // Получаем переданный рецепт из аргументов
        arguments?.getSerializable("selected_recipe")?.let { recipe ->
            val selectedRecipe = recipe as? Recipe ?: return
            updateUI(selectedRecipe)
        }
    }

    private fun updateUI(recipe: Recipe) {
        // Загружаем изображение с помощью Glide
        Glide.with(this)
            .load(recipe.url)
            .into(binding.imageView)

        // Устанавливаем название рецепта
        binding.nameRecipe.text = recipe.name

        // Если есть другие поля, например, время приготовления, добавьте их здесь
        // binding.timeTextView.text = recipe.time
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ViewBinding
        _binding = null
    }
}