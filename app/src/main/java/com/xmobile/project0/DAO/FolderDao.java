package com.xmobile.project0.DAO;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.xmobile.project0.Entities.Folder;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

@Dao
public interface FolderDao {

    @Query("SELECT * FROM folders")
    Single<List<Folder>> getAllFolders();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertFolder(Folder folder);

    @Query("UPDATE folders SET folder_name = :newName WHERE id = :folderId")
    Completable updateFolder(int folderId, String newName);

    @Query("DELETE FROM folders WHERE id = :folderId")
    Completable deleteFolder(int folderId);

    @Query("DELETE FROM folders")
    Completable deleteAllFolder();

    @Query("SELECT COUNT(*) FROM notes WHERE note_folder_id = :folderId AND note_is_deleted = 0")
    Single<Integer> sizeFolderWithId(int folderId);

    @Query("SELECT COUNT(*) FROM notes where note_is_deleted = 0")
    Single<Integer> sizeFolder();

    @Query("SELECT COUNT(*) FROM folders where folder_name = :name")
    Single<Integer> checkFolder(String name);

    @Query("SELECT COUNT(*) FROM folders where id = :idFolder")
    Single<Integer> checkFolderID(int idFolder);
}
