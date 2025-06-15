package com.xmobile.project0.Activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.xmobile.project0.Adapter.FolderAdapter;
import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.DAO.NoteDao;
import com.xmobile.project0.DAO.TagDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Folder;
import com.xmobile.project0.Entities.NewTag;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.Entities.Tag;
import com.xmobile.project0.Helper.FinishRequestListener;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.Helper.ItemLongClickListener;
import com.xmobile.project0.Helper.MyItemTouchHelper;
import com.xmobile.project0.Helper.MySwipeHelper;
import com.xmobile.project0.databinding.ActivityListNoteBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ListNoteActivity extends BaseActivity implements ItemLongClickListener, ItemClickListener, FinishRequestListener {

    private ActivityListNoteBinding binding;
    private int idFolder;
    private String nameFolder;
    private NoteAdapter adapter;
    private FolderAdapter folderAdapter;
    private List<Note> notes = new ArrayList<>();
    List<Folder> folders;
    List<NewTag> newTags = new ArrayList<>();
    private int stateList = 1;  // 1: list, 0: collection
    private int stateNote = 1;  // 1: normal, 0: selection mode
    private int idNote;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private NoteDao noteDao;

    @SuppressLint("RestrictedApi")
    private MenuBuilder menuBuilder, menuBuilder2;
    private MenuInflater inflater, inflater2;

    AlertDialog dialogDeleteNote;
    MySwipeHelper mySwipeHelper;
    RecyclerView recyclerViewFolderMove;

    private static final int PADDING_DP_15 = 15;
    private static final int PADDING_DP_75 = 75;

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        noteDao = NoteDatabase.getDatabase(this).noteDao();
        initMenu();
        initControl();

    }

    private void initControl() {

        loadIntentData();
        setupDisplay();

        binding.backLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
        binding.createNote.setOnClickListener(v -> createNote());
        binding.menuListNote.setOnClickListener(this::optionMenu);
        binding.searchLayout.setOnClickListener(v -> {
            startActivity(new Intent(this, SearchActivity.class));
            finish();
        });

        updateTagsFromIntent();
        loadNotes();
    }

    private void updateTagsFromIntent() {
        Object tagsObj = getIntent().getSerializableExtra("newTagsFromNoteActivity");
        if (tagsObj != null){
            newTags = (List<NewTag>) tagsObj;
            TagDao tagDao = NoteDatabase.getDatabase(ListNoteActivity.this).tagDao();

            List<Completable> completables = new ArrayList<>();
            for (NewTag newTag : newTags) {
                Tag tag = new Tag(idNote, newTag.getName());
                completables.add(tagDao.insertTag(tag));  // insertTag() trả về Completable
            }

            compositeDisposable.add(
                    Completable.merge(completables)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> {
                                        // Thành công (nếu cần thực hiện gì sau khi insert xong)
                                    },
                                    throwable -> {
                                        Log.e("UpdateNewTag", "Lỗi khi thêm tag", throwable);
                                    }
                            )
            );
        }
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        idFolder = intent.getIntExtra("folderId", 0);
        nameFolder = intent.getStringExtra("folderName");
    }

    private void setupDisplay() {
        binding.nameFolder.setText(nameFolder);
    }

    private void modifyList(){
        int padding15Px = (int) (PADDING_DP_15 * getResources().getDisplayMetrics().density);
        int padding75Px = (int) (PADDING_DP_75 * getResources().getDisplayMetrics().density);

        boolean selectionMode = (stateNote == 0);

        binding.backLayout.setVisibility(selectionMode ? View.INVISIBLE : View.VISIBLE);
        binding.menuListNote.setVisibility(selectionMode ? View.INVISIBLE : View.VISIBLE);
        binding.createNote.setVisibility(selectionMode ? View.INVISIBLE : View.VISIBLE);

        binding.txtDone.setVisibility(selectionMode ? View.VISIBLE : View.INVISIBLE);
        binding.layoutModify.setVisibility(selectionMode ? View.VISIBLE : View.INVISIBLE);

        int bottomPadding = selectionMode ? padding75Px : padding15Px;
        binding.recyclerViewListNote.setPadding(padding15Px, 0, padding15Px, bottomPadding);
    }

    @SuppressLint("RestrictedApi")
    private void initMenu(){
        menuBuilder = new MenuBuilder(this);
        inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_option_modify_note_list, menuBuilder);

        menuBuilder2 = new MenuBuilder(this);
        inflater2 = new MenuInflater(this);
        inflater2.inflate(R.menu.menu_option_item_collection, menuBuilder2);
    }

    @SuppressLint("RestrictedApi")
    private void optionMenu(View anchor) {
        boolean isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        binding.menuListNote.setBackgroundResource(isDarkMode ? R.drawable.bg_pale_darkblue : R.drawable.bg_pale_blue);
        binding.menuListNote.setColorFilter(ContextCompat.getColor(this, R.color.pale_lightGray), PorterDuff.Mode.SRC_IN);

        MenuPopupHelper popup = new MenuPopupHelper(this, menuBuilder, anchor);
        popup.setForceShowIcon(true);
        popup.setGravity(Gravity.END);

        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                //Đổi sang dạng khác
                if (item.getItemId() == R.id.menu_settype){
                    toggleListType(menu);
                    return true;
                    //Chọn note
                } else if (item.getItemId() == R.id.menu_choose) {
                    enterSelectionMode();
                    return true;
                } else if (item.getItemId() == R.id.menu_sort_date) {
                    sortNotesByDate();
                    return true;
                }else if (item.getItemId() == R.id.menu_sort_title) {
                    sortNotesByTitle();
                    return true;
                }
                return false;
            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {

            }
        });

        popup.setOnDismissListener(() -> {
            binding.menuListNote.setBackgroundResource(R.drawable.bg_round_btn);
            binding.menuListNote.setColorFilter(ContextCompat.getColor(this, R.color.lightGray), PorterDuff.Mode.SRC_IN);
        });

        popup.show();
    }

    @SuppressLint("RestrictedApi")
    private void toggleListType(@SuppressLint("RestrictedApi") MenuBuilder menu) {
        attachSwipe();
        stateList = 1 - stateList;
        menu.clear();

        if (stateList == 1) {
            inflater.inflate(R.menu.menu_option_modify_note_list, menu);
            binding.recyclerViewListNote.setLayoutManager(new LinearLayoutManager(this));
        } else {
            inflater.inflate(R.menu.menu_option_modify_note_collection, menu);
            binding.recyclerViewListNote.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        }
        adapter.setTypeList(stateList);
    }

    private void enterSelectionMode() {
        stateNote = 0;
        modifyList();
        adapter.setTypeNote(stateNote);

        binding.txtDone.setOnClickListener(v -> {
            stateNote = 1;
            modifyList();
            adapter.setTypeNote(stateNote);
        });

        binding.txtDelete.setOnClickListener(v -> deleteNoteFromChecked());
        binding.txtMove.setOnClickListener(v -> moveNoteFromChecked());
    }

    @SuppressLint("RestrictedApi")
    private void longClickMenu(int pos, View v){
        MenuPopupHelper popup = new MenuPopupHelper(this, menuBuilder2, v);
        popup.setForceShowIcon(true);
        popup.setGravity(Gravity.FILL);

        menuBuilder2.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_item_share){
                    exportNoteAtPosition(pos);
                    return true;
                } else if (item.getItemId() == R.id.menu_item_move) {
                    showBottomSheetMove(pos);
                    return true;
                } else if (item.getItemId() == R.id.menu_item_delete) {
                    deleteNoteFromSwipe(pos);
                    return true;
                }
                return false;
            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {

            }
        });

        //Đặt lại trạng thái
        popup.setOnDismissListener(() -> {
            View child = binding.recyclerViewListNote.getChildAt(pos);
            if (child != null) child.setBackgroundResource(R.drawable.bg_note);
        });

        popup.show();
    }

    private void sortNotesByTitle() {
        NoteDao noteDao = NoteDatabase.getDatabase(this).noteDao();
        Single<List<Note>> noteSingle;

        if (getString(R.string.all_notes).equals(nameFolder)) {
            noteSingle = noteDao.getAllNotesWithTitle();
        } else {
            noteSingle = noteDao.getAllNotesWithTitleAndId(idFolder);
        }

        compositeDisposable.add(
                noteSingle
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                sortedNotes -> {
                                    notes = sortedNotes;
                                    adapter.setNotes(notes);
                                },
                                throwable -> {
                                    Toast.makeText(this, "Lỗi khi sắp xếp ghi chú", Toast.LENGTH_SHORT).show();
                                    Log.e("RxJava", "sortNotesByTitle error", throwable);
                                }
                        )
        );
    }


    private void sortNotesByDate() {
        NoteDao noteDao = NoteDatabase.getDatabase(this).noteDao();
        Single<List<Note>> noteSingle;

        if (getString(R.string.all_notes).equals(nameFolder)) {
            noteSingle = noteDao.getAllNotesWithDate();
        } else {
            noteSingle = noteDao.getAllNotesWithDateAndId(idFolder);
        }

        compositeDisposable.add(
                noteSingle
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                sortedNotes -> {
                                    notes = sortedNotes;
                                    adapter.setNotes(notes);
                                },
                                throwable -> {
                                    Toast.makeText(this, "Lỗi khi sắp xếp ghi chú theo ngày", Toast.LENGTH_SHORT).show();
                                    Log.e("RxJava", "sortNotesByDate error", throwable);
                                }
                        )
        );
    }


    private void loadNotes(){
        Single<List<Note>> noteSingle;

        if (getString(R.string.all_notes).equals(nameFolder)) {
            noteSingle = noteDao.getAllNotesWithPos();
        } else {
            noteSingle = noteDao.getAllNotesWithPosAndId(idFolder);
        }

        compositeDisposable.add(
                noteSingle
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                loadedNotes -> {
                                    notes = loadedNotes;
                                    setupRecyclerView();
                                    if (!notes.isEmpty()) {
                                        idNote = notes.get(0).getId();
                                    } else {
                                        binding.menuListNote.setVisibility(View.GONE);
                                    }
                                },
                                throwable -> Log.e("LoadNotes", "Error loading notes", throwable)
                        )
        );
    }

    private void setupRecyclerView() {
        binding.recyclerViewListNote.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NoteAdapter(notes, this, this);
        attachSwipe();
        attachMove();
        binding.recyclerViewListNote.setAdapter(adapter);
    }

    private void exportNoteAtPosition(int pos) {
        Note note = notes.get(pos);
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isUpdate", true);
        intent.putExtra("isExport", true);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        finish();
    }

    private void moveNoteFromSwipe(int pos) {
        showBottomSheetMove(pos);
    }

    private void moveNoteFromChecked() {
        showBottomSheetMove(-1);
    }

    @SuppressLint("SetTextI18n")
    private void showBottomSheetMove(int pos) {
        ConstraintLayout bottomSheet = findViewById(R.id.bottomSheetMove);
        View dimView = findViewById(R.id.dimViewMove);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        dimView.setVisibility(View.VISIBLE);
        dimView.setOnTouchListener((v, event) -> true);

        TextView txtCancel = bottomSheet.findViewById(R.id.txtCancel);
        TextView titleMove = bottomSheet.findViewById(R.id.titleMove);
        TextView countMove = bottomSheet.findViewById(R.id.countMove);
        recyclerViewFolderMove = bottomSheet.findViewById(R.id.recyclerViewFolderMove);

        txtCancel.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));

        List<Note> checkedNotes = noteCheckedList();

        if (pos >= 0){
            titleMove.setText(notes.get(pos).getTitle());
            countMove.setText(getString(R.string.one_note));
        }else {
            List<Note> listToShow = checkedNotes.isEmpty() ? notes : checkedNotes;
            titleMove.setText(joinTitles(listToShow));
            countMove.setText(String.format("%d %s", listToShow.size(), getString(R.string.notes)));
        }

        loadFolderList();

        recyclerViewFolderMove.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());
                    if (childView != null) {
                        int position = rv.getChildAdapterPosition(childView);
                        handleFolderItemClick(position, pos, checkedNotes, bottomSheetBehavior);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                dimView.setVisibility(newState == BottomSheetBehavior.STATE_EXPANDED ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dimView.setAlpha(slideOffset);
            }
        });
    }

    private String joinTitles(List<Note> notesList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < notesList.size(); i++) {
            sb.append(notesList.get(i).getTitle());
            if (i < notesList.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private void handleFolderItemClick(int folderPos, int notePos, List<Note> checkedNotes, BottomSheetBehavior<View> bottomSheetBehavior) {
        String selectedFolderName = folders.get(folderPos).getName();
        if (selectedFolderName.equals(getString(R.string.all_notes))) return;

        NoteDao noteDao = NoteDatabase.getDatabase(ListNoteActivity.this).noteDao();
        List<Completable> completables = new ArrayList<>();

        if (notePos >= 0) {
            int noteId = notes.get(notePos).getId();
            completables.add(noteDao.updateFolderId(noteId, folders.get(folderPos).getId()));
            completables.add(noteDao.updateFolderName(noteId, selectedFolderName));
        } else {
            for (Note note : checkedNotes) {
                int noteId = note.getId();
                completables.add(noteDao.updateFolderId(noteId, folders.get(folderPos).getId()));
                completables.add(noteDao.updateFolderName(noteId, selectedFolderName));
            }
        }

        compositeDisposable.add(
                Completable.merge(completables)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                    loadNotes();
                                },
                                throwable -> {
                                    Log.e("UpdateFolderId", "Lỗi khi cập nhật folder", throwable);
                                }
                        )
        );
    }

    private void deleteNoteFromSwipe(int pos) {
        int noteId = notes.get(pos).getId();
        NoteDatabase db = NoteDatabase.getDatabase(ListNoteActivity.this);

        Completable deleteAll = Completable.mergeArray(
                db.noteDao().deleteNote(noteId),
                db.notiDao().deleteNotiWithIdNote(noteId),
                db.tagDao().deleteAllTagWithId(noteId),
                db.noteLinkDao().deleteAllNoteLinksWithNoteId(noteId)
        );

        compositeDisposable.add(
                deleteAll
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    notes.remove(pos);
                                    adapter.notifyItemRemoved(pos);
                                    if (notes.isEmpty()) {
                                        binding.menuListNote.setVisibility(View.GONE);
                                    }
                                },
                                throwable -> {
                                    Log.e("DeleteNote", "Lỗi khi xóa note", throwable);
                                }
                        )
        );
    }

    private void deleteNoteFromChecked() {
        List<Note> checked = noteCheckedList();
        if (checked.isEmpty()) {
            showDeleteAllNotesDialog();
        } else {
            deleteCheckedNotes(checked);
        }
    }

    private void showDeleteAllNotesDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ListNoteActivity.this);
            View view = LayoutInflater.from(ListNoteActivity.this).inflate(R.layout.layout_deleteallnote, null);
            builder.setView(view);
            dialogDeleteNote = builder.create();

            view.findViewById(R.id.txtConfirm).setOnClickListener(v -> {
                NoteDatabase db = NoteDatabase.getDatabase(ListNoteActivity.this);

                Completable deleteAll = Completable.fromAction(() -> {
                    for (Note note : notes) {
                        db.notiDao().deleteNotiWithIdNote(note.getId()).blockingAwait();
                        db.tagDao().deleteAllTagWithId(note.getId()).blockingAwait();
                        db.noteLinkDao().deleteAllNoteLinksWithNoteId(note.getId()).blockingAwait();
                    }
                    db.noteDao().deleteAllNotes(idFolder).blockingAwait();
                    notes.clear();
                });

                compositeDisposable.add(
                        deleteAll
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(
                                        () -> {
                                            adapter.notifyDataSetChanged();
                                            dialogDeleteNote.dismiss();
                                            if (notes.isEmpty()) {
                                                binding.menuListNote.setVisibility(View.GONE);
                                            }
                                        },
                                        throwable -> Log.e("DelAllNote", "Lỗi khi xóa tất cả note", throwable)
                                )
                );
            });

            view.findViewById(R.id.txtCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }
        dialogDeleteNote.show();
    }

    private void deleteCheckedNotes(List<Note> checked) {
        NoteDatabase db = NoteDatabase.getDatabase(ListNoteActivity.this);

        Completable deleteTask = Completable.fromAction(() -> {
            for (Note note : new ArrayList<>(checked)) {
                int noteId = note.getId();
                db.noteDao().deleteNote(noteId).blockingAwait();
                db.notiDao().deleteNotiWithIdNote(noteId).blockingAwait();
                db.tagDao().deleteAllTagWithId(noteId).blockingAwait();
                db.noteLinkDao().deleteAllNoteLinksWithNoteId(noteId).blockingAwait();
                notes.remove(note);
            }
        });

        compositeDisposable.add(
                deleteTask
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    adapter.notifyDataSetChanged();
                                    if (notes.isEmpty()) {
                                        binding.menuListNote.setVisibility(View.GONE);
                                    }
                                },
                                throwable -> Log.e("DelNote", "Lỗi khi xóa ghi chú", throwable)
                        )
        );
    }

    private List<Note> noteCheckedList(){
        List<Note> list = new ArrayList<>();
        for (Note note : notes) {
            if (note.isChecked()) {
                list.add(note);
            }
        }
        return list;
    }

    private void attachSwipe() {
        if (mySwipeHelper == null){
            mySwipeHelper = new MySwipeHelper(this, binding.recyclerViewListNote, 200) {
                @Override
                public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer) {
                    buffer.clear();
                    if (stateList == 1 && stateNote == 1){
                        buffer.add(new MyButton(ListNoteActivity.this,
                                getString(R.string.delete),
                                30,
                                R.drawable.ic_delete,
                                Color.parseColor("#FF0000"),
                                pos -> {
                                    deleteNoteFromSwipe(pos);
                                }));
                        buffer.add(new MyButton(ListNoteActivity.this,
                                getString(R.string.move),
                                30,
                                R.drawable.ic_move,
                                Color.parseColor("#252836"),
                                pos -> {
                                    moveNoteFromSwipe(pos);
                                }));
                        buffer.add(new MyButton(ListNoteActivity.this,
                                getString(R.string.share),
                                30,
                                R.drawable.ic_share,
                                Color.parseColor("#007AFF"),
                                pos -> {
                                    exportNoteAtPosition(pos);
                                }));
                    }
                }
            };
        }
    }

    private void attachMove(){
        ItemTouchHelper.Callback callback = new MyItemTouchHelper(this ,adapter, 1);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        adapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewListNote);
    }

    private void createNote() {
        Intent intent = new Intent(ListNoteActivity.this, NoteActivity.class);
        intent.putExtra("folderId", idFolder);
        intent.putExtra("folderName", nameFolder);
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
        finish();
    }

    private void loadFolderList() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).folderDao().getAllFolders()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                folders -> {
                                    this.folders = folders; // gán nếu dùng ở chỗ khác
                                    recyclerViewFolderMove.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
                                    folderAdapter = new FolderAdapter(folders, this, this);
                                    folderAdapter.setState(2);
                                    recyclerViewFolderMove.setAdapter(folderAdapter);
                                },
                                throwable -> {
                                    Toast.makeText(this, "Lỗi khi tải danh sách thư mục", Toast.LENGTH_SHORT).show();
                                    Log.e("RxJava", "loadFolderList error", throwable);
                                }
                        )
        );
    }


    @Override
    public void onItemLongClick(int pos, View v) {
        longClickMenu(pos, v);
    }

    @Override
    public void onNoteClicked(Note note) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isUpdate", true);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        finish();
    }

    @Override
    public void onTagClicked(String tag, int position) {

    }

    @Override
    public void onNotiClicked(Noti noti, int type) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == REQUEST_CODE_ADD_NOTE || requestCode == REQUEST_CODE_UPDATE_NOTE) && resultCode == RESULT_OK) {
            loadNotes();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adapter = null;
        mySwipeHelper = null;
        binding = null;
        folderAdapter = null;
        recyclerViewFolderMove = null;
        dialogDeleteNote = null;
        folders = null;
        notes = null;
        menuBuilder = null;
        menuBuilder2 = null;
        inflater = null;
        inflater2 = null;
        compositeDisposable.clear();
    }

    @Override
    public void onFinishRequested() {
        finish();
    }
}

//done
/*menu modify
chế độ xem: dạng bộ sưu tập, dạng danh sách
sắp xếp ghi chú theo ngày sửa (mặc định mới->cũ), tiêu đề (a->z)
chọn ghi chú -> di chuyển/xóa
 */

//tìm kiếm giống trong homefragment

/*item //còn phần xuất ghi chú, ấn giữ item collection
vuốt ngang item list -> chía sẻ/di chuyển/xóa
ấn giữ item collection -> chía sẻ/di chuyển/xóa
cấu trúc: dạng danh sách
    tiêu đề, mô tả, ngày tạo, ảnh đầu tiên
cấu trúc: dạng bộ sưu tập
    giống app note mẫu
chế độ chọn:
    hiển thị check box ở bên trái
    không có item nào được choọn -> di chuyển tất cả/xóa tất cả
    có item nào được chọn -> di chuyển/xóa
    di chuyển -> hiển thị bottom sheet danh sách thư mục
    xóa -> gọi đến delete
    chia sẻ -> xuất dạng pdf
 */

//done
/*tạo note
gửi id, name
 */

//còn tìm kiếm, soạn thảo, lịch, đa dạng các loại note, nhắc nhở note,
//tìm kiếm: chuyển qua fragment tìm kiếm
//xóa -> cho vào thùng rác