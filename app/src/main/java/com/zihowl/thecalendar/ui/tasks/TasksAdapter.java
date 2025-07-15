package com.zihowl.thecalendar.ui.tasks;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Task;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TasksAdapter extends ListAdapter<Task, TasksAdapter.TaskViewHolder> {

    private final OnTaskStateChangedListener listener;

    public interface OnTaskStateChangedListener {
        void onTaskCheckedChanged(Task task);
    }

    public TasksAdapter(OnTaskStateChangedListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task currentTask = getItem(position);
        holder.bind(currentTask, listener);
    }

    private static final DiffUtil.ItemCallback<Task> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Task oldItem, @NonNull Task newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.isCompleted() == newItem.isCompleted();
        }
    };

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView title, description, dueDate, subjectName;
        public CheckBox completedCheckBox;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTaskTitle);
            description = itemView.findViewById(R.id.textViewTaskDescription);
            dueDate = itemView.findViewById(R.id.textViewTaskDueDate);
            subjectName = itemView.findViewById(R.id.textViewTaskSubject);
            completedCheckBox = itemView.findViewById(R.id.checkBoxTaskCompleted);
        }

        public void bind(final Task task, final OnTaskStateChangedListener listener) {
            title.setText(task.getTitle());
            description.setText(task.getDescription());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dueDate.setText(sdf.format(task.getDueDate()));
            subjectName.setText(task.getSubjectName());
            completedCheckBox.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                title.setPaintFlags(title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            }

            completedCheckBox.setOnClickListener(v -> listener.onTaskCheckedChanged(task));
        }
    }
}