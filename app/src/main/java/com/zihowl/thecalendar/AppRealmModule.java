package com.zihowl.thecalendar;

import com.zihowl.thecalendar.data.model.Note;
import com.zihowl.thecalendar.data.model.Subject;
import com.zihowl.thecalendar.data.model.Task;

import io.realm.annotations.RealmModule;

// Declaramos un módulo que contiene todas nuestras clases de RealmObject
@RealmModule(classes = {Subject.class, Task.class, Note.class})
public class AppRealmModule {
}