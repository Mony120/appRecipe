package com.example.apprecipe.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R

class DashboardFragment : Fragment() {
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        noteInput = view.findViewById(R.id.note_input)
        addNoteButton = view.findViewById(R.id.add_note_button) // Убедитесь, что это AppCompatImageButton
        recyclerView = view.findViewById(R.id.recycler_view)

        noteAdapter = NoteAdapter(notesList)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = noteAdapter

        loadNotes() // Загружаем заметки при создании фрагмента

        addNoteButton.setOnClickListener {
            val noteText = noteInput.text.toString()
            if (noteText.isNotEmpty()) {
                notesList.add(noteText)
                noteAdapter.notifyItemInserted(notesList.size - 1)
                noteInput.text.clear()
                saveNotes() // Сохраняем заметки после добавления
            }
            else{
                Toast.makeText(requireContext(), "Введите заметку!", Toast.LENGTH_SHORT).show()
            }


        }


        return view
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
}