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
import com.google.firebase.database.*

data class Note(val id: String, val text: String)

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteInput: EditText
    private lateinit var addNoteButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private val notesList: MutableList<Note> = mutableListOf()
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users/${FirebaseAuth.getInstance().currentUser ?.uid}/notes")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        noteInput = binding.noteInput
        addNoteButton = binding.addNoteButton
        recyclerView = binding.recyclerView

        noteAdapter = NoteAdapter(notesList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = noteAdapter

        setupButtonListeners()

        loadNotes() // Загружаем заметки при создании фрагмента

        addNoteButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                saveNoteToFirebase(noteText)
                noteInput.text.clear()
            } else {
                Toast.makeText(requireContext(), "Введите заметку!", Toast.LENGTH_SHORT).show()
            }
        }

        val registrationPrompt: LinearLayout = binding.homeRegistrationPrompt
        checkCurrentUser (registrationPrompt)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Метод для сохранения заметки в Firebase
    private fun saveNoteToFirebase(noteText: String) {
        val noteId = database.push().key // Генерируем уникальный ключ для заметки
        noteId?.let {
            database.child(it).setValue(noteText).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    notesList.add(Note(it, noteText))
                    noteAdapter.notifyItemInserted(notesList.size - 1)
                    loadNotes()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении заметки", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Метод для загрузки заметок из Firebase
    private fun loadNotes() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                notesList.clear()
                for (snapshot in dataSnapshot.children) {
                    val noteText = snapshot.getValue(String::class.java)
                    val noteId = snapshot.key
                    if (noteText != null && noteId != null) {
                        notesList.add(Note(noteId, noteText))
                    }
                }
                noteAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(requireContext(), "Ошибка загрузки заметок", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private inner class NoteAdapter(private val notes: MutableList<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

        inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val noteText: TextView = itemView.findViewById(R.id.note_text)
            val btnEdit: AppCompatImageButton = itemView.findViewById(R.id.btnEdit)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
            return NoteViewHolder(view)
        }

        override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
            holder.noteText.text = notes[position].text
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

        // Метод для отображения диалогового окна редактирования заметки
        private fun showEditDialog(position: Int, currentNote: Note) {
            val editText = EditText(requireContext())
            editText.setText(currentNote.text)

            AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                .setTitle("Редактировать заметку")
                .setView(editText)
                .setPositiveButton("Сохранить") { _, _ ->
                    val updatedNote = editText.text.toString()
                    if (updatedNote.isNotEmpty()) {
                        updateNoteInFirebase(currentNote.id, updatedNote)
                    } else {
                        Toast.makeText(requireContext(), "Заметка не может быть пустой", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // Метод для обновления заметки в Firebase
        private fun updateNoteInFirebase(noteId: String, updatedNote: String) {
            database.child(noteId).setValue(updatedNote).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val position = notes.indexOfFirst { it.id == noteId }
                    if (position != -1) {
                        notes[position] = Note(noteId, updatedNote)
                        notifyItemChanged(position)
                    }
                } else {
                    Toast.makeText(requireContext(), "Ошибка при обновлении заметки", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Метод для отображения диалогового окна подтверждения удаления
        private fun showDeleteConfirmationDialog(position: Int) {
            AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
                .setTitle("Удалить заметку")
                .setMessage("Вы уверены, что хотите удалить эту заметку?")
                .setPositiveButton("Да") { _, _ -> removeNoteAt(position) }
                .setNegativeButton("Нет", null)
                .show()
        }

        // Метод для удаления заметки по позиции
        private fun removeNoteAt(position: Int) {
            val noteId = notes[position].id // Получаем ключ заметки
            database.child(noteId).removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    loadNotes() // Перезагружаем заметки после удаления
                } else {
                    Toast.makeText(requireContext(), "Ошибка при удалении заметки", Toast.LENGTH_SHORT).show()
                }
            }
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