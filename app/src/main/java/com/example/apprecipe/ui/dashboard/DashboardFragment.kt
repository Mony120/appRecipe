package com.example.apprecipe.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteInput: EditText
    private lateinit var addNoteButton: Button // Изменено на AppCompatImageButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private val notesList: MutableList<String> = mutableListOf()
    private val sharedPreferences by lazy {
        requireActivity().getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация binding
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        // Инициализация элементов интерфейса
        noteInput = binding.noteInput
        addNoteButton = binding.addNoteButton // Убедитесь, что это AppCompatImageButton
        recyclerView = binding.recyclerView

        // Настройка RecyclerView
        noteAdapter = NoteAdapter(notesList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = noteAdapter

        // Настройка слушателей кнопок
        setupButtonListeners()

        loadNotes() // Загружаем заметки при создании фрагмента

        addNoteButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                notesList.add(noteText)
                noteAdapter.notifyItemInserted(notesList.size - 1)
                noteInput.text.clear()
                saveNotes() // Сохраняем заметки после добавления
            } else {
                Toast.makeText(requireContext(), "Введите заметку!", Toast.LENGTH_SHORT).show()
            }
        }

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser (registrationPrompt)

        return binding.root // Возвращаем корневой элемент
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Освобождаем ссылку на binding, чтобы избежать утечек памяти
        _binding = null
    }

    // Метод для сохранения заметок в SharedPreferences
    private fun saveNotes() {
        val editor = sharedPreferences.edit()
        editor.putStringSet("notes", notesList.toSet())
        editor.apply()
    }

    // Метод для загрузки заметок из SharedPreferences
    private fun loadNotes() {
        val savedNotes = sharedPreferences.getStringSet("notes", emptySet())
        savedNotes?.let {
            notesList.clear()
            notesList.addAll(it)
            noteAdapter.notifyDataSetChanged()
        }
    }

    // Вложенный адаптер для заметок
    private inner class NoteAdapter(private val notes: MutableList<String>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

        inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val noteText: TextView = itemView.findViewById(R.id.note_text)
            val btnEdit: AppCompatImageButton = itemView.findViewById(R.id.btnEdit) // Изменено на AppCompatImageButton
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
            return NoteViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            holder.noteText.text = notes[position]

            // Обработка нажатия на кнопку редактирования
            holder.btnEdit.setOnClickListener {
                showEditDialog(position, notes[position])
            }

            // Обработка долгого нажатия на элемент (для удаления)
            holder.itemView.setOnLongClickListener {
                showDeleteConfirmationDialog(position)
                true
            }
        }

        override fun getItemCount(): Int {
            return notes.size
        }

        // Метод для удаления заметки по позиции
        private fun removeNoteAt(position: Int) {
            notes.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notes.size) // Обновляем позиции оставшихся элементов
            saveNotes() // Сохраняем заметки после удаления
        }

        // Метод для отображения диалогового окна подтверждения удаления
        private fun showDeleteConfirmationDialog(position: Int) {
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Удалить заметку?")
            builder.setMessage("Вы уверены, что хотите удалить эту заметку?")
            builder.setPositiveButton("Да") { dialog, _ ->
                removeNoteAt(position) // Удаляем заметку
                dialog.dismiss()
            }
            builder.setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }

        // Метод для отображения диалогового окна редактирования заметки
        private fun showEditDialog(position: Int, currentNote: String) {
            val editText = EditText(requireContext())
            editText.setText(currentNote)

            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Редактировать заметку")
            builder.setView(editText)
            builder.setPositiveButton("Сохранить") { dialog, _ ->
                val updatedNote = editText.text.toString()
                if (updatedNote.isNotEmpty()) {
                    notes[position] = updatedNote
                    notifyItemChanged(position)
                    saveNotes() // Сохраняем заметки после редактирования
                }
                dialog.dismiss()
            }
            builder.setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()

        }
    }
    private fun checkCurrentUser (registrationPrompt: LinearLayout) {
        val currentUser  = FirebaseAuth.getInstance().currentUser  // Получение текущего пользователя

        if (currentUser  == null) {
            showRegistrationPrompt(registrationPrompt) // Отображение подсказки регистрации, если пользователь не найден
            hideOtherElements() // Скрытие остальных элементов
        } else {
            registrationPrompt.visibility = View.GONE // Скрытие подсказки, если пользователь найден
            showOtherElements() // Показ остальных элементов
        }
    }

    private fun hideOtherElements() {
        binding.scroll.visibility = View.GONE // Скрыть ScrollView
        binding.recyclerView.visibility = View.GONE // Скрыть другие элементы, если необходимо
    }

    private fun showOtherElements() {
        binding.scroll.visibility = View.VISIBLE // Показать ScrollView
        binding.recyclerView.visibility = View.VISIBLE // Показать другие элементы, если они скрыты
    }

    private fun showRegistrationPrompt(registrationPrompt: LinearLayout) {
        registrationPrompt.visibility = View.VISIBLE // Отображение подсказки регистрации
        val slideIn = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom) // Загрузка анимации
        registrationPrompt.startAnimation(slideIn) // Запуск анимации
    }

    private fun setupButtonListeners() {
        binding.homeRegistrationBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_setting) // Переход к фрагменту регистрации
        }
    }
}