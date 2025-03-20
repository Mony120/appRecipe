package com.example.apprecipe.ui.forum

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.databinding.FragmentForumBinding
import com.example.apprecipe.databinding.FragmentNotificationsBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var postsRef: DatabaseReference
    private lateinit var adapter: PostsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        postsRef = database.getReference("posts")
        setupButtonListeners()
        setupRecyclerView()
        loadPosts()

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser(registrationPrompt)

        view.findViewById<FloatingActionButton>(R.id.fabAddPost).setOnClickListener {
            showAddPostDialog()
        }

    }

    private fun setupRecyclerView() {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.postsRecyclerView)
        val user = auth.currentUser
        adapter = PostsAdapter(
            posts = emptyList(),
            currentUserId = user?.uid ?: "",
            onLongClick = { post -> showDeleteDialog(post) }
        )
        recyclerView?.adapter = adapter
    }

    private fun loadPosts() {
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<Post>()
                for (postSnapshot in snapshot.children) {
                    val post = postSnapshot.getValue(Post::class.java)
                    post?.let { posts.add(it) }
                }

                // Загружаем аватарки для каждого поста
                lifecycleScope.launch {
                    val updatedPosts = posts.map { post ->
                        val avatar = withContext(Dispatchers.IO) {
                            database.getReference("users/${post.authorId}/profile/image").get().await()
                                .getValue(String::class.java)
                        }
                        post.copy(authorAvatar = avatar) // Обновляем аватарку в посте
                    }

                    adapter = PostsAdapter(
                        posts = updatedPosts.sortedByDescending { it.timestamp },
                        currentUserId = auth.currentUser?.uid ?: "",
                        onLongClick = { post -> showDeleteDialog(post) }
                    )
                    view?.findViewById<RecyclerView>(R.id.postsRecyclerView)?.adapter = adapter
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Ошибка загрузки постов", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddPostDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_post, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Новый пост")
            .setView(dialogView)
            .setPositiveButton("Опубликовать") { _, _ ->
                val text = dialogView.findViewById<EditText>(R.id.etPostText).text.toString()
                createPost(text)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun showDeleteDialog(post: Post) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление поста")
            .setMessage("Вы уверены, что хотите удалить этот пост?")
            .setPositiveButton("Удалить") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deletePost(post: Post) {
        postsRef.child(post.postId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Пост удален", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка удаления: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createPost(text: String) {
        val user = auth.currentUser ?: return
        val postRef = postsRef.push()

        lifecycleScope.launch {
            try {
                // Получаем имя пользователя и аватарку
                val userData = withContext(Dispatchers.IO) {
                    database.getReference("users/${user.uid}").get().await()
                }

                val username = userData.child("username").getValue(String::class.java)
                val avatar = userData.child("profile").child("image").getValue(String::class.java)

                val post = Post(
                    postId = postRef.key!!,
                    authorId = user.uid,
                    authorName = username ?: "Аноним",
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    authorAvatar = avatar // Сохраняем аватарку
                )

                postRef.setValue(post)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Пост опубликован!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Ошибка публикации", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Log.e("ForumFragment", "Error creating post", e)
            }
        }
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
        binding.fabAddPost.visibility = View.GONE
        binding.postsRecyclerView.visibility = View.GONE

    }

    private fun showOtherElements() {
        binding.fabAddPost.visibility = View.VISIBLE
        binding.postsRecyclerView.visibility = View.VISIBLE
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
}