package com.xmobile.project0.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.xmobile.project0.Entities.Note;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NoteDao {

    @Query("SELECT * FROM notes WHERE note_folder_id = :folderId AND note_is_deleted = 0 ORDER BY note_position DESC")
    Single<List<Note>> getAllNotesWithPosAndId(int folderId);

    @Query("SELECT * FROM notes WHERE note_is_deleted = 0 ORDER BY note_position DESC")
    Single<List<Note>> getAllNotesWithPos();

    @Query("SELECT * FROM notes WHERE note_is_deleted = 0 ORDER BY strftime('%Y-%m-%d %H:%M', note_date_time) DESC")
    Single<List<Note>> getAllNotesWithDate();

    @Query("SELECT * FROM notes WHERE note_is_deleted = 0 ORDER BY note_title DESC")
    Single<List<Note>> getAllNotesWithTitle();

    @Query("SELECT * FROM notes WHERE note_folder_id = :folderId AND note_is_deleted = 0 ORDER BY strftime('%Y-%m-%d %H:%M', note_date_time) DESC")
    Single<List<Note>> getAllNotesWithDateAndId(int folderId);

    @Query("SELECT * FROM notes WHERE note_folder_id = :folderId AND note_is_deleted = 0 ORDER BY note_title DESC")
    Single<List<Note>> getAllNotesWithTitleAndId(int folderId);

    @Query("SELECT * FROM notes WHERE note_date_time LIKE :day AND note_is_deleted = 0")
    Single<List<Note>> getAllNotesWithDay(String day);

    @Query("SELECT * FROM notes WHERE id = :noteId AND note_is_deleted = 0")
    Maybe<Note> getNoteWithId(int noteId);

    @Query("SELECT * FROM notes WHERE note_is_deleted = 1 ORDER BY note_position DESC")
    Single<List<Note>> getAllDeletedNotesWithPos();

    @Query("SELECT COUNT(*) FROM notes WHERE note_title LIKE 'Không có tiêu đề%' AND note_folder_id = :folderId AND note_is_deleted = 0")
    Single<Integer> countNoTitle(int folderId);

    @Query("SELECT COUNT(*) FROM notes WHERE note_date_time LIKE :day AND note_is_deleted = 0")
    Single<Integer> countNotesWithDay(String day);

    @Query("SELECT COUNT(*) FROM notes WHERE note_is_deleted = 1")
    Single<Integer> countDeletedNotes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertNote(Note note);

    @Query("UPDATE notes SET note_position = :pos WHERE id = :noteId")
    Completable updatePos(int noteId, int pos);

    @Query("UPDATE notes SET note_checked = :checked WHERE id = :noteId")
    Completable updateChecked(int noteId, boolean checked);

    @Query("UPDATE notes SET note_folder_id = :folderId WHERE id = :noteId")
    Completable updateFolderId(int noteId, int folderId);

    @Query("UPDATE notes SET note_folder_name = :folderName WHERE id = :noteId")
    Completable updateFolderName(int noteId, String folderName);

    @Query("UPDATE notes SET note_is_noti = :checkNoti WHERE id = :noteId")
    Completable updateCheckNoti(int noteId, boolean checkNoti);

    @Query("UPDATE notes SET note_is_deleted = 1 WHERE id = :noteId")
    Completable deleteNote(int noteId);

    @Query("UPDATE notes SET note_is_deleted = 1 WHERE note_folder_id = :folderId")
    Completable deleteAllNotes(int folderId);

    @Query("DELETE FROM notes WHERE id = :noteId")
    Completable deleteNoteFromTrash(int noteId);

    @Query("UPDATE notes SET note_is_deleted = 0 WHERE id = :noteId")
    Completable restoreNoteFromTrash(int noteId);
}
