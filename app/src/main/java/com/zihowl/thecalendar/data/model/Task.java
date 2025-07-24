package com.zihowl.thecalendar.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Representa una Tarea en la aplicación.
 * Este objeto está configurado para ser persistido en Realm y también
 * para ser serializado/deserializado por GSON para la comunicación con la API.
 */
public class Task extends RealmObject implements Serializable {

    @PrimaryKey
    private int id;

    @SerializedName("titulo") // Mapea el campo 'titulo' del JSON a 'title'
    private String title;

    @SerializedName("descripcion") // Mapea 'descripcion' a 'description'
    private String description;

    @SerializedName("fecha_entrega") // Mapea 'fecha_entrega' a 'dueDate'
    private Date dueDate;

    @SerializedName("completada") // Mapea 'completada' a 'isCompleted'
    private boolean isCompleted;

    // Campo para uso local en la UI. No se sincroniza directamente.
    private String subjectName;

    // --- Campo para la API ---
    // GSON lo usará para enviar el ID de la materia al crear una nueva tarea.
    // 'transient' significa que Realm ignorará este campo y no lo guardará en la BD local.
    @SerializedName("id_materia")
    private transient Integer subjectId;

    // Usuario dueño del registro
    private String owner;

    // Constructor vacío requerido por Realm
    public Task() {}

    // Constructor principal utilizado en la app
    public Task(String title, String description, Date dueDate, String subjectName) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.isCompleted = false;
        this.subjectName = subjectName;
    }

    // --- Getters y Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Date getDueDate() { return dueDate; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    // Getters y setters para el campo que solo usa la API
    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }


    // --- Métodos de igualdad (equals y hashCode) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}