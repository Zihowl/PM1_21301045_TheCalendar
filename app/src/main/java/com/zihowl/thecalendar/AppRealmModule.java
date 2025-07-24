package com.zihowl.thecalendar;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;
import com.zihowl.thecalendar.data.model.PendingOperation;

import io.realm.annotations.RealmModule;

// Declaramos un módulo que contiene todas nuestras clases de RealmObject
@RealmModule(classes = {Subject.class, Task.class, Note.class, PendingOperation.class})
public class AppRealmModule {
}