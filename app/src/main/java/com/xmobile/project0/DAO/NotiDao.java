package com.xmobile.project0.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.xmobile.project0.Entities.Noti;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface NotiDao {
    @Query("SELECT * FROM notis ORDER BY id DESC")
    Single<List<Noti>> getAllNotis();

    @Query("SELECT * FROM notis WHERE idNote = :idNote")
    Single<List<Noti>> getNotisByNoteId(int idNote);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Single<Long> insertNoti(Noti noti);

    @Query("DELETE FROM notis WHERE id = :id")
    Completable deleteNoti(int id);

    @Query("DELETE FROM notis WHERE idNote = :idNote")
    Completable deleteNotiWithIdNote(int idNote);

    @Query("UPDATE notis SET isNotified = :isNotified WHERE id = :id")
    Completable updateNotified(int id, boolean isNotified);
}
