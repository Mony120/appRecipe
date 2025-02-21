package com.example.apprecipe.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apprecipe.R
import com.example.apprecipe.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            loginUser ()
        }

        binding.tvRedirectLogin.setOnClickListener {
            findNavController().navigate(R.id.navigation_reg)
        }
    }

    private fun loginUser () {
        val email = binding.emailLogin.text.toString()
        val password = binding.passwordLogin.text.toString()

        if (email.isBlank() || password.isBlank()) {
            showLoginButton() // Показать кнопку входа
            return
        }

        binding.btnLogin.visibility = View.GONE // Скрыть кнопку входа
        binding.progressBar.visibility = View.VISIBLE // Показать ProgressBar

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    if (user.isEmailVerified) {
                        findNavController().navigate(R.id.navigation_notifications) // Переход к NotificationsFragment
                    } else {
                        showLoginButton() // Показать кнопку входа, если email не подтвержден
                        Toast.makeText(context, "Пожалуйста, подтвердите свой адрес электронной почты.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                showLoginButton() // Показать кнопку входа, если вход не удался
                Toast.makeText(context, "Ошибка входа: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoginButton() {
        // Проверка на null перед доступом к binding
        binding?.let {
            it.btnLogin.visibility = View.VISIBLE
            it.progressBar.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}