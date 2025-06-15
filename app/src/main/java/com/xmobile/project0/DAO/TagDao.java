package com.xmobile.project0.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.xmobile.project0.Entities.Tag;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface TagDao {
    @Query("SELECT * FROM tags")
    Single<List<Tag>> getAllTags();

    @Query("SELECT COUNT(*) FROM tags WHERE tag_name = :nameTag")
    Single<Integer> countNoteWithTag(String nameTag);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertTag(Tag tag);

    @Query("DELETE FROM tags WHERE tag_note_id = :idNote AND tag_name = :nameTag")
    Completable deleteTag(int idNote, String nameTag);

    @Query("DELETE FROM tags WHERE tag_note_id = :idNote")
    Completable deleteAllTagWithId(int idNote);
}
