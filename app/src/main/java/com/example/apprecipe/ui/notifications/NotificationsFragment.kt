package com.example.apprecipe.ui.notifications

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.Recipe
import com.example.apprecipe.RecipeAdapter
import com.example.apprecipe.databinding.FragmentNotificationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Locale

class NotificationsFragment : Fragment(), RecipeAdapter.OnItemClickListener {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()
    private val allRecipes = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.recycleView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter(recipeList, this)
        recyclerView.adapter = recipeAdapter

        setupButtonListeners()
        setupFilterButtons()
        setupSearchView() // Добавлен вызов метода настройки SearchView

        val spacingInPixels = resources.getDimensionPixelSize(R.dimen.card_spacing)
        recyclerView.addItemDecoration(SpaceItemDecoration(spacingInPixels))

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser(registrationPrompt)

        fetchRecipesFromFirebase()

        return root
    }

    // Добавлен метод для настройки SearchView
    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { performSearch(it) }
                return true
            }
        })
    }

    // Метод для выполнения поиска
    private fun performSearch(query: String) {
        val searchQuery = query.lowercase(Locale.getDefault()).trim()

        if (searchQuery.isEmpty()) {
            showAllRecipes()
            return
        }

        val filteredList = allRecipes.filter { recipe ->
            // Поиск по названию
            recipe.name?.lowercase(Locale.getDefault())?.contains(searchQuery) == true ||
                    // Поиск по жанрам
                    recipe.genre?.any { it.lowercase(Locale.getDefault()).contains(searchQuery) } == true
        }

        updateRecipeList(filteredList)
    }

    private fun setupFilterButtons() {
        binding.btnAll.setOnClickListener { showAllRecipes() }
        binding.btnSoup.setOnClickListener { filterRecipesByCategory("Суп") }
        binding.btnSalad.setOnClickListener { filterRecipesByCategory("Салат") }
        binding.btnDessert.setOnClickListener { filterRecipesByCategory("Десерт") }
        binding.btnGarnir.setOnClickListener { filterRecipesByCategory("Гарнир") }
        binding.btnPech.setOnClickListener { filterRecipesByCategory("Выпечка") }
        binding.btnMeat.setOnClickListener { filterRecipesByCategory("Мясо") }
    }

    private fun fetchRecipesFromFirebase() {
        val recipesRef = FirebaseDatabase.getInstance().getReference("recipes")

        recipesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allRecipes.clear()
                recipeList.clear()
                for (recipeSnapshot in dataSnapshot.children) {
                    val recipe = recipeSnapshot.getValue(Recipe::class.java)
                    recipe?.let {
                        it.id = recipeSnapshot.key
                        allRecipes.add(it)
                        recipeList.add(it)
                    }
                }
                recipeAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("NotificationFragment", "Load Database Error: ${databaseError.message}")
            }
        })
    }

    private fun filterRecipesByCategory(category: String) {
        val filteredList = allRecipes.filter { recipe ->
            recipe.genre?.any { it.equals(category, ignoreCase = true) } == true
        }
        updateRecipeList(filteredList)
    }

    private fun showAllRecipes() {
        updateRecipeList(allRecipes)
    }

    private fun updateRecipeList(newList: List<Recipe>) {
        recipeList.clear()
        recipeList.addAll(newList)
        recipeAdapter.notifyDataSetChanged()
        recyclerView.scrollToPosition(0)
    }

    private fun checkCurrentUser(registrationPrompt: LinearLayout) {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
            showRegistrationPrompt(registrationPrompt)
            hideOtherElements()
        } else {
            registrationPrompt.visibility = View.GONE
            showOtherElements()
        }
    }

    private fun hideOtherElements() {
        binding.scroll.visibility = View.GONE
        binding.recycleView.visibility = View.GONE
        binding.cdView.visibility = View.GONE
    }

    private fun showOtherElements() {
        binding.scroll.visibility = View.VISIBLE
        binding.recycleView.visibility = View.VISIBLE
        binding.cdView.visibility = View.VISIBLE
    }

    private fun showRegistrationPrompt(registrationPrompt: LinearLayout) {
        registrationPrompt.visibility = View.VISIBLE
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom)
        registrationPrompt.startAnimation(slideIn)
    }

    private fun setupButtonListeners() {
        binding.homeRegistrationBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_setting)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(recipe: Recipe) {
        val bundle = Bundle().apply {
            putSerializable("selected_recipe", recipe)
        }
        findNavController().navigate(R.id.action_notificationsFragment_to_recipeDetailFragment, bundle)
    }
    class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            outRect.bottom = space // Установка отступа снизу
        }
    }
}