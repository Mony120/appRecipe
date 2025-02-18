package com.example.apprecipe.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.Recipe
import com.example.apprecipe.RecipeAdapter
import com.example.apprecipe.databinding.FragmentNotificationsBinding
import com.google.firebase.database.*

class NotificationsFragment : Fragment() {

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
        recipeAdapter = RecipeAdapter(recipeList)
        recyclerView.adapter = recipeAdapter

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
