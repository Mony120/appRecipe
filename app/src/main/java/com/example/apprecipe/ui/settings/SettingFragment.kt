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
    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_setting, container, false)

        // Инициализация Switch
        switch = view.findViewById(R.id.themeSwitch)

        // Восстановление состояния Switch из SharedPreferences
        val sharedPreferences = requireContext().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val savedTheme = sharedPreferences.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        switch.isChecked = savedTheme == AppCompatDelegate.MODE_NIGHT_YES

        // Обработчик переключения Switch
        switch.setOnCheckedChangeListener { _, isChecked ->
            val newThemeMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(newThemeMode)
            sharedPreferences.edit().putInt("ThemeMode", newThemeMode).apply()
            // Обновление UI без перезапуска активности
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        myRef = database.getReference("users")

        val emailTextView: TextView = view.findViewById(R.id.tv_email)
        val loginTextView: TextView = view.findViewById(R.id.tv_name)
        val btnLogout: Button = view.findViewById(R.id.btn_logout)
        imageViewProfile = view.findViewById(R.id.imageView)

        val btnChangeName: Button = view.findViewById(R.id.btn_ChangeName) // Новая кнопка
        btnChangeName.setOnClickListener { showChangeNameDialog() }

        val btnChangeProf: Button = view.findViewById(R.id.btn_ChangeProfile)

        // Обработчик нажатия на кнопку изменения профиля
        btnChangeProf.setOnClickListener {
            openGallery()
        }

        val user = auth.currentUser

        if (user != null) {
            if (user.isEmailVerified) { // Проверка, подтвержден ли email
                emailTextView.text = "${user.email}"
                getUserName(user.uid, loginTextView)
                loadImageFromFirebase(user.uid) // Загрузка изображения профиля
            } else {
                // Если email не подтвержден, перенаправляем на экран входа
                Log.d("SettingFragment", "Email не подтвержден")
                Toast.makeText(context, "Пожалуйста, подтвердите свой адрес электронной почты.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.navigation_login)
            }
        } else {
            // Если пользователь не зарегистрирован, перенаправляем на экран входа
            findNavController().navigate(R.id.navigation_login)
        }

        btnLogout.setOnClickListener {
            logOut()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == AppCompatActivity.RESULT_OK && data != null) {
            imageUri = data.data
            imageViewProfile.setImageURI(imageUri) // Устанавливаем изображение в ImageView

            // Сохраняем изображение в Firebase
            imageUri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                saveImageToFirebase(bitmap) // Сохранение изображения в Firebase
            }
        }
    }

    private fun getUserName(userId: String, loginTextView: TextView) {
        lifecycleScope.launch {
            try {
                val name = withContext(Dispatchers.IO) {
                    val dataSnapshot = myRef.child(userId).get().await()
                    if (dataSnapshot.exists()) {
                        dataSnapshot.child("username").getValue(String::class.java)
                    } else {
                        Log.d("SettingFragment", "Пользователь не найден в базе данных")
                        null
                    }
                }
                loginTextView.text = "${name ?: "Неизвестно"}"
            } catch (e: Exception) {
                Log.w("SettingFragment", "Ошибка при получении данных", e)
            }
        }
    }

    private fun saveImageToFirebase(bitmap: Bitmap) {
        val userId = auth.currentUser?.uid ?: return

        // Преобразуем Bitmap в строку Base64
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

        // Сохраняем строку в Firebase по пути profile/image
        myRef.child(userId).child("profile").child("image").setValue(encodedImage)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Изображение успешно сохранено", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w("SettingFragment", "Ошибка при сохранении изображения", e)
            }
    }

    private fun loadImageFromFirebase(userId: String) {
        // Загружаем изображение из profile/image
        myRef.child(userId).child("profile").child("image").get()
            .addOnSuccessListener { dataSnapshot ->
                val encodedImage = dataSnapshot.getValue(String::class.java)
                if (encodedImage != null) {
                    val decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    imageViewProfile.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener { e ->
                Log.w("SettingFragment", "Ошибка при загрузке изображения", e)
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
                if (newName.isNotEmpty()) {
                    updateUserName(newName)
                } else {
                    Toast.makeText(context, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    // ОБНОВЛЕНИЕ ИМЕНИ В FIREBASE
    private fun updateUserName(newName: String) {
        val userId = auth.currentUser?.uid ?: return

        myRef.child(userId).child("username").setValue(newName)
            .addOnSuccessListener {
                // Обновляем TextView с именем
                view?.findViewById<TextView>(R.id.tv_name)?.text = newName
                Toast.makeText(context, "Имя успешно изменено", Toast.LENGTH_SHORT).show()
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