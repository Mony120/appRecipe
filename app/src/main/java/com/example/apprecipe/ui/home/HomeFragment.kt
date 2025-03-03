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

    private lateinit var favAdapter: RecipeAdapter
    private lateinit var finishAdapter: RecipeAdapter
    private val favList = mutableListOf<Recipe>()
    private val finishList = mutableListOf<Recipe>()
    private val loadedFavIds = mutableSetOf<String>()
    private val loadedFinishIds = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Инициализация RecyclerView для избранного
        binding.rvFav.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        favAdapter = RecipeAdapter(favList, this)
        binding.rvFav.adapter = favAdapter
        binding.rvFav.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.card_spacing)))

        // Инициализация RecyclerView для приготовленного
        binding.rvFinish.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        finishAdapter = RecipeAdapter(finishList, this)
        binding.rvFinish.adapter = finishAdapter
        binding.rvFinish.addItemDecoration(SpaceItemDecoration(resources.getDimensionPixelSize(R.dimen.card_spacing)))

        setupButtonListeners()
        checkCurrentUser(binding.homeRegistrationPrompt)
        loadFavoriteRecipes()
        loadFinishedRecipes() // Новый метод для загрузки приготовленных

        return root
    }

    private fun loadFavoriteRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().getReference("users/$userId/favorites")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || isDetached) return // Проверка состояния Fragment

                    favList.clear()
                    loadedFavIds.clear()

                    snapshot.children.forEach { child ->
                        val recipeId = child.key
                        recipeId?.let {
                            if (!loadedFavIds.contains(it)) {
                                loadRecipe(it, "favorites")
                            }
                        }
                    }
                    updateUI() // Обновление UI после загрузки данных
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeFragment", "Favorites error: ${error.message}")
                }
            })
    }

    private fun loadFinishedRecipes() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().getReference("users/$userId/finish")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || isDetached) return // Проверка состояния Fragment

                    finishList.clear()
                    loadedFinishIds.clear()

                    snapshot.children.forEach { child ->
                        val recipeId = child.key
                        recipeId?.let {
                            if (!loadedFinishIds.contains(it)) {
                                loadRecipe(it, "finish")
                            }
                        }
                    }
                    updateUI() // Обновление UI после загрузки данных
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeFragment", "Finish error: ${error.message}")
                }
            })
    }

    private fun loadRecipe(recipeId: String, category: String) {
        FirebaseDatabase.getInstance().getReference("recipes/$recipeId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || isDetached) return // Проверка состояния Fragment

                    val recipe = snapshot.getValue(Recipe::class.java)?.apply {
                        id = snapshot.key
                    }

                    recipe?.let {
                        when (category) {
                            "favorites" -> {
                                if (!loadedFavIds.contains(it.id)) {
                                    favList.add(it)
                                    loadedFavIds.add(it.id!!)
                                    favAdapter.notifyDataSetChanged()
                                }
                            }
                            "finish" -> {
                                if (!loadedFinishIds.contains(it.id)) {
                                    finishList.add(it)
                                    loadedFinishIds.add(it.id!!)
                                    finishAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                        updateUI() // Обновление видимости элементов
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HomeFragment", "Recipe load error: ${error.message}")
                }
            })
    }

    private fun updateUI() {
        if (_binding == null) return // Проверка на null

        // Управление видимостью для избранного
        binding.rvFav.visibility = if (favList.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyFav.visibility = if (favList.isEmpty()) View.VISIBLE else View.GONE

        // Управление видимостью для приготовленного
        binding.rvFinish.visibility = if (finishList.isEmpty()) View.GONE else View.VISIBLE
        binding.tvEmptyFinish.visibility = if (finishList.isEmpty()) View.VISIBLE else View.GONE
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

    private fun checkCurrentUser(registrationPrompt: LinearLayout) {
        val currentUser = FirebaseAuth.getInstance().currentUser // Получение текущего пользователя

        if (currentUser == null) {
            showRegistrationPrompt(registrationPrompt) // Отображение подсказки регистрации, если пользователь не найден
            hideOtherElements() // Скрытие остальных элементов
        } else {
            registrationPrompt.visibility = View.GONE // Скрытие подсказки, если пользователь найден
            showOtherElements() // Показ остальных элементов
        }
    }

    private fun hideOtherElements() {
        binding.rvFav.visibility = View.GONE
        binding.rvFinish.visibility = View.GONE
        binding.tvEmptyFav.visibility = View.GONE
        binding.tvEmptyFinish.visibility = View.GONE
    }

    private fun showOtherElements() {
        binding.rvFav.visibility = View.VISIBLE
        binding.rvFinish.visibility = View.VISIBLE
        updateUI() // Обновление видимости элементов
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