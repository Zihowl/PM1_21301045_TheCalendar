package com.zihowl.thecalendar.ui.subjects;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Subject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class SubjectsAdapter extends RecyclerView.Adapter<SubjectsAdapter.SubjectViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Subject subject, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Subject subject, int position);
    }

    // AHORA: El adapter no maneja listas, solo recibe datos para mostrar.
    private List<Subject> subjectList = new ArrayList<>();
    private Set<Subject> selectedItems = Collections.emptySet();
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public SubjectsAdapter(OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    // NUEVO: Método para actualizar la lista y los seleccionados desde el Fragment.
    public void submitList(List<Subject> subjects, Set<Subject> selected) {
        this.subjectList = subjects != null ? new ArrayList<>(subjects) : new ArrayList<>();
        this.selectedItems = selected != null ? selected : Collections.emptySet();
        notifyDataSetChanged(); // Simplificado por claridad, idealmente usar DiffUtil
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subject, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        Subject currentSubject = subjectList.get(position);
        holder.bind(currentSubject, clickListener, longClickListener);

        // AHORA: La lógica de selección depende del Set que pasamos
        if (selectedItems.contains(currentSubject)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return subjectList.size();
    }

    // Clase ViewHolder interna
    public static class SubjectViewHolder extends RecyclerView.ViewHolder {
        // ... tu código de ViewHolder no necesita cambios
        public TextView name, schedule, tasksPending, notesCount;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.textViewSubjectName);
            schedule = itemView.findViewById(R.id.textViewSubjectSchedule);
            tasksPending = itemView.findViewById(R.id.textViewTasksPending);
            notesCount = itemView.findViewById(R.id.textViewNotesCount);
        }

        public void bind(final Subject subject, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
            name.setText(subject.getName());
            schedule.setText(formatSchedule(subject.getSchedule()));
            tasksPending.setText("Tareas pendientes: " + subject.getTasksPending());
            notesCount.setText("Notas: " + subject.getNotesCount());

            itemView.setOnClickListener(v -> clickListener.onItemClick(subject, getAdapterPosition()));
            itemView.setOnLongClickListener(v -> {
                longClickListener.onItemLongClick(subject, getAdapterPosition());
                return true;
            });
        }

        private String formatTime(String time24h) {
            if (time24h == null || time24h.isEmpty()) return "";
            try {
                SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return sdf12.format(sdf24.parse(time24h));
            } catch (ParseException e) {
                return time24h; // Devuelve el original si falla el parseo
            }
        }

        private String formatSchedule(String schedule24h) {
            if (schedule24h == null || schedule24h.isEmpty()) return "Sin horario";

            String[] lines = schedule24h.split("\n");
            StringBuilder formattedSchedule = new StringBuilder();

            for (String line : lines) {
                try {
                    // CORRECCIÓN CLAVE: La misma lógica que en el diálogo
                    String[] parts = line.split(" ");
                    String day = parts[0];
                    String[] times = line.substring(day.length()).trim().split(" - ");

                    // Aplicar formato de 12 horas a cada parte
                    formattedSchedule.append(String.format("%s %s - %s\n", day, formatTo12Hour(times[0]), formatTo12Hour(times[1])));
                } catch (Exception e) {
                    // Si el formato es inesperado, simplemente añade la línea original.
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