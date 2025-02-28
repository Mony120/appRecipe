package com.example.apprecipe.ui.home

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.Recipe
import com.example.apprecipe.RecipeAdapter
import com.example.apprecipe.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment(), RecipeAdapter.OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()
    private val loadedRecipeIds = mutableSetOf<String>() // Для отслеживания загруженных рецептов

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.homeRecycleView
        recipeAdapter = RecipeAdapter(recipeList, this)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = recipeAdapter
        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing)
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        setupButtonListeners()

        loadFavoriteRecipes()

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser (registrationPrompt)

        return root
    }

    private fun loadFavoriteRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("HomeFragment", "User ID: $userId")

        userId?.let { uid ->
            val favoritesRef = FirebaseDatabase.getInstance()
                .getReference("users/$uid/favorites")

            favoritesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    recipeList.clear()
                    loadedRecipeIds.clear()
                    Log.d("HomeFragment", "Found ${dataSnapshot.childrenCount} favorites")

                    dataSnapshot.children.forEach { snapshot ->
                        val recipeId = snapshot.key
                        Log.d("HomeFragment", "Processing recipe ID: $recipeId")

                        recipeId?.let {
                            if (!loadedRecipeIds.contains(it)) {
                                getRecipeById(it)
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("HomeFragment", "Error loading favorites: ${databaseError.message}")
                }
            })
        } ?: run {
            Log.d("HomeFragment", "User not authenticated")
        }
    }

    private fun getRecipeById(recipeId: String) {
        val recipeRef = FirebaseDatabase.getInstance()
            .getReference("recipes/$recipeId")

        recipeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val recipe = dataSnapshot.getValue(Recipe::class.java)?.apply {
                    id = dataSnapshot.key // Устанавливаем ID из ключа Firebase
                }

                recipe?.let {
                    if (!loadedRecipeIds.contains(it.id)) {
                        recipeList.add(it)
                        loadedRecipeIds.add(it.id!!)
                        recipeAdapter.notifyDataSetChanged()
                        Log.d("HomeFragment", "Added recipe: ${it.name}")
                    }
                } ?: Log.d("HomeFragment", "Recipe not found: $recipeId")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("HomeFragment", "Error loading recipe: ${databaseError.message}")
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

    class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = space
        }
    }
    private fun checkCurrentUser (registrationPrompt: LinearLayout) {
        val currentUser  = FirebaseAuth.getInstance().currentUser  // Получение текущего пользователя

        if (currentUser  == null) {
            showRegistrationPrompt(registrationPrompt) // Отображение подсказки регистрации, если пользователь не найден
            hideOtherElements() // Скрытие остальных элементов
        } else {
            registrationPrompt.visibility = View.GONE // Скрытие подсказки, если пользователь найден
            showOtherElements() // Показ остальных элементов
        }
    }

    private fun hideOtherElements() {

        binding.homeRecycleView.visibility = View.GONE // Скрыть другие элементы, если необходимо
    }

    private fun showOtherElements() {
        binding.homeRecycleView.visibility = View.VISIBLE // Показать другие элементы, если они скрыты
    }

    private fun showRegistrationPrompt(registrationPrompt: LinearLayout) {
        registrationPrompt.visibility = View.VISIBLE // Отображение подсказки регистрации
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom) // Загрузка анимации
        registrationPrompt.startAnimation(slideIn) // Запуск анимации
    }

    private fun setupButtonListeners() {
        binding.homeRegistrationBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_setting) // Переход к фрагменту регистрации
        }
    }
}