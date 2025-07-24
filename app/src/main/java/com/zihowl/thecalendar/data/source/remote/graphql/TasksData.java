package com.zihowl.thecalendar.data.source.remote.graphql;

import com.zihowl.thecalendar.data.model.Task;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TasksData {
    @SerializedName("todasMisTareas")
    private List<Task> todasMisTareas;
    public List<Task> getTodasMisTareas() { return todasMisTareas; }
}