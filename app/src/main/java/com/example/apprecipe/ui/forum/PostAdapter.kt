package com.example.apprecipe.ui.forum

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostsAdapter(
    private val posts: List<Post>,
    private val currentUserId: String,
    private val onLongClick: (Post) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)

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
        holder.tvAuthor.text = post.authorName
        holder.tvText.text = post.text
        holder.tvTimestamp.text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            .format(Date(post.timestamp))



        // Включаем долгое нажатие только для своих постов
        holder.itemView.isLongClickable = post.authorId == currentUserId
    }

    override fun getItemCount() = posts.size
}