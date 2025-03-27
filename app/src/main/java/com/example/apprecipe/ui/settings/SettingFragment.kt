package com.example.apprecipe.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.apprecipe.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var switch: Switch
    private lateinit var imageViewProfile: ImageView
    private lateinit var imageViewBear: ImageView
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Инициализация SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // Инициализация Switch
        switch = view.findViewById(R.id.themeSwitch)
        setupThemeSwitch()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("users")

        // Инициализация ImageView с медведем
        imageViewBear = view.findViewById(R.id.bear)

        val emailTextView: TextView = view.findViewById(R.id.tv_email)
        val loginTextView: TextView = view.findViewById(R.id.tv_name)
        val btnLogout: Button = view.findViewById(R.id.btn_logout)
        imageViewProfile = view.findViewById(R.id.imageView)

        val btnChangeName: Button = view.findViewById(R.id.btn_ChangeName)
        btnChangeName.setOnClickListener { showChangeNameDialog() }

        val btnChangeProf: Button = view.findViewById(R.id.btn_ChangeProfile)
        btnChangeProf.setOnClickListener { openGallery() }

        // Проверка текущей темы при создании
        updateBearVisibility()

        val user = auth.currentUser

        if (user != null) {
            if (user.isEmailVerified) {
                emailTextView.text = "${user.email}"
                getUserName(user.uid, loginTextView)
                loadImageFromFirebase(user.uid)
            } else {
                Log.d("SettingFragment", "Email не подтвержден")
                Toast.makeText(context, "Пожалуйста, подтвердите email.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.navigation_login)
            }
        } else {
            findNavController().navigate(R.id.navigation_login)
        }

        btnLogout.setOnClickListener { logOut() }
    }

    private fun setupThemeSwitch() {
        // Восстановление состояния темы
        val savedTheme = sharedPreferences.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        switch.isChecked = savedTheme == AppCompatDelegate.MODE_NIGHT_YES

        // Обработчик переключения
        switch.setOnCheckedChangeListener { _, isChecked ->
            val newThemeMode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(newThemeMode)
            sharedPreferences.edit().putInt("ThemeMode", newThemeMode).apply()
            updateBearVisibility()
        }
    }

    private fun updateBearVisibility() {
        val currentNightMode = AppCompatDelegate.getDefaultNightMode()
        imageViewBear.visibility = when (currentNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> View.GONE
            AppCompatDelegate.MODE_NIGHT_NO -> View.VISIBLE
            else -> View.VISIBLE // Для системной темы по умолчанию
        }
    }

    // Остальные методы без изменений
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            imageUri = data.data
            imageViewProfile.setImageURI(imageUri)
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                saveImageToFirebase(bitmap)
            }
        }
    }

    private fun getUserName(userId: String, loginTextView: TextView) {
        lifecycleScope.launch {
            try {
                val name = withContext(Dispatchers.IO) {
                    val dataSnapshot = myRef.child(userId).get().await()
                    dataSnapshot.child("username").getValue(String::class.java) ?: "Неизвестно"
                }
                loginTextView.text = name
            } catch (e: Exception) {
                Log.w("SettingFragment", "Ошибка при получении данных", e)
            }
        }
    }

    private fun saveImageToFirebase(bitmap: Bitmap) {
        val userId = auth.currentUser?.uid ?: return
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)

        myRef.child(userId).child("profile").child("image").setValue(encodedImage)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Изображение сохранено", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("SettingFragment", "Ошибка сохранения", e)
            }
    }

    private fun loadImageFromFirebase(userId: String) {
        myRef.child(userId).child("profile").child("image").get()
            .addOnSuccessListener { dataSnapshot ->
                dataSnapshot.getValue(String::class.java)?.let { encodedImage ->
                    val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageViewProfile.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener { e ->
                Log.w("SettingFragment", "Ошибка загрузки", e)
            }
    }

    private fun showChangeNameDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_name, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.etNewName)

        AlertDialog.Builder(requireContext())
            .setTitle("Смена имени")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val newName = editTextName.text.toString().trim()
                if (newName.isNotEmpty()) updateUserName(newName)
                else Toast.makeText(context, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUserName(newName: String) {
        val userId = auth.currentUser?.uid ?: return
        myRef.child(userId).child("username").setValue(newName)
            .addOnSuccessListener {
                view?.findViewById<TextView>(R.id.tv_name)?.text = newName
                Toast.makeText(context, "Имя изменено", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logOut() {
        auth.signOut()
        findNavController().navigate(R.id.navigation_login)
    }
}