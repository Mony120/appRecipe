package com.example.apprecipe.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.Recipe
import com.example.apprecipe.RecipeAdapter
import com.example.apprecipe.databinding.FragmentHomeBinding
import com.example.apprecipe.ui.RecipeDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment(), RecipeAdapter.OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.homeRecycleView
        recipeAdapter = RecipeAdapter(recipeList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = recipeAdapter

        loadFavoriteRecipes()

        return root
    }

    private fun loadFavoriteRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser ?.uid
        Log.d("HomeFragment", "User  ID: $userId") // Логируем User ID
        if (userId != null) {
            val favoritesRef = FirebaseDatabase.getInstance().getReference("users/$userId/favorites")
            favoritesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    recipeList.clear() // Очистите список перед добавлением новых данных
                    Log.d("HomeFragment", "Избранные рецепты загружены: ${dataSnapshot.childrenCount}")
                    if (dataSnapshot.exists()) {
                        for (snapshot in dataSnapshot.children) {
                            val recipeId = snapshot.key
                            Log.d("HomeFragment", "ID рецепта: $recipeId") // Логируем ID рецепта
                            recipeId?.let {
                                getRecipeById(it)
                            }
                        }
                    } else {
                        Log.d("HomeFragment", "Нет избранных рецептов.")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("HomeFragment", "Ошибка загрузки избранных рецептов: ${databaseError.message}")
                }
            })
        } else {
            Log.d("HomeFragment", "Пользователь не авторизован.")
        }
    }

    private fun getRecipeById(recipeId: String) {
        val recipesRef = FirebaseDatabase.getInstance().getReference("recipes")
        recipesRef.orderByChild("id").equalTo(recipeId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        val recipe = snapshot.getValue(Recipe::class.java)
                        Log.d("HomeFragment", "Рецепт загружен: ${recipe?.name}") // Логируем загруженный рецепт
                        recipe?.let {
                            recipeList.add(it)
                            recipeAdapter.notifyDataSetChanged() // Уведомляем адаптер об изменениях
                        }
                    }
                } else {
                    Log.d("HomeFragment", "Рецепт не найден по ID: $recipeId")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("HomeFragment", "Ошибка получения рецепта: ${databaseError.message}")
            }
        })
    }
    override fun onItemClick(recipe: Recipe) {
        val bundle = Bundle().apply {
            putSerializable("selected_recipe", recipe)
        }
        findNavController().navigate(R.id.action_homeFragment_to_RecipeDetailFragment, bundle)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}