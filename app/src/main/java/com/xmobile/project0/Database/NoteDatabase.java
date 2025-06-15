package com.xmobile.project0.Database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.xmobile.project0.DAO.FolderDao;
import com.xmobile.project0.DAO.NoteDao;
import com.xmobile.project0.DAO.NoteLinkDao;
import com.xmobile.project0.DAO.NotiDao;
import com.xmobile.project0.DAO.TagDao;
import com.xmobile.project0.Entities.Folder;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.NoteLink;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.Entities.Tag;

@Database(entities = {Note.class, Folder.class, Tag.class, Noti.class, NoteLink.class}, version = 3, exportSchema = false)
public abstract class NoteDatabase extends RoomDatabase {

    private static NoteDatabase noteDatabase;

//    private static final Migration migration = new Migration(5, 6) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            database.execSQL("ALTER TABLE notes ADD COLUMN note_path_image TEXT NOT NULL DEFAULT ''");
//        }
//    };

    public static synchronized NoteDatabase getDatabase(Context context){
        if (noteDatabase == null){
            noteDatabase = Room.databaseBuilder(
                    context.getApplicationContext(),
                    NoteDatabase.class,
                    "notes_db"
            ).fallbackToDestructiveMigration().fallbackToDestructiveMigration().build();
        }
        return noteDatabase;
    }

    public static void destroyInstance() {
        noteDatabase = null;
    }

    public abstract NoteDao noteDao();
    public abstract FolderDao folderDao();
    public abstract TagDao tagDao();
    public abstract NotiDao notiDao();
    public abstract NoteLinkDao noteLinkDao();
}
