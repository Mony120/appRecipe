package com.example.apprecipe.ui.forum

import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostsAdapter(
    private val posts: List<Post>,
    private val currentUserId: String,
    private val onLongClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val database = FirebaseDatabase.getInstance() // Инициализация базы данных

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar) // ImageView для аватарки

        var avatarListener: ValueEventListener? = null // Слушатель для аватарки

        fun bind(post: Post) {
            tvAuthor.text = post.authorName
            tvText.text = post.text
            tvTimestamp.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                .format(Date(post.timestamp))

            // Удаляем предыдущий слушатель, если он есть
            avatarListener?.let {
                database.getReference("users/${post.authorId}/profile/image").removeEventListener(it)
            }

            // Добавляем слушатель для обновления аватарки в реальном времени
            avatarListener = database.getReference("users/${post.authorId}/profile/image")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val avatar = snapshot.getValue(String::class.java)
                        avatar?.let {
                            val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            ivAvatar.setImageBitmap(bitmap)
                        } ?: run {
                            ivAvatar.setImageResource(R.drawable.user) // Дефолтная аватарка
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("PostsAdapter", "Ошибка загрузки аватарки", error.toException())
                    }
                })
        }

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = posts[position]
                    if (post.authorId == currentUserId) {
                        onLongClick(post)
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post) // Используем метод bind для привязки данных
    }

    override fun getItemCount() = posts.size

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        // Очищаем слушатель при переиспользовании ViewHolder
        val post = posts[holder.adapterPosition]
        holder.avatarListener?.let {
            database.getReference("users/${post.authorId}/profile/image").removeEventListener(it)
        }
    }
}