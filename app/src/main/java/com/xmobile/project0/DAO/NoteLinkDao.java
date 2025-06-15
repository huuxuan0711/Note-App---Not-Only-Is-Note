package com.xmobile.project0.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.xmobile.project0.Entities.NoteLink;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NoteLinkDao {
    @Query("SELECT * FROM notesLink WHERE note_id_from = :noteId")
    Single<List<NoteLink>> getNoteLinksWithNoteId(int noteId);

    @Query("SELECT * FROM notesLink")
    Single<List<NoteLink>> getAllNoteLinks();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertNoteLink(NoteLink noteLink);

    @Query("DELETE FROM notesLink WHERE note_id_from = :noteIdFrom AND note_id_to = :noteIdTo")
    Completable deleteNoteLinksWithNoteId(int noteIdFrom, int noteIdTo);

    @Query("DELETE FROM notesLink WHERE note_id_from = :noteId")
    Completable deleteAllNoteLinksWithNoteId(int noteId);

    @Query("UPDATE notesLink SET link_date_time = :dateTime WHERE note_id_from = :noteIdFrom")
    Completable updateDateTime(int noteIdFrom, String dateTime);
}
