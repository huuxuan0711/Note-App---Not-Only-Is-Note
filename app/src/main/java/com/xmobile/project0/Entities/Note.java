package com.xmobile.project0.Entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "notes")
public class Note implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "note_title")
    private String title;

    @ColumnInfo(name = "note_description")
    private String description;

    @ColumnInfo(name = "note_content")
    private String content;

    @ColumnInfo(name = "note_date_time")
    private String dateTime;

    @ColumnInfo(name = "note_path_image", defaultValue = "")
    private String pathImage;

    @ColumnInfo(name = "note_folder_id")
    private int folderId;

    @ColumnInfo(name = "note_folder_name")
    private String folderName;

    @ColumnInfo(name = "note_position", defaultValue = "0")
    private int pos;

    @ColumnInfo(name = "note_checked", defaultValue = "0")
    private boolean checked;

    @ColumnInfo(name = "note_is_noti", defaultValue = "0")
    private boolean isNoti;

    @ColumnInfo(name = "note_is_deleted", defaultValue = "0")
    private boolean isDeleted;

    public Note() {

    }

    public Note(String title, String description, String dateTime) {
        this.title = title;
        this.description = description;
        this.dateTime = dateTime;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isNoti() {
        return isNoti;
    }

    public void setNoti(boolean noti) {
        isNoti = noti;
    }

    public String getPathImage() {
        return pathImage;
    }

    public void setPathImage(String pathImage) {
        this.pathImage = pathImage;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
