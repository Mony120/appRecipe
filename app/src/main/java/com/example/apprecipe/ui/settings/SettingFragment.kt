package com.example.apprecipe.ui.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
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

class SettingFragment : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var switch: Switch

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

        val user = auth.currentUser

        if (user != null) {
            if (user.isEmailVerified) { // Проверка, подтвержден ли email
                emailTextView.text = "Email: ${user.email}"
                getUserName(user.uid, loginTextView)
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
                loginTextView.text = "Имя: ${name ?: "Неизвестно"}"
            } catch (e: Exception) {
                Log.w("SettingFragment", "Ошибка при получении данных", e)
            }
        }
    }

    private fun logOut() {
        auth.signOut()
        findNavController().navigate(R.id.navigation_login)
    }
}