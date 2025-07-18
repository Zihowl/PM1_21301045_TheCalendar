package com.zihowl.thecalendar.ui.subjects;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class SubjectsAdapter extends ListAdapter<Subject, SubjectsAdapter.SubjectViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Subject subject, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Subject subject, int position);
    }

    private Set<Subject> selectedItems = Collections.emptySet();
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public SubjectsAdapter(OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    public void setSelectedItems(Set<Subject> selected) {
        this.selectedItems = selected != null ? selected : Collections.emptySet();
        // Aclaración sobre la advertencia:
        // Usamos notifyDataSetChanged() aquí a propósito porque necesitamos forzar el redibujado
        // de todos los elementos visibles para actualizar su estado de selección (el color de fondo).
        // Dado que solo se llama cuando la selección cambia, su impacto en el rendimiento es mínimo
        // y es una solución aceptada para este caso de uso específico.
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    // Dentro de SubjectsAdapter.java

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        Subject currentSubject = getItem(position);
        holder.bind(currentSubject, clickListener, longClickListener);

        androidx.cardview.widget.CardView cardView = (androidx.cardview.widget.CardView) holder.itemView;

        if (selectedItems.contains(currentSubject)) {
            // Usamos un color gris claro para el elemento seleccionado
            cardView.setCardBackgroundColor(Color.LTGRAY);
        } else {
            // Para los no seleccionados, restauramos el color original guardado
            cardView.setCardBackgroundColor(holder.defaultCardBackgroundColor);
        }
    }

    private static final DiffUtil.ItemCallback<Subject> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Subject oldItem, @NonNull Subject newItem) {
            // Comparamos por ID.
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Subject oldItem, @NonNull Subject newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && Objects.equals(oldItem.getProfessorName(), newItem.getProfessorName())
                    && oldItem.getSchedule().equals(newItem.getSchedule())
                    && oldItem.getTasksPending() == newItem.getTasksPending()
                    && oldItem.getNotesCount() == newItem.getNotesCount();
        }
    };

    public static class SubjectViewHolder extends RecyclerView.ViewHolder {
        public TextView name, professorName, schedule, tasksPending, notesCount; // CAMBIADO
        public final ColorStateList defaultCardBackgroundColor;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewSubjectName);
            professorName = itemView.findViewById(R.id.textViewProfessorName); // AÑADIDO
            schedule = itemView.findViewById(R.id.textViewSubjectSchedule);
            tasksPending = itemView.findViewById(R.id.textViewTasksPending);
            notesCount = itemView.findViewById(R.id.textViewNotesCount);

            defaultCardBackgroundColor = ((androidx.cardview.widget.CardView) itemView).getCardBackgroundColor();
        }

        public void bind(final Subject subject, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
            name.setText(subject.getName());
            schedule.setText(formatSchedule(subject.getSchedule()));

            // AÑADIDO: Lógica para mostrar el nombre del profesor
            if (subject.getProfessorName() != null && !subject.getProfessorName().isEmpty()) {
                professorName.setText(subject.getProfessorName());
                professorName.setVisibility(View.VISIBLE);
            } else {
                professorName.setVisibility(View.GONE);
            }

            Resources res = itemView.getResources();
            tasksPending.setText(res.getString(R.string.subject_tasks_pending, subject.getTasksPending()));
            notesCount.setText(res.getString(R.string.subject_notes_count, subject.getNotesCount()));

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(subject, position);
                }
            });
            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(subject, position);
                }
                return true;
            });
        }

        private String formatSchedule(String schedule24h) {
            if (schedule24h == null || schedule24h.isEmpty()) return "Sin horario";
            String[] lines = schedule24h.split("\n");
            StringBuilder formattedSchedule = new StringBuilder();
            for (String line : lines) {
                try {
                    String[] parts = line.split(" ");
                    String day = parts[0];
                    String[] times = line.substring(day.length()).trim().split(" - ");
                    formattedSchedule.append(String.format("%s %s - %s\n", day, formatTo12Hour(times[0]), formatTo12Hour(times[1])));
                } catch (Exception e) {
                    formattedSchedule.append(line).append("\n");
                }
            }
            return formattedSchedule.toString().trim();
        }

        private String formatTo12Hour(String time24h) {
            if (time24h == null || time24h.isEmpty()) return "";
            try {
                SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return sdf12.format(Objects.requireNonNull(sdf24.parse(time24h)));
            } catch (Exception e) {
                return time24h;
            }
        }
    }
}