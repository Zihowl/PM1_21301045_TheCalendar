package com.zihowl.thecalendar.ui.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.zihowl.thecalendar.ui.main.MainActivity;
import java.util.Locale;
import java.util.Set;

public class SubjectsFragment extends Fragment {

    private SubjectsViewModel viewModel;
    private SubjectsAdapter adapter;
    private OnBackPressedCallback backPressedCallback;
    private TextView emptyText;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setupMenu();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewSubjects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyText = view.findViewById(R.id.text_empty_subjects);
        setupAdapter();
        recyclerView.setAdapter(adapter);
        setupObservers();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.loadSubjects();
    }

    private void setupAdapter() {
        adapter = new SubjectsAdapter(
                (subject, position) -> {
                    if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                        viewModel.toggleSelection(subject);
                    } else {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).showSubjectDetail(subject.getName());
                        }
                    }
                },
                (subject, position) -> viewModel.toggleSelection(subject)
        );
    }

    private void setupObservers() {
        viewModel.subjects.observe(getViewLifecycleOwner(), subjects -> {
            adapter.submitList(subjects);
            boolean empty = subjects == null || subjects.isEmpty();
            if (emptyText != null) {
                emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });
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

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {}

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                if (isNotCurrentFragment()) return;
                boolean isSelection = Boolean.TRUE.equals(viewModel.isSelectionMode.getValue());
                int selectedCount = viewModel.selectedSubjects.getValue() != null ? viewModel.selectedSubjects.getValue().size() : 0;
                menu.findItem(R.id.action_add).setVisible(!isSelection);
                menu.findItem(R.id.action_delete).setVisible(isSelection);
                menu.findItem(R.id.action_edit).setVisible(isSelection && selectedCount == 1);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (isNotCurrentFragment()) return false;
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
        Set<Subject> selected = viewModel.selectedSubjects.getValue();
        if (selected != null && !selected.isEmpty()) {
            Subject subjectToEdit = selected.iterator().next();
            AddSubjectDialogFragment dialog = AddSubjectDialogFragment.newInstance(subjectToEdit);
            dialog.show(getParentFragmentManager(), "EditSubjectDialog");
        }
    }

    private void showDeleteConfirmationDialog() {
        Set<Subject> selectedSubjects = viewModel.selectedSubjects.getValue();
        if (selectedSubjects == null || selectedSubjects.isEmpty()) return;

        // Caso 1: Se selecciona una sola materia
        if (selectedSubjects.size() == 1) {
            Subject subject = selectedSubjects.iterator().next();
            // El ViewModel ahora determina si hay contenido
            if (viewModel.subjectHasContent(subject)) {
                int[] counts = viewModel.getSubjectContentCount(subject);
                String message = String.format(
                        Locale.getDefault(),
                        "Esta materia tiene %d tareas y %d notas asociadas.\n\n¿Qué deseas hacer?",
                        counts[0], counts[1]
                );
                new AlertDialog.Builder(requireContext())
                        .setTitle("Materia con Contenido")
                        .setMessage(message)
                        .setNeutralButton("Cancelar", null)
                        .setNegativeButton("Desvincular y Eliminar", (dialog, which) -> viewModel.disassociateAndDelete(subject, requireContext()))
                        .setPositiveButton("Eliminar Todo", (dialog, which) -> {
                            viewModel.deleteSelectedSubjects(requireContext());
                        })
                        .show();
                return;
            }
        }

        // Caso 2: Se seleccionan múltiples materias o una sin contenido.
        String message = "¿Estás seguro de que quieres eliminar las " + selectedSubjects.size() + " materias seleccionadas y todo su contenido asociado?";
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage(message)
                .setPositiveButton("Eliminar", (dialog, which) -> viewModel.deleteSelectedSubjects(requireContext()))
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
            return !((MainActivity) getActivity()).isCurrentTab(0);
        }
        return true;
    }
}