package com.example.apprecipe.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apprecipe.R
import com.example.apprecipe.databinding.FragmentDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class Note(
    var id: String = "",
    var text: String = "",
    var timestamp: Long = System.currentTimeMillis()
) {
    // Конструктор без аргументов для Firebase
    constructor() : this("", "", System.currentTimeMillis())
}

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var noteInput: EditText
    private lateinit var addNoteButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var sortSpinner: Spinner
    private lateinit var noteAdapter: NoteAdapter
    private val notesList = mutableListOf<Note>()

    private enum class SortType { ALPHABET_ASC, ALPHABET_DESC, DATE_ASC, DATE_DESC }
    private var currentSortType = SortType.ALPHABET_ASC

    private val database: DatabaseReference? by lazy {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseDatabase.getInstance().getReference("users/$uid/notes")
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        checkCurrentUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        setupViews()
        setupSpinner()
        setupRecyclerView()
        checkCurrentUser()

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    private fun setupViews() {
        noteInput = binding.noteInput
        addNoteButton = binding.addNoteButton
        recyclerView = binding.recyclerView
        sortSpinner = binding.sortSpinner

        addNoteButton.setOnClickListener {
            val text = noteInput.text.toString()
            if (text.isNotEmpty()) {
                saveNote(Note(id = database?.push()?.key ?: "", text = text))
                noteInput.text.clear()
            } else {
                Toast.makeText(context, "Введите текст заметки", Toast.LENGTH_SHORT).show()
            }
        }

        binding.homeRegistrationBtn.setOnClickListener {
            findNavController().navigate(R.id.navigation_login)
        }
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sortSpinner.adapter = adapter
        }

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                currentSortType = when (pos) {
                    0 -> SortType.ALPHABET_ASC
                    1 -> SortType.ALPHABET_DESC
                    2 -> SortType.DATE_DESC
                    3 -> SortType.DATE_ASC
                    else -> SortType.ALPHABET_ASC
                }
                sortNotes()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        noteAdapter = NoteAdapter(notesList,
            onEditClick = { position -> showEditDialog(position) },
            onDeleteClick = { position -> showDeleteDialog(position) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = noteAdapter
        }
    }

    private fun sortNotes() {
        notesList.sortWith(Comparator { n1, n2 ->
            when (currentSortType) {
                SortType.ALPHABET_ASC -> n1.text.compareTo(n2.text, true)
                SortType.ALPHABET_DESC -> n2.text.compareTo(n1.text, true)
                SortType.DATE_ASC -> n1.timestamp.compareTo(n2.timestamp)
                SortType.DATE_DESC -> n2.timestamp.compareTo(n1.timestamp)
            }
        })
        noteAdapter.notifyDataSetChanged()
    }

    private fun saveNote(note: Note) {
        database?.child(note.id)?.setValue(note)
            ?.addOnSuccessListener { loadNotes() }
            ?.addOnFailureListener {
                Toast.makeText(context, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
            } ?: showUnauthorizedState()
    }

    private fun loadNotes() {
        database?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notesList.clear()
                snapshot.children.mapNotNull { it.getValue(Note::class.java) }
                    .forEach { notesList.add(it) }
                sortNotes()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }) ?: showUnauthorizedState()
    }

    private fun showEditDialog(position: Int) {
        val note = notesList[position]
        val editText = EditText(requireContext()).apply {
            setText(note.text)
        }

        AlertDialog.Builder(requireContext(), R.style.MyDialogTheme)
            .setTitle("Редактировать заметку")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedText = editText.text.toString()
                if (updatedText.isNotEmpty()) {
                    updateNote(note.id, updatedText)
                } else {
                    Toast.makeText(context, "Заметка не может быть пустой", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateNote(noteId: String, newText: String) {
        database?.child(noteId)?.setValue(Note(noteId, newText))
            ?.addOnSuccessListener { loadNotes() }
            ?.addOnFailureListener {
                Toast.makeText(context, "Ошибка обновления", Toast.LENGTH_SHORT).show()
            } ?: showUnauthorizedState()
    }

    private fun showDeleteDialog(position: Int) {
        val note = notesList[position]
        AlertDialog.Builder(requireContext(),R.style.MyDialogTheme)
            .setTitle("Удалить заметку")
            .setMessage("Вы уверены, что хотите удалить эту заметку?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteNote(note.id)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteNote(noteId: String) {
        database?.child(noteId)?.removeValue()
            ?.addOnSuccessListener { loadNotes() }
            ?.addOnFailureListener {
                Toast.makeText(context, "Ошибка удаления", Toast.LENGTH_SHORT).show()
            } ?: showUnauthorizedState()
    }

    private fun checkCurrentUser() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            showUnauthorizedState()
        } else {
            loadNotes()
            binding.homeRegistrationPrompt.visibility = View.GONE
            binding.sortSpinner.visibility = View.VISIBLE
            binding.scroll.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showUnauthorizedState() {
        binding.homeRegistrationPrompt.visibility = View.VISIBLE
        binding.sortSpinner.visibility = View.GONE
        binding.scroll.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}