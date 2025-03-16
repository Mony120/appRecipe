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

class PostsAdapter(private val posts: List<Post>) :
    RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
        val tvText: TextView = view.findViewById(R.id.tvText)
        val ivPostImage: ImageView = view.findViewById(R.id.ivPostImage)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
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

        post.image?.let {
            val decodedBytes = Base64.decode(it, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            holder.ivPostImage.setImageBitmap(bitmap)
        } ?: run {
            holder.ivPostImage.visibility = View.GONE
        }
    }

    override fun getItemCount() = posts.size
}