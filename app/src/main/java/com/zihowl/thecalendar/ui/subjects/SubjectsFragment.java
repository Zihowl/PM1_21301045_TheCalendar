package com.zihowl.thecalendar.ui.subjects;

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

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;

import java.util.Objects;
import java.util.Set;

public class SubjectsFragment extends Fragment {

    private SubjectsViewModel viewModel;
    private SubjectsAdapter adapter;
    private OnBackPressedCallback backPressedCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // CAMBIO: Ya no se llama a setHasOptionsMenu(true).
        viewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);

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
        return inflater.inflate(R.layout.fragment_subjects, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMenu(); // NUEVO: Llamamos al método que configura el MenuProvider.

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupAdapter();
        recyclerView.setAdapter(adapter);

        setupObservers();

        if (viewModel.subjects.getValue() == null) {
            viewModel.loadSubjects();
        }
    }

    private void setupAdapter() {
        adapter = new SubjectsAdapter(
                (subject, position) -> {
                    if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                        viewModel.toggleSelection(subject);
                    }
                },
                (subject, position) -> viewModel.toggleSelection(subject) // Lambda de expresión
        );
    }

    private void setupObservers() {
        viewModel.subjects.observe(getViewLifecycleOwner(), subjects -> adapter.submitList(subjects));

        viewModel.isSelectionMode.observe(getViewLifecycleOwner(), isSelection -> {
            backPressedCallback.setEnabled(isSelection);
            if (!isSelection) {
                updateActionBarTitle("Materias");
            }
            requireActivity().invalidateOptionsMenu();
        });

        viewModel.selectedSubjects.observe(getViewLifecycleOwner(), selectedSubjects -> {
            if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                int count = selectedSubjects.size();
                if (count == 0) {
                    viewModel.finishSelectionMode();
                } else {
                    updateActionBarTitle(count + " seleccionados");
                }
            }
            adapter.setSelectedItems(selectedSubjects);
            requireActivity().invalidateOptionsMenu();
        });
    }

    // NUEVO: Método para configurar el menú de forma moderna y segura.
    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // Se infla el menú aquí.
                menuInflater.inflate(R.menu.main_menu, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                // La lógica de visibilidad de los botones va aquí.
                boolean isSelection = Boolean.TRUE.equals(viewModel.isSelectionMode.getValue());
                int selectedCount = viewModel.selectedSubjects.getValue() != null ? viewModel.selectedSubjects.getValue().size() : 0;

                menu.findItem(R.id.action_add).setVisible(!isSelection);
                menu.findItem(R.id.action_delete).setVisible(isSelection);
                menu.findItem(R.id.action_edit).setVisible(isSelection && selectedCount == 1);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // La lógica para manejar los clics en los botones va aquí.
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_delete) {
                    showDeleteConfirmationDialog();
                    return true;
                } else if (itemId == R.id.action_edit) {
                    handleEditAction();
                    return true;
                }
                // Si este provider no maneja el evento, se devuelve false.
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    // NUEVO: Método para manejar la acción de editar.
    private void handleEditAction() {
        Set<Subject> selected = viewModel.selectedSubjects.getValue();
        if (selected != null && !selected.isEmpty()) {
            Subject subjectToEdit = selected.iterator().next();
            int position = Objects.requireNonNull(viewModel.subjects.getValue()).indexOf(subjectToEdit);
            AddSubjectDialogFragment dialog = AddSubjectDialogFragment.newInstance(position, subjectToEdit);
            dialog.show(getParentFragmentManager(), "EditSubjectDialog");
        }
    }

    // NUEVO: Método centralizado y seguro para cambiar el título.
    private void updateActionBarTitle(String title) {
        if (getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar las materias seleccionadas?")
                .setPositiveButton("Eliminar", (dialog, which) -> viewModel.deleteSelectedSubjects())
                .setNegativeButton("Cancelar", null)
                .show();
    }
}