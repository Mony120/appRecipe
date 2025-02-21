package com.example.apprecipe.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apprecipe.R
import com.example.apprecipe.databinding.FragmentRegBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegFragment : Fragment() {

    private var _binding: FragmentRegBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var searchProgressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegBinding.inflate(inflater, container, false) // Инициализация привязки фрагмента
        return binding.root // Возврат корневого представления
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance() // Инициализация Firebase Authentication
        database = FirebaseDatabase.getInstance() // Инициализация Firebase Database
        searchProgressBar = binding.progressBar // Получение ссылки на прогресс-бар

        binding.btnReg.setOnClickListener {
            signUpUser () // Вызов метода регистрации пользователя
            binding.btnReg.visibility = View.GONE // Скрытие кнопки регистрации
            searchProgressBar.visibility = View.VISIBLE // Отображение прогресс-бара
        }

        binding.tvRedirectLogin.setOnClickListener {
            findNavController().navigate(R.id.navigation_login) // Переход к экрану входа
        }
    }

    private fun signUpUser () {
        val email = binding.emailReg.text.toString().trim() // Получение email из поля ввода
        val pass = binding.passwordReg.text.toString().trim() // Получение пароля из поля ввода
        val login = binding.loginReg.text.toString().trim() // Получение логина из поля ввода

        if (email.isBlank() || pass.isBlank() || login.isBlank()) {
            binding.btnReg.visibility = View.VISIBLE // Отображение кнопки регистрации
            searchProgressBar.visibility = View.GONE // Скрытие прогресс-бара
            return // Завершение метода, если поля пустые
        }

        auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser  // Получение текущего пользователя
                val userId = user?.uid // Получение уникального идентификатора пользователя

                if (userId != null) {
                    val usersRef = database.getReference("users").child(userId) // Ссылка на узел "users" с уникальным идентификатором пользователя
                    val userData = HashMap<String, String>() // Создание хэш-карты для хранения данных пользователя
                    userData["username"] = login // Сохранение имени пользователя в хэш-карте
                    usersRef.setValue(userData) // Сохранение данных в Realtime Database
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                user.sendEmailVerification().addOnCompleteListener { verificationTask -> // Отправка письма для подтверждения email
                                    if (verificationTask.isSuccessful) {
                                        findNavController().navigate(R.id.navigation_login) // Переход на экран входа
                                    } else {
                                        binding.btnReg.visibility = View.VISIBLE // Отображение кнопки регистрации
                                    }
                                }
                            } else {
                                binding.btnReg.visibility = View.VISIBLE // Отображение кнопки регистрации
                            }
                        }
                }
            } else {
                binding.btnReg.visibility = View.VISIBLE // Отображение кнопки регистрации
            }
            searchProgressBar.visibility = View.GONE // Скрытие прогресс-бара
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Освобождение привязки при уничтожении представления
    }
}