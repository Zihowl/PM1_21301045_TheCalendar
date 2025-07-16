package com.zihowl.thecalendar.ui.subjects;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.ui.main.MainActivity;
import com.zihowl.thecalendar.ui.notes.NotesAdapter;
import com.zihowl.thecalendar.ui.notes.NotesViewModel;
import com.zihowl.thecalendar.ui.tasks.TasksAdapter;
import com.zihowl.thecalendar.ui.tasks.TasksViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SubjectDetailFragment extends Fragment {

    private static final String ARG_SUBJECT_NAME = "subject_name";
    private static final String HEADER_PENDING = "Pendientes";
    private static final String HEADER_COMPLETED = "Completadas";

    private SubjectsViewModel subjectsViewModel;
    private TasksViewModel tasksViewModel;
    private NotesViewModel notesViewModel;

    private String subjectName;
    private TasksAdapter tasksAdapter;
    private NotesAdapter notesAdapter;

    private boolean isPendingTasksExpanded = true;
    private boolean isCompletedTasksExpanded = false;

    public static SubjectDetailFragment newInstance(String subjectName) {
        SubjectDetailFragment fragment = new SubjectDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUBJECT_NAME, subjectName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            subjectName = getArguments().getString(ARG_SUBJECT_NAME);
        }
        // --- SOLUCIÓN: INICIALIZAR VIEWMODELS ---
        subjectsViewModel = new ViewModelProvider(requireActivity()).get(SubjectsViewModel.class);
        tasksViewModel = new ViewModelProvider(requireActivity()).get(TasksViewModel.class);
        notesViewModel = new ViewModelProvider(requireActivity()).get(NotesViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // --- SOLUCIÓN: PRE-CARGAR DATOS ANTES DE CREAR LA VISTA ---
        if (subjectsViewModel.subjects.getValue() == null) subjectsViewModel.loadSubjects();
        if (tasksViewModel.pendingTasks.getValue() == null) tasksViewModel.loadTasks();
        if (notesViewModel.notes.getValue() == null) notesViewModel.loadNotes();

        return inflater.inflate(R.layout.fragment_subject_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setDrawerLocked(true);
        }

        setupAdapters();
        setupRecyclerViews(view);

        // Dibuja la UI inmediatamente con los datos ya disponibles.
        updateAllUI(view);

        // Añade observadores para reaccionar a cambios futuros.
        setupObservers();
    }

    private void setupAdapters() {
        tasksAdapter = new TasksAdapter(
                task -> tasksViewModel.toggleTaskCompletion(task),
                task -> {},
                task -> {},
                headerTitle -> {
                    if (HEADER_PENDING.equals(headerTitle)) isPendingTasksExpanded = !isPendingTasksExpanded;
                    else if (HEADER_COMPLETED.equals(headerTitle)) isCompletedTasksExpanded = !isCompletedTasksExpanded;
                    updateAllUI(getView());
                }
        );
        notesAdapter = new NotesAdapter((note, position) -> {}, (note, position) -> {});
    }

    private void setupRecyclerViews(View view) {
        RecyclerView tasksRv = view.findViewById(R.id.recycler_view_tasks);
        tasksRv.setLayoutManager(new LinearLayoutManager(getContext()));
        tasksRv.setAdapter(tasksAdapter);

        RecyclerView notesRv = view.findViewById(R.id.recycler_view_notes);
        notesRv.setLayoutManager(new LinearLayoutManager(getContext()));
        notesRv.setAdapter(notesAdapter);
    }

    private void setupObservers() {
        subjectsViewModel.subjects.observe(getViewLifecycleOwner(), s -> updateAllUI(getView()));
        tasksViewModel.pendingTasks.observe(getViewLifecycleOwner(), t -> updateAllUI(getView()));
        tasksViewModel.completedTasks.observe(getViewLifecycleOwner(), t -> updateAllUI(getView()));
        notesViewModel.notes.observe(getViewLifecycleOwner(), n -> updateAllUI(getView()));
    }

    private void updateAllUI(View view) {
        if (view == null) return;

        Subject currentSubject = findCurrentSubject();
        if (currentSubject == null) return;

        updateSubjectInfo(view, currentSubject);
        updateTasksList(currentSubject);
        updateNotesList(currentSubject);
    }

    private Subject findCurrentSubject() {
        List<Subject> subjects = subjectsViewModel.subjects.getValue();
        return (subjects == null) ? null : subjects.stream()
                .filter(s -> s.getName().equals(subjectName))
                .findFirst()
                .orElse(null);
    }

    private void updateSubjectInfo(@NonNull View view, @NonNull Subject subject) {
        TextView subjectNameTv = view.findViewById(R.id.detail_subject_name);
        TextView professorNameTv = view.findViewById(R.id.detail_professor_name);
        TextView scheduleTv = view.findViewById(R.id.detail_schedule);

        subjectNameTv.setText(subject.getName());
        scheduleTv.setText(subject.getSchedule());
        if (subject.getProfessorName() != null && !subject.getProfessorName().isEmpty()) {
            professorNameTv.setText(subject.getProfessorName());
            professorNameTv.setVisibility(View.VISIBLE);
        } else {
            professorNameTv.setVisibility(View.GONE);
        }
    }

    private void updateTasksList(@NonNull Subject subject) {
        List<Task> pending = filterTasks(tasksViewModel.pendingTasks.getValue(), subject.getName());
        List<Task> completed = filterTasks(tasksViewModel.completedTasks.getValue(), subject.getName());

        List<Object> displayList = new ArrayList<>();
        if (!pending.isEmpty()) {
            displayList.add(HEADER_PENDING);
            if (isPendingTasksExpanded) displayList.addAll(pending);
        }
        if (!completed.isEmpty()) {
            displayList.add(HEADER_COMPLETED);
            if (isCompletedTasksExpanded) displayList.addAll(completed);
        }
        tasksAdapter.submitList(displayList);
    }

    private void updateNotesList(@NonNull Subject subject) {
        List<Note> allNotes = notesViewModel.notes.getValue();
        if (allNotes != null) {
            notesAdapter.submitList(allNotes.stream()
                    .filter(n -> subject.getName().equals(n.getSubjectName()))
                    .collect(Collectors.toList()));
        }
    }

    private List<Task> filterTasks(List<Task> tasks, String subjectName) {
        if (tasks == null || subjectName == null) return new ArrayList<>();
        return tasks.stream()
                .filter(t -> subjectName.equals(t.getSubjectName()))
                .collect(Collectors.toList());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setDrawerLocked(false);
        }
    }
}