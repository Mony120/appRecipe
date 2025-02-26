package com.example.apprecipe.ui.notifications

import android.content.Intent
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
import com.example.apprecipe.databinding.FragmentNotificationsBinding
import com.example.apprecipe.ui.RecipeDetailActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsFragment : Fragment(), RecipeAdapter.OnItemClickListener {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        recyclerView = binding.recycleView // Убедитесь, что у вас есть RecyclerView в вашем XML
        recyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter(recipeList, this)
        recyclerView.adapter = recipeAdapter

        setupButtonListeners()

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser (registrationPrompt)

        fetchRecipesFromFirebase()

        return root
    }

    private fun fetchRecipesFromFirebase() {
        val recipesRef = FirebaseDatabase.getInstance().getReference("recipes")

        recipesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                recipeList.clear()
                for (recipeSnapshot in dataSnapshot.children) {
                    val recipe = recipeSnapshot.getValue(Recipe::class.java)
                    if (recipe != null) {
                        recipeList.add(recipe)
                    }
                }
                recipeAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("NotificationFragment", "Load Database Error: ${databaseError.message}")
            }
        })
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
        binding.scroll.visibility = View.GONE // Скрыть ScrollView
        binding.recycleView.visibility = View.GONE // Скрыть ScrollView
        binding.cdView.visibility = View.GONE // Скрыть другие элементы, если необходимо
    }

    private fun showOtherElements() {
        binding.scroll.visibility = View.VISIBLE // Показать ScrollView
        binding.recycleView.visibility = View.VISIBLE // Показать ScrollView
        binding.cdView.visibility = View.VISIBLE // Показать другие элементы, если они скрыты
    }

    private fun showRegistrationPrompt(registrationPrompt: LinearLayout) {
        registrationPrompt.visibility = View.VISIBLE // Отображение подсказки регистрации
        val slideIn = AnimationUtils.loadAnimation(requireContext(), com.example.apprecipe.R.anim.slide_in_bottom) // Загрузка анимации
        registrationPrompt.startAnimation(slideIn) // Запуск анимации
    }

    private fun setupButtonListeners() {
        binding.homeRegistrationBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_setting) // Переход к фрагменту регистрации
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
}
