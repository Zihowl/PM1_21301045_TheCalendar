package com.zihowl.thecalendar.ui.notes;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.zihowl.thecalendar.R;
import com.zihowl.thecalendar.data.model.Note;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class NotesAdapter extends ListAdapter<Note, NotesAdapter.NoteViewHolder> {

    // --- INTERFACES QUE FALTABAN ---
    public interface OnItemClickListener {
        void onItemClick(Note note, int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Note note, int position);
    }
    // ------------------------------------

    private Set<Note> selectedItems = Collections.emptySet();
    private final OnItemClickListener clickListener;
    private final OnItemLongClickListener longClickListener;

    public NotesAdapter(OnItemClickListener clickListener, OnItemLongClickListener longClickListener) {
        super(DIFF_CALLBACK); // Ahora DIFF_CALLBACK será encontrado
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @SuppressLint("NotifyDataSetChanged") // Suprime el warning de eficiencia
    public void setSelectedItems(Set<Note> selected) {
        this.selectedItems = selected != null ? selected : Collections.emptySet();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note currentNote = getItem(position);
        holder.bind(currentNote, clickListener, longClickListener);

        CardView cardView = (CardView) holder.itemView;
        if (selectedItems.contains(currentNote)) {
            cardView.setCardBackgroundColor(Color.LTGRAY);
        } else {
            cardView.setCardBackgroundColor(holder.defaultCardBackgroundColor);
        }
    }

    // --- DIFF_CALLBACK QUE FALTABA ---
    private static final DiffUtil.ItemCallback<Note> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            // Un identificador único sería ideal aquí, pero el título puede funcionar si es único
            return oldItem.getTitle().equals(newItem.getTitle()) && Objects.equals(oldItem.getContent(), newItem.getContent());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getContent().equals(newItem.getContent())
                    && Objects.equals(oldItem.getSubjectName(), newItem.getSubjectName());
        }
    };
    // -----------------------------------

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        public TextView title, content, subjectName;
        public final ColorStateList defaultCardBackgroundColor;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewNoteTitle);
            content = itemView.findViewById(R.id.textViewNoteContent);
            subjectName = itemView.findViewById(R.id.textViewNoteSubject);
            defaultCardBackgroundColor = ((CardView) itemView).getCardBackgroundColor();
        }

        public void bind(final Note note, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            if (note.getSubjectName() != null && !note.getSubjectName().isEmpty()) {
                subjectName.setText(note.getSubjectName());
                subjectName.setVisibility(View.VISIBLE);
            } else {
                subjectName.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(note, position);
                }
            });

            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(note, position);
                }
                return true;
            });
        }
    }
}