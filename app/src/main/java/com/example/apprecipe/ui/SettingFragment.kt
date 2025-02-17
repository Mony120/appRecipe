package com.example.apprecipe.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.appcompat.app.AppCompatDelegate
import com.example.apprecipe.R

class SettingFragment : Fragment() {

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
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_YES -> switch.isChecked = true
            AppCompatDelegate.MODE_NIGHT_NO -> switch.isChecked = false
        }

        // Обработчик переключения Switch
        switch.setOnCheckedChangeListener { _, isChecked ->
            val newThemeMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(newThemeMode)
            sharedPreferences.edit().putInt("ThemeMode", newThemeMode).apply()
            activity?.recreate() // Перезапуск активности для применения темы
        }

        return view
    }
}