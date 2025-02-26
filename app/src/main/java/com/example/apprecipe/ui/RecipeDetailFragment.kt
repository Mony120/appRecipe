package com.example.apprecipe.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.apprecipe.MainActivity
import com.example.apprecipe.Recipe
import com.example.apprecipe.databinding.FragmentRecipeDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

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

            // Обработка нажатия на кнопку "Избранное"
            binding.favoriteButton.setOnClickListener {
                selectedRecipe.id?.let { id ->
                    toggleFavorite(id, binding.favoriteButton) // Передаем id только если он не null
                }
            }

            // Обновляем состояние кнопки "Избранное"
            selectedRecipe.id?.let { id ->
                updateFavoriteButtonState(binding.favoriteButton, id)
            }
        }
    }

    private fun updateUI(recipe: Recipe) {
        // Загружаем изображение с помощью Glide
        Glide.with(this)
            .load(recipe.url)
            .into(binding.imageView)

        // Устанавливаем данные рецепта
        binding.nameRecipe.text = recipe.name
        binding.ingTextView.text = recipe.ingredients
        binding.cookTextView.text = recipe.cooking
        binding.kbzyTextView.text = recipe.kbzy

        // Обработка нажатия на кнопку "Назад"
        binding.backButton.setOnClickListener { navigateBack() }
    }

    private fun navigateBack() {
        requireActivity().onBackPressed()
    }

    private fun toggleFavorite(recipeId: String, button: ImageButton) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Показываем нижнюю навигацию в MainActivity
        (activity as? MainActivity)?.showBottomNavigation()
        _binding = null
    }
}