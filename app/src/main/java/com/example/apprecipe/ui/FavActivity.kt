package com.example.apprecipe.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.Recipe
import com.example.apprecipe.RecipeAdapter
import com.example.apprecipe.ui.notifications.NotificationsFragment.SpaceItemDecoration

class FavActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fav)

        // Инициализируем RecyclerView
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(baseContext)
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing) // Получаем значение из ресурсов
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))
        // Создаем адаптер с обработчиком клика
        adapter = RecipeAdapter(recipeList, object : RecipeAdapter.OnItemClickListener {
            override fun onItemClick(recipe: Recipe) {

                val intent = Intent(this@FavActivity, RecipeDetailActivity::class.java)
                intent.putExtra("selected_recipe", recipe) // Передаем выбранную книгу
                startActivity(intent) // Запускаем активность
            }
        })

        recyclerView.adapter = adapter

        // Получаем переданные данные
        val recipes: ArrayList<Recipe>? = intent.getSerializableExtra("fav_recipes") as? ArrayList<Recipe>

        // Используем полученные данные
        if (recipes != null) {
            recipeList.addAll(recipes)
            adapter.notifyDataSetChanged() // Обновляем адаптер
        }
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // Возвращает на предыдущий экран
        }

    }
}