package com.xmobile.project0.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "tags")
public class Tag {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "tag_note_id")
    private int idNote;

    @ColumnInfo(name = "tag_name")
    private String nameTag;

    public Tag(int idNote, String nameTag) {
        this.idNote = idNote;
        this.nameTag = nameTag;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdNote() {
        return idNote;
    }

    public void setIdNote(int idNote) {
        this.idNote = idNote;
    }

    public String getNameTag() {
        return nameTag;
    }

    public void setNameTag(String nameTag) {
        this.nameTag = nameTag;
    }
}
