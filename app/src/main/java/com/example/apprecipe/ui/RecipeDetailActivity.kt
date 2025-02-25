package com.example.apprecipe.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.apprecipe.R
import com.example.apprecipe.Recipe

class RecipeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail) // Убедитесь, что это правильный макет

        if (savedInstanceState == null) {
            val RecipeDetailFragment = RecipeDetailFragment()

            // Получаем переданную книгу из Intent
            val selectedRecipe = intent.getSerializableExtra("selected_recipe") as? Recipe

            // Передаем данные в фрагмент через аргументы
            val bundle = Bundle().apply {
                putSerializable("selected_recipe", selectedRecipe)
            }
            RecipeDetailFragment.arguments = bundle

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RecipeDetailFragment) // Убедитесь, что id совпадает
                .commit()
        }
    }
}