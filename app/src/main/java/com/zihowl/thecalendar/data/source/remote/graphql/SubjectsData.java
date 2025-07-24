package com.zihowl.thecalendar.data.source.remote.graphql;

import com.zihowl.thecalendar.data.model.Subject;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SubjectsData {
    @SerializedName("misMaterias")
    private List<Subject> misMaterias;
    public List<Subject> getMisMaterias() { return misMaterias; }
}