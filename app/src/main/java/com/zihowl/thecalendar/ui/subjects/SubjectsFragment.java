package com.zihowl.thecalendar.ui.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class SubjectsFragment extends Fragment {

    // NUEVO: Instancia del ViewModel
    private SubjectsViewModel viewModel;
    private SubjectsAdapter adapter;
    private OnBackPressedCallback backPressedCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Permite al fragmento manejar el menú

        // NUEVO: Obtenemos la instancia del ViewModel.
        // El 'this' asegura que el ViewModel sobrevive mientras el Fragment exista.
        viewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);

        // Callback para manejar el botón "Atrás"
        backPressedCallback = new OnBackPressedCallback(false) { // Inicialmente deshabilitado
            @Override
            public void handleOnBackPressed() {
                viewModel.finishSelectionMode(); // Delega la acción al ViewModel
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

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        setupAdapter();
        recyclerView.setAdapter(adapter);

        // NUEVO: Observamos los cambios en el ViewModel
        setupObservers();

        // Carga los datos solo si no existen
        if (viewModel.subjects.getValue() == null) {
            viewModel.loadSubjects();
        }
    }

    private void setupAdapter() {
        adapter = new SubjectsAdapter(
                (subject, position) -> { // Click listener
                    // Si estamos en modo selección, delegamos el toggle al ViewModel
                    if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                        viewModel.toggleSelection(subject);
                    } else {
                        // Aquí puedes manejar clics normales, como ir a una pantalla de detalle
                    }
                },
                (subject, position) -> { // Long-click listener
                    viewModel.toggleSelection(subject); // Siempre inicia o modifica la selección
                }
        );
    }

    private void setupObservers() {
        // Observador para la lista de materias
        viewModel.subjects.observe(getViewLifecycleOwner(), subjects -> {
            // El adapter se actualiza cada vez que la lista cambia en el ViewModel
            adapter.submitList(subjects, viewModel.selectedSubjects.getValue());
        });

        // Observador para el modo de selección
        viewModel.isSelectionMode.observe(getViewLifecycleOwner(), isSelection -> {
            backPressedCallback.setEnabled(isSelection);
            if (!isSelection && getActivity() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Materias");
            }
            requireActivity().invalidateOptionsMenu(); // Actualiza los botones del menú
        });

        // Observador para los ítems seleccionados (para actualizar el título)
        viewModel.selectedSubjects.observe(getViewLifecycleOwner(), selectedSubjects -> {
            if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                int count = selectedSubjects.size();
                if (count == 0) {
                    viewModel.finishSelectionMode();
                } else {
                    ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(count + " seleccionados");
                }
            }
            // Actualiza la lista para reflejar los cambios de selección
            adapter.submitList(viewModel.subjects.getValue(), selectedSubjects);
            requireActivity().invalidateOptionsMenu();
        });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        // La visibilidad del menú ahora depende del estado en el ViewModel
        boolean isSelection = Boolean.TRUE.equals(viewModel.isSelectionMode.getValue());
        int selectedCount = viewModel.selectedSubjects.getValue() != null ? viewModel.selectedSubjects.getValue().size() : 0;

        menu.findItem(R.id.action_add).setVisible(!isSelection);
        menu.findItem(R.id.action_delete).setVisible(isSelection);
        menu.findItem(R.id.action_edit).setVisible(isSelection && selectedCount == 1);

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_delete) {
            showDeleteConfirmationDialog();
            return true;
        } else if (itemId == R.id.action_edit) {
            // Obtenemos la materia seleccionada desde el ViewModel
            Set<Subject> selected = viewModel.selectedSubjects.getValue();
            if (selected != null && !selected.isEmpty()) {
                Subject subjectToEdit = selected.iterator().next();
                int position = Objects.requireNonNull(viewModel.subjects.getValue()).indexOf(subjectToEdit);

                AddSubjectDialogFragment dialog = AddSubjectDialogFragment.newInstance(position, subjectToEdit);
                dialog.show(getParentFragmentManager(), "EditSubjectDialog");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar las materias seleccionadas?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteSelectedSubjects(); // Delega la eliminación al ViewModel
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}