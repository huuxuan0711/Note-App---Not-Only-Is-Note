package com.xmobile.project0.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;

import java.io.Serializable;

@Entity(tableName = "notesLink", primaryKeys = {"note_id_from", "note_id_to"})
public class NoteLink implements Serializable {
    @ColumnInfo(name = "note_id_from")
    private int idFrom;

    @ColumnInfo(name = "note_id_to")
    private int idTo;

    @ColumnInfo(name = "link_date_time")
    private String dateTime;

    public NoteLink(int idFrom, int idTo, String dateTime) {
        this.idFrom = idFrom;
        this.idTo = idTo;
        this.dateTime = dateTime;
    }

    public int getIdFrom() {
        return idFrom;
    }

    public void setIdFrom(int idFrom) {
        this.idFrom = idFrom;
    }

    public int getIdTo() {
        return idTo;
    }

    public void setIdTo(int idTo) {
        this.idTo = idTo;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}
