package com.zihowl.thecalendar.ui.notes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.TextView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.ui.main.MainActivity;

import java.util.Set;

public class NotesFragment extends Fragment {

    private NotesViewModel viewModel;
    private NotesAdapter adapter;
    private OnBackPressedCallback backPressedCallback;
    private TextView emptyText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);

        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.finishSelectionMode();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recarga las notas para reflejar borrados en cascada desde Materias.
        viewModel.loadNotes();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMenu();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewNotes);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyText = view.findViewById(R.id.text_empty_notes);

        setupAdapter();
        recyclerView.setAdapter(adapter);

        setupObservers();
    }

    private void setupAdapter() {
        adapter = new NotesAdapter(
                (note, position) -> { // onItemClick
                    if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                        viewModel.toggleSelection(note);
                    }
                },
                (note, position) -> { // onItemLongClick
                    viewModel.toggleSelection(note);
                }
        );
    }

    private void setupObservers() {
        viewModel.notes.observe(getViewLifecycleOwner(), notes -> {
            adapter.submitList(notes);
            boolean empty = notes == null || notes.isEmpty();
            if (emptyText != null) {
                emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.isSelectionMode.observe(getViewLifecycleOwner(), isSelection -> {
            backPressedCallback.setEnabled(isSelection);
            if (!isSelection) {
                updateActionBarTitle("Notas");
            }
            requireActivity().invalidateOptionsMenu();
        });

        viewModel.selectedNotes.observe(getViewLifecycleOwner(), selectedNotes -> {
            if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                int count = selectedNotes.size();
                if (count == 0) {
                    viewModel.finishSelectionMode();
                } else {
                    updateActionBarTitle(count + " seleccionadas");
                }
            }
            adapter.setSelectedItems(selectedNotes);
            requireActivity().invalidateOptionsMenu();
        });
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {}

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                if (!isResumed() || isNotCurrentFragment()) return;

                boolean isSelection = Boolean.TRUE.equals(viewModel.isSelectionMode.getValue());
                int selectedCount = viewModel.selectedNotes.getValue() != null ? viewModel.selectedNotes.getValue().size() : 0;

                MenuItem addItem = menu.findItem(R.id.action_add);
                if (addItem != null) addItem.setVisible(!isSelection);

                MenuItem deleteItem = menu.findItem(R.id.action_delete);
                if (deleteItem != null) deleteItem.setVisible(isSelection);

                MenuItem editItem = menu.findItem(R.id.action_edit);
                if (editItem != null) editItem.setVisible(isSelection && selectedCount == 1);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (!isResumed() || isNotCurrentFragment()) return false;

                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_delete) {
                    showDeleteConfirmationDialog();
                    return true;
                } else if (itemId == R.id.action_edit) {
                    handleEditAction();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void handleEditAction() {
        Set<Note> selected = viewModel.selectedNotes.getValue();
        if (selected != null && !selected.isEmpty()) {
            Note noteToEdit = selected.iterator().next();
            AddNoteDialogFragment dialog = AddNoteDialogFragment.newInstance(noteToEdit);
            dialog.show(getParentFragmentManager(), "EditNoteDialog");
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar las notas seleccionadas?")
                .setPositiveButton("Eliminar", (dialog, which) -> viewModel.deleteSelectedNotes(requireContext()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateActionBarTitle(String title) {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }
    }

    private boolean isNotCurrentFragment() {
        if (getActivity() instanceof MainActivity) {
            return !((MainActivity) getActivity()).isCurrentTab(2); // Índice 2 para Notas
        }
        return true;
    }
}