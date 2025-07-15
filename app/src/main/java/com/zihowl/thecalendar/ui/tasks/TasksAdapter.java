package com.zihowl.thecalendar.ui.tasks;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Task;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TasksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TASK = 1;

    private List<Object> displayList = new ArrayList<>();
    private Set<Task> selectedItems = Collections.emptySet();

    private final OnTaskStateChangedListener taskListener;
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;
    private final OnHeaderClickListener headerClickListener;

    public interface OnTaskStateChangedListener { void onTaskCheckedChanged(Task task); }
    public interface OnItemClickListener { void onItemClick(Task task); }
    public interface OnItemLongClickListener { void onItemLongClick(Task task); }
    public interface OnHeaderClickListener { void onHeaderClick(String headerTitle); }

    public TasksAdapter(OnTaskStateChangedListener taskListener, OnItemClickListener clickListener, OnItemLongClickListener longClickListener, OnHeaderClickListener headerClickListener) {
        this.taskListener = taskListener;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
        this.headerClickListener = headerClickListener;
    }

    public void submitList(List<Object> newList) {
        this.displayList = newList;
        // Forzar un redibujado completo de la lista. Esta es la solución clave.
        notifyDataSetChanged();
    }

    public void setSelectedItems(Set<Task> selected) {
        this.selectedItems = selected != null ? selected : Collections.emptySet();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayList.get(position) instanceof String ? VIEW_TYPE_HEADER : VIEW_TYPE_TASK;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_task_header, parent, false);
            return new HeaderViewHolder(view, headerClickListener);
        }
        View view = inflater.inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((String) displayList.get(position));
        } else {
            Task currentTask = (Task) displayList.get(position);
            TaskViewHolder taskHolder = (TaskViewHolder) holder;
            taskHolder.bind(currentTask, taskListener, clickListener, longClickListener);

            CardView cardView = (CardView) taskHolder.itemView;
            if (selectedItems.contains(currentTask)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.lightgray));
            } else {
                cardView.setCardBackgroundColor(taskHolder.defaultCardBackgroundColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // --- View Holders ---
    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        public TextView title, description, dueDate, subjectName;
        public CheckBox completedCheckBox;
        public final int defaultCardBackgroundColor;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTaskTitle);
            description = itemView.findViewById(R.id.textViewTaskDescription);
            dueDate = itemView.findViewById(R.id.textViewTaskDueDate);
            subjectName = itemView.findViewById(R.id.textViewTaskSubject);
            completedCheckBox = itemView.findViewById(R.id.checkBoxTaskCompleted);
            defaultCardBackgroundColor = ((CardView) itemView).getCardBackgroundColor().getDefaultColor();
        }

        public void bind(final Task task, final OnTaskStateChangedListener listener, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
            title.setText(task.getTitle());
            description.setText(task.getDescription());
            description.setVisibility(task.getDescription() != null && !task.getDescription().isEmpty() ? View.VISIBLE : View.GONE);

            if (task.getDueDate() != null) {
                dueDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(task.getDueDate()));
                dueDate.setVisibility(View.VISIBLE);
            } else {
                dueDate.setVisibility(View.GONE);
            }

            if (task.getSubjectName() != null && !task.getSubjectName().isEmpty()) {
                subjectName.setText(task.getSubjectName());
                subjectName.setVisibility(View.VISIBLE);
            } else {
                subjectName.setVisibility(View.GONE);
            }

            completedCheckBox.setChecked(task.isCompleted());
            title.setPaintFlags(task.isCompleted() ? (title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG) : (title.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)));
            itemView.setAlpha(task.isCompleted() ? 0.6f : 1.0f);

            itemView.setOnClickListener(v -> clickListener.onItemClick(task));
            itemView.setOnLongClickListener(v -> { longClickListener.onItemLongClick(task); return true; });
            completedCheckBox.setOnClickListener(v -> listener.onTaskCheckedChanged(task));
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerTitle;
        private final ImageView expandIcon;

        public HeaderViewHolder(@NonNull View itemView, OnHeaderClickListener listener) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.header_title);
            expandIcon = itemView.findViewById(R.id.expand_icon);
            itemView.setOnClickListener(v -> listener.onHeaderClick(headerTitle.getText().toString()));
        }

        public void bind(String title) {
            headerTitle.setText(title);
        }
    }
}