package com.zihowl.thecalendar.data.source.remote.graphql;

import com.zihowl.thecalendar.data.model.Note;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class NotesData {
    @SerializedName("todasMisNotas")
    private List<Note> todasMisNotas;
    public List<Note> getTodasMisNotas() { return todasMisNotas; }
}