package com.zihowl.thecalendar.ui.tasks;

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
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.ui.main.MainActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TasksFragment extends Fragment {

    private TasksViewModel viewModel;
    private TasksAdapter adapter;
    private OnBackPressedCallback backPressedCallback;
    private static final String HEADER_PENDING = "Pendientes";
    private static final String HEADER_COMPLETED = "Completadas";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);

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
        return inflater.inflate(R.layout.fragment_tasks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMenu();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new TasksAdapter(
                task -> viewModel.toggleTaskCompletion(task),
                task -> {
                    if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                        viewModel.toggleSelection(task);
                    } else {
                        handleEditAction(task);
                    }
                },
                task -> viewModel.toggleSelection(task),
                headerTitle -> {
                    if (HEADER_PENDING.equals(headerTitle)) {
                        viewModel.togglePendingExpansion();
                    } else if (HEADER_COMPLETED.equals(headerTitle)) {
                        viewModel.toggleCompletedExpansion();
                    }
                }
        );
        recyclerView.setAdapter(adapter);
        setupObservers();

        if (Objects.requireNonNull(viewModel.pendingTasks.getValue()).isEmpty() && Objects.requireNonNull(viewModel.completedTasks.getValue()).isEmpty()) {
            viewModel.loadTasks();
        }
    }

    private void setupObservers() {
        // Observadores que reconstruirán la lista cuando cualquier dato cambie
        viewModel.pendingTasks.observe(getViewLifecycleOwner(), tasks -> buildDisplayList());
        viewModel.completedTasks.observe(getViewLifecycleOwner(), tasks -> buildDisplayList());
        viewModel.isPendingExpanded.observe(getViewLifecycleOwner(), isExpanded -> buildDisplayList());
        viewModel.isCompletedExpanded.observe(getViewLifecycleOwner(), isExpanded -> buildDisplayList());

        // Observadores para el modo de selección
        viewModel.isSelectionMode.observe(getViewLifecycleOwner(), isSelection -> {
            backPressedCallback.setEnabled(isSelection);
            if (!isSelection) updateActionBarTitle("Tareas");
            requireActivity().invalidateOptionsMenu();
        });

        viewModel.selectedTasks.observe(getViewLifecycleOwner(), selectedTasks -> {
            adapter.setSelectedItems(selectedTasks);
            if (Boolean.TRUE.equals(viewModel.isSelectionMode.getValue())) {
                int count = selectedTasks.size();
                if (count == 0) {
                    viewModel.finishSelectionMode();
                } else {
                    updateActionBarTitle(count + " seleccionadas");
                }
            }
            requireActivity().invalidateOptionsMenu();
        });
    }

    private void buildDisplayList() {
        List<Object> displayList = new ArrayList<>();
        List<Task> pending = viewModel.pendingTasks.getValue();
        List<Task> completed = viewModel.completedTasks.getValue();

        if (pending != null && !pending.isEmpty()) {
            displayList.add(HEADER_PENDING);
            if (Boolean.TRUE.equals(viewModel.isPendingExpanded.getValue())) {
                displayList.addAll(pending);
            }
        }

        if (completed != null && !completed.isEmpty()) {
            displayList.add(HEADER_COMPLETED);
            if (Boolean.TRUE.equals(viewModel.isCompletedExpanded.getValue())) {
                displayList.addAll(completed);
            }
        }
        adapter.submitList(displayList);
    }

    private void handleEditAction(Task taskToEdit) {
        if (taskToEdit == null) {
            Set<Task> selected = viewModel.selectedTasks.getValue();
            if (selected != null && !selected.isEmpty()) {
                taskToEdit = selected.iterator().next();
            }
        }

        if (taskToEdit != null) {
            AddTaskDialogFragment.newInstance(taskToEdit).show(getParentFragmentManager(), "EditTaskDialog");
        }
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {}

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                if (!isResumed() || !isCurrentFragment()) return;
                boolean isSelection = Boolean.TRUE.equals(viewModel.isSelectionMode.getValue());
                int selectedCount = viewModel.selectedTasks.getValue() != null ? viewModel.selectedTasks.getValue().size() : 0;
                menu.findItem(R.id.action_add).setVisible(!isSelection);
                menu.findItem(R.id.action_delete).setVisible(isSelection);
                menu.findItem(R.id.action_edit).setVisible(isSelection && selectedCount == 1);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (!isCurrentFragment()) return false;
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_delete) {
                    showDeleteConfirmationDialog();
                    return true;
                } else if (itemId == R.id.action_edit) {
                    handleEditAction(null);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirmar Eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar las tareas seleccionadas?")
                .setPositiveButton("Eliminar", (dialog, which) -> viewModel.deleteSelectedTasks())
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

    private boolean isCurrentFragment() {
        if (getActivity() instanceof MainActivity) {
            return ((MainActivity) getActivity()).isCurrentTab(1);
        }
        return false;
    }
}