package com.zihowl.thecalendar.data.source.local;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.model.PendingOperation;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;
import java.util.List;

public class RealmDataSource {

    private int getNextId(Realm realm, Class<? extends RealmObject> clazz) {
        Number maxId = realm.where(clazz).max("id");
        return (maxId == null) ? 1 : maxId.intValue() + 1;
    }

    // --- Subject ---
    public List<Subject> getAllSubjectsForOwner(String owner) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(
                    realm.where(Subject.class).equalTo("owner", owner).findAll()
            );
        }
    }

    public void saveSubject(Subject subject) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                if (subject.getId() == 0) {
                    subject.setId(getNextId(r, Subject.class));
                }
                r.insertOrUpdate(subject);
            });
        }
    }

    public Subject getSubjectById(int id) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Subject subject = realm.where(Subject.class).equalTo("id", id).findFirst();
            return subject != null ? realm.copyFromRealm(subject) : null;
        }
    }

    /**
     * Obtiene una materia por su nombre exacto.
     */
    public Subject getSubjectByName(String name) {
        try (Realm realm = Realm.getDefaultInstance()) {
            Subject subject = realm.where(Subject.class).equalTo("name", name).findFirst();
            return subject != null ? realm.copyFromRealm(subject) : null;
        }
    }

    public void updateSubjectCounters(int subjectId, int taskCount, int noteCount) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                Subject subject = r.where(Subject.class).equalTo("id", subjectId).findFirst();
                if (subject != null) {
                    subject.setTasksPending(taskCount);
                    subject.setNotesCount(noteCount);
                }
            });
        }
    }

    // --- Task ---
    public List<Task> getAllTasksForOwner(String owner) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(
                    realm.where(Task.class).equalTo("owner", owner).findAll()
            );
        }
    }

    public void saveTask(Task task) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                if (task.getId() == 0) {
                    task.setId(getNextId(r, Task.class));
                }
                r.insertOrUpdate(task);
            });
        }
    }

    public void deleteTasks(List<Task> tasks) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                for (Task task : tasks) {
                    r.where(Task.class).equalTo("id", task.getId()).findAll().deleteAllFromRealm();
                }
            });
        }
    }

    // --- Note ---
    public List<Note> getAllNotesForOwner(String owner) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(
                    realm.where(Note.class).equalTo("owner", owner).findAll()
            );
        }
    }

    public void saveNote(Note note) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                if (note.getId() == 0) {
                    note.setId(getNextId(r, Note.class));
                }
                r.insertOrUpdate(note);
            });
        }
    }

    public void deleteNotes(List<Note> notes) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                for (Note note : notes) {
                    r.where(Note.class).equalTo("id", note.getId()).findAll().deleteAllFromRealm();
                }
            });
        }
    }

    // --- MÉTODOS PARA LA LÓGICA DE BORRADO ---
    public List<Task> getTasksForSubject(String subjectName) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(realm.where(Task.class).equalTo("subjectName", subjectName).findAll());
        }
    }

    public List<Note> getNotesForSubject(String subjectName) {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(realm.where(Note.class).equalTo("subjectName", subjectName).findAll());
        }
    }

    public void disassociateAndDeleteSubject(int subjectId) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                Subject managedSubject = r.where(Subject.class).equalTo("id", subjectId).findFirst();
                if (managedSubject != null) {
                    String subjectName = managedSubject.getName();

                    // Desvincular tareas y notas
                    RealmResults<Task> tasks = r.where(Task.class).equalTo("subjectName", subjectName).findAll();
                    for (Task task : tasks) {
                        task.setSubjectName(null);
                    }
                    RealmResults<Note> notes = r.where(Note.class).equalTo("subjectName", subjectName).findAll();
                    for (Note note : notes) {
                        note.setSubjectName(null);
                    }

                    // Eliminar la materia
                    managedSubject.deleteFromRealm();
                }
            });
        }
    }

    public void cascadeDeleteSubjects(List<Integer> subjectIds) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                for (Integer id : subjectIds) {
                    Subject managedSubject = r.where(Subject.class).equalTo("id", id).findFirst();
                    if (managedSubject != null) {
                        String subjectName = managedSubject.getName();

                        // Borrar tareas y notas asociadas
                        r.where(Task.class).equalTo("subjectName", subjectName).findAll().deleteAllFromRealm();
                        r.where(Note.class).equalTo("subjectName", subjectName).findAll().deleteAllFromRealm();

                        // Eliminar la materia
                        managedSubject.deleteFromRealm();
                    }
                }
            });
        }
    }

    // --- Pending operations management ---
    public List<PendingOperation> getAllPendingOperations() {
        try (Realm realm = Realm.getDefaultInstance()) {
            return realm.copyFromRealm(realm.where(PendingOperation.class).findAll());
        }
    }

    public void savePendingOperation(PendingOperation op) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                if (op.getId() == 0) {
                    op.setId(getNextId(r, PendingOperation.class));
                }
                r.insertOrUpdate(op);
            });
        }
    }

    public void deletePendingOperation(long id) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                r.where(PendingOperation.class).equalTo("id", id).findAll().deleteAllFromRealm();
            });
        }
    }

    public void clearUserData(String owner) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.executeTransaction(r -> {
                r.where(Subject.class).equalTo("owner", owner).findAll().deleteAllFromRealm();
                r.where(Task.class).equalTo("owner", owner).findAll().deleteAllFromRealm();
                r.where(Note.class).equalTo("owner", owner).findAll().deleteAllFromRealm();
                r.where(PendingOperation.class).findAll().deleteAllFromRealm();
            });
        }
    }
}