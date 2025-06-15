package com.xmobile.project0.Activity;

import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.R;
import com.xmobile.project0.databinding.ActivityDeletedNotesBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class DeletedNotesActivity extends AppCompatActivity implements ItemClickListener {
    private ActivityDeletedNotesBinding binding;
    private List<Note> notes = new ArrayList<>();
    private NoteAdapter adapter;
    private int stateNote = 1;
    private AlertDialog dialogDeleteAllNote, dialogRestoreAllNote, dialogChooseOption;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeletedNotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initControl();
    }

    private void initControl() {
        loadDeletedNote();

        //back
        binding.backLayout.setOnClickListener(v -> finish());

        binding.imgChoose.setOnClickListener(v -> chooseNoteMode());
    }

    private void loadDeletedNote() {
        NoteDatabase db = NoteDatabase.getDatabase(this);

        compositeDisposable.add(
                db.noteDao().getAllDeletedNotesWithPos() // Trả về Single<List<Note>>
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    notes = result;
                                    adapter = new NoteAdapter(notes, DeletedNotesActivity.this);
                                    binding.recyclerViewDeletedNote.setLayoutManager(new LinearLayoutManager(DeletedNotesActivity.this));
                                    binding.recyclerViewDeletedNote.setAdapter(adapter);
                                    binding.imgChoose.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
                                },
                                throwable -> {
                                    Log.e("DeletedNotesActivity", "Error loading deleted notes", throwable);
                                }
                        )
        );
    }

    private void chooseNoteMode() {
        stateNote = 0;
        updateUIState();
        adapter.setTypeNote(stateNote);
        binding.txtDone.setOnClickListener(v -> {
            stateNote = 1;
            updateUIState();
            adapter.setTypeNote(stateNote);
        });

        binding.txtDelete.setOnClickListener(v -> handleNoteAction(true));
        binding.txtRestore.setOnClickListener(v -> handleNoteAction(false));
    }


    private void handleNoteAction(boolean isDelete) {
        List<Note> checked = getCheckedNotes();
        if (checked.isEmpty()) {
            showConfirmDialog(isDelete);
        } else {
            performNoteAction(checked, isDelete);
        }
    }

    private void showConfirmDialog(boolean isDelete) {
        if ((isDelete && dialogDeleteAllNote != null) || (!isDelete && dialogRestoreAllNote != null)) {
            (isDelete ? dialogDeleteAllNote : dialogRestoreAllNote).show();
            return;
        }

        View view = LayoutInflater.from(this).inflate(isDelete ? R.layout.layout_deleteallnote : R.layout.layout_restoreallnote, null);
        AlertDialog dialog = new AlertDialog.Builder(this).setView(view).create();

        view.findViewById(R.id.txtConfirm).setOnClickListener(v -> {
            performNoteAction(new ArrayList<>(notes), isDelete);
            dialog.dismiss();
        });

        view.findViewById(R.id.txtCancel).setOnClickListener(v -> dialog.dismiss());

        if (isDelete) dialogDeleteAllNote = dialog;
        else dialogRestoreAllNote = dialog;

        dialog.show();
    }

    private void updateUIState(){
        int dp15 = (int) (15 * getResources().getDisplayMetrics().density);
        int dp75 = (int) (75 * getResources().getDisplayMetrics().density);
        boolean isNormalState = stateNote == 1;

        binding.backLayout.setVisibility(isNormalState ? View.VISIBLE : View.INVISIBLE);
        binding.imgChoose.setVisibility(isNormalState ? View.VISIBLE : View.INVISIBLE);
        binding.txtDone.setVisibility(isNormalState ? View.INVISIBLE : View.VISIBLE);
        binding.layoutModify.setVisibility(isNormalState ? View.INVISIBLE : View.VISIBLE);
        binding.recyclerViewDeletedNote.setPadding(dp15, 0, dp15, isNormalState ? dp15 : dp75);
    }

    private List<Note> getCheckedNotes() {
        List<Note> checked = new ArrayList<>();
        for (Note note : notes) {
            if (note.isChecked()) checked.add(note);
        }
        return checked;
    }


    private void performNoteAction(List<Note> targetNotes, boolean isDelete) {
        NoteDatabase db = NoteDatabase.getDatabase(DeletedNotesActivity.this);

        List<Completable> tasks = new ArrayList<>();

        for (Note note : targetNotes) {
            Completable task = isDelete
                    ? db.noteDao().deleteNoteFromTrash(note.getId())
                    : db.noteDao().restoreNoteFromTrash(note.getId());

            tasks.add(task.doOnComplete(() -> notes.remove(note)));
        }

        compositeDisposable.add(
                Completable.merge(tasks)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    adapter.notifyDataSetChanged();
                                    binding.imgChoose.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
                                },
                                throwable -> {
                                    Log.e("NoteActionRx", "Lỗi khi thực hiện hành động ghi chú", throwable);
                                    Toast.makeText(this, "Lỗi khi thực hiện hành động", Toast.LENGTH_SHORT).show();
                                }
                        )
        );

    }

    @Override
    public void onNoteClicked(Note note) {
        if (dialogChooseOption == null) {
            View view = LayoutInflater.from(this).inflate(R.layout.layout_choose_option, null);
            dialogChooseOption = new AlertDialog.Builder(this).setView(view).create();

            view.findViewById(R.id.txtDelete).setOnClickListener(v -> {
                performNoteAction(List.of(note), true);
                dialogChooseOption.dismiss();
            });

            view.findViewById(R.id.txtRestore).setOnClickListener(v -> {
                performNoteAction(List.of(note), false);
                dialogChooseOption.dismiss();
            });

            view.findViewById(R.id.txtCancel).setOnClickListener(v -> dialogChooseOption.dismiss());
        }
        dialogChooseOption.show();
    }

    @Override
    public void onTagClicked(String tag, int position) {

    }

    @Override
    public void onNotiClicked(Noti noti, int type) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dialogDeleteAllNote != null) dialogDeleteAllNote.dismiss();
        if (dialogRestoreAllNote != null) dialogRestoreAllNote.dismiss();
        if (dialogChooseOption != null) dialogChooseOption.dismiss();
        adapter.clear();
        dialogDeleteAllNote = null;
        dialogRestoreAllNote = null;
        dialogChooseOption = null;
        binding = null;
        compositeDisposable.clear();
    }
}