package com.xmobile.project0.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.textfield.TextInputLayout;
import com.xmobile.project0.Adapter.FolderAdapter;
import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.Adapter.TagAdapter;
import com.xmobile.project0.DAO.NoteDao;
import com.xmobile.project0.DAO.NoteLinkDao;
import com.xmobile.project0.DAO.TagDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Folder;
import com.xmobile.project0.Entities.NewTag;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.NoteLink;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.Entities.Tag;
import com.xmobile.project0.Helper.FinishRequestListener;
import com.xmobile.project0.R;
import com.xmobile.project0.Util.ScreenshotUtil;
import com.xmobile.project0.databinding.ActivityNoteBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NoteActivity extends BaseActivity implements FinishRequestListener {
    // ==== View Binding ====
    private ActivityNoteBinding binding;

    // ==== Dữ liệu ghi chú ====
    private Note currentNote = new Note();
    private int idFolder;
    private String nameFolder;
    private String noteTitle;
    private String oldContent;
    private int countNoTitle;
    private String pdfFileName;

    // ==== Danh sách dữ liệu ====
    private List<Note> notes = new ArrayList<>();
    private List<Folder> folders = new ArrayList<>();
    private List<String> existTags_name = new ArrayList<>();
    private List<Tag> existTags = new ArrayList<>();
    private List<NewTag> newTags = new ArrayList<>();
    private List<NoteLink> noteLinks = new ArrayList<>();

    // ==== Bộ nhớ tạm ====
    private final Map<String, Note> cache = new HashMap<>();

    // ==== Adapter ====
    private NoteAdapter adapter;
    private TagAdapter tagAdapter;
    private FolderAdapter folderAdapter;

    // ==== Trạng thái ====
    private boolean isSave = false;
    private boolean isTxt = false;
    private boolean isBg = false;

    // ==== Màu sắc hệ thống ====
    private String selectedTxTColor = "#FFFFFFFF";
    private String txtColorSystem = selectedTxTColor;
    private String selectedBgColor = "#202020";
    private String bgColorSystem = selectedBgColor;

    // ==== Bottom Sheet ====
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ConstraintLayout bottomSheet;
    private View dimView;

    // ==== Menu ====
    @SuppressLint("RestrictedApi")
    private MenuBuilder menuBuilder, menuBuilder2;

    // ==== Dialog ====
    private AlertDialog dialogAddWebLink;
    private AlertDialog dialogDeleteNoti;

    // ==== RecyclerView ====
    private RecyclerView recyclerViewNoteLink;
    private RecyclerView recyclerViewFolderMove;

    // ==== Shared Preferences ====
    private SharedPreferences sharedPreferences;

    // ==== Request Code ====
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_IMAGE = 2;
    private static final int REQUEST_CODE_PICK_FILE = 3;
    private static final int REQUEST_CODE_ADD_NOTI = 4;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initControl();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initControl() {
        display();
        listTag();
        getNoteLink();

        setupBackButton();
        setupTitleFocus();
        setupNotiButton();
        setupShareButton();
        setupOptionMenu();
    }

    private void setupBackButton() {
        binding.backLayout.setOnClickListener(v -> {
            extractAndSaveTag();
            saveNoteToCache();
            saveCacheToDatabase();
            updateNoteLink();
            isSave = true;

            Intent intent = null;
            Intent currentIntent = getIntent();

            if (currentIntent.getBooleanExtra("fromCalendarFragment", false)) {
                intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
                return;
            }

            int idNote = currentIntent.getIntExtra("idNote", 0);
            boolean isUpdate = currentIntent.getBooleanExtra("isUpdate", false);

            if (idNote != 0 && isUpdate) {
                finish();
                return;
            }

            intent = new Intent(NoteActivity.this, ListNoteActivity.class);
            if (newTags.size() > 0 && currentNote.getId() == 0) {
                intent.putExtra("newTagsFromNoteActivity", (Serializable) newTags);
            }
            intent.putExtra("folderId", idFolder);
            intent.putExtra("folderName", nameFolder);

            setResult(RESULT_OK, intent);
            startActivity(intent);
            finish();
        });
    }

    private void setupTitleFocus() {
        binding.titleNote.requestFocus();
    }

    private void setupNotiButton() {
        binding.btnNoti.setOnClickListener(v -> notiNote());
    }

    private void setupShareButton() {
        binding.btnShare.setOnClickListener(v -> exportNote());
    }

    private void setupOptionMenu() {
        binding.menuNote.setOnClickListener(this::optionMenu);
    }

    private void updateNoteLink() {
        List<Integer> noteIdsToAfter = extractLinkedNoteIdsFromEditor();
        List<Integer> noteIdsToBefore = new ArrayList<>();
        for (NoteLink noteLink : noteLinks){
            if (noteLink.getIdFrom() == currentNote.getId()){
                noteIdsToBefore.add(noteLink.getIdTo());
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"));
        String date = formatter.format(System.currentTimeMillis());

        NoteDatabase db = NoteDatabase.getDatabase(NoteActivity.this);

        Completable updateLinks = Completable.fromAction(() -> {
            NoteLinkDao dao = db.noteLinkDao();

            // Thêm liên kết mới
            for (int noteIdTo : noteIdsToAfter) {
                if (!noteIdsToBefore.contains(noteIdTo)) {
                    NoteLink noteLink = new NoteLink(currentNote.getId(), noteIdTo, date);
                    dao.insertNoteLink(noteLink).blockingAwait();
                }
            }

            // Xóa liên kết cũ không còn dùng nữa
            for (int noteIdTo : noteIdsToBefore) {
                if (!noteIdsToAfter.contains(noteIdTo)) {
                    dao.deleteNoteLinksWithNoteId(currentNote.getId(), noteIdTo).blockingAwait();
                }
            }
        });

        compositeDisposable.add(
                updateLinks
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d("UpdateNoteLink", "Cập nhật liên kết thành công"),
                                throwable -> Log.e("UpdateNoteLink", "Lỗi khi cập nhật liên kết", throwable)
                        )
        );
    }

    private void getNoteLink() {
        if (currentNote.getId() == 0) {
            noteLinks = new ArrayList<>();
            return;
        }

        compositeDisposable.add(
                NoteDatabase.getDatabase(this).noteLinkDao().getNoteLinksWithNoteId(currentNote.getId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                links -> noteLinks = links,
                                throwable -> Log.e("RxJava", "getNoteLink error", throwable)
                        )
        );
    }

    private void listTag() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).tagDao().getAllTags()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                tags -> {
                                    existTags = tags;
                                    existTags_name = new ArrayList<>();
                                    for (Tag tag : tags) {
                                        existTags_name.add(tag.getNameTag());
                                    }
                                },
                                throwable -> Log.e("RxJava", "listTag error", throwable)
                        )
        );
    }


    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void display() {
        Intent intent = getIntent();
        idFolder = intent.getIntExtra("folderId", 0);
        nameFolder = intent.getStringExtra("folderName");
        binding.horizontalScrollView.setVisibility(View.GONE);

        if (intent.getBooleanExtra("isUpdate", false)) {
            int idNote = intent.getIntExtra("idNote", 0);
            if (idNote != 0) {
                loadNoteById(idNote);
            } else {
                currentNote = (Note) intent.getSerializableExtra("note");
                updateUIWithNote(currentNote);
            }

            if (intent.getSerializableExtra("noti") != null) {
                Noti noti = (Noti) intent.getSerializableExtra("noti");
                updateNotiStatus(noti);
            }
        }

        if (intent.getBooleanExtra("isSearch", false)) {
            String searchText = intent.getStringExtra("searchText");
            highlightSearchInEditor(searchText);
        }

        if (nameFolder != null) {
            binding.nameFolder.setText(nameFolder);
        }

        updateDateTimeDisplay();

        updateUIForNoteStatus();

        countNoTitle();
        contentEditor();

        if (intent.getBooleanExtra("isExport", false)) {
            new Handler(Looper.getMainLooper()).postDelayed(this::exportNote, 1000);
        }
    }

    private void loadNoteById(int idNote) {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).noteDao().getNoteWithId(idNote)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                note -> {
                                    currentNote = note;
                                    updateUIWithNote(note);
                                },
                                throwable -> Log.e("RxJava", "loadNoteById error", throwable)
                        )
        );
    }


    private void updateUIWithNote(Note note) {
        if (note == null) return;
        binding.titleNote.setText(note.getTitle());
        binding.descriptionNote.setText(note.getDescription());
        binding.editor.setHtml(note.getContent());
        idFolder = note.getFolderId();
        nameFolder = note.getFolderName();
        binding.nameFolder.setText(nameFolder);
    }

    private void updateNotiStatus(Noti noti) {
        if (noti == null) return;

        NoteDatabase db = NoteDatabase.getDatabase(NoteActivity.this);
        int idNote = noti.getIdNote();
        int idNoti = noti.getId();

        Completable completable = Completable.fromAction(() -> {
            db.noteDao().updateCheckNoti(idNote, false).blockingAwait();
            db.notiDao().deleteNoti(idNoti).blockingAwait();
        });

        compositeDisposable.add(
                completable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d("updateNotiStatus", "Cập nhật trạng thái thông báo thành công"),
                                throwable -> Log.e("updateNotiStatus", "Lỗi khi cập nhật trạng thái thông báo", throwable)
                        )
        );
    }

    private void highlightSearchInEditor(String searchText) {
        if (currentNote == null || searchText == null) return;
        String content = currentNote.getContent();
        String highlightedContent = content.replaceAll(
                "(?i)(" + Pattern.quote(searchText) + ")",
                "<mark>$1</mark>"
        );
        binding.editor.setHtml(highlightedContent);
    }

    private void updateDateTimeDisplay() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"));
        binding.dateNote.setText(formatter.format(System.currentTimeMillis()));
    }

    private void updateUIForNoteStatus() {
        if (currentNote == null) return;
        if (currentNote.getId() == 0) {
            binding.menuNote.setVisibility(View.GONE);
            binding.btnShare.setVisibility(View.GONE);
            binding.btnNoti.setVisibility(View.GONE);
        } else {
            binding.menuNote.setVisibility(View.VISIBLE);
            binding.btnShare.setVisibility(View.VISIBLE);
            binding.btnNoti.setVisibility(View.VISIBLE);
            if (currentNote.isNoti()) {
                binding.btnNoti.setImageResource(R.drawable.ic_delete_noti);
            }
        }
    }

    private void notiNote() {
        if (currentNote.isNoti()) {
            showDeleteNotiDialog();
        } else {
            openAddNotiActivity();
        }
    }

    private void showDeleteNotiDialog() {
        if (dialogDeleteNoti == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
            View view = LayoutInflater.from(NoteActivity.this).inflate(R.layout.layout_deleteallnote, null);
            builder.setView(view);
            dialogDeleteNoti = builder.create();

            view.findViewById(R.id.txtConfirm).setOnClickListener(v -> deleteAllNoti());

            view.findViewById(R.id.txtCancel).setOnClickListener(v -> dialogDeleteNoti.dismiss());
        }
        dialogDeleteNoti.show();
    }

    private void deleteAllNoti() {
        int noteId = currentNote.getId();
        NoteDatabase db = NoteDatabase.getDatabase(NoteActivity.this);

        Completable completable = Completable.fromAction(() -> {
            db.notiDao().deleteNotiWithIdNote(noteId).blockingAwait();
            db.noteDao().updateCheckNoti(noteId, false).blockingAwait();
        });

        compositeDisposable.add(
                completable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    currentNote.setNoti(false);
                                    dialogDeleteNoti.dismiss();
                                    binding.btnNoti.setImageResource(R.drawable.ic_noti);
                                },
                                throwable -> Log.e("DelAllNoti", "Lỗi khi xóa tất cả thông báo", throwable)
                        )
        );
    }

    private void openAddNotiActivity() {
        Intent intent = new Intent(NoteActivity.this, AddNotiActivity.class);
        intent.putExtra("idNote", currentNote.getId());
        intent.putExtra("titleNote", currentNote.getTitle());
        intent.putExtra("descriptionNote", currentNote.getDescription());
        intent.putExtra("pathImageNote", currentNote.getPathImage());
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTI);
    }

    private void exportNote() {
        try {
            Bitmap bitmap = captureNoteScreenshot();
            String pdfPath = generatePdfFilePath();
            createPdfFromBitmap(bitmap, pdfPath);
            sharePdfFile(pdfPath);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Xuất PDF thất bại", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap captureNoteScreenshot() {
        ScrollView scrollView = binding.scrollView;
        return ScreenshotUtil.captureFullScrollViewScreenshot(scrollView);
    }

    private String generatePdfFilePath() {
        String sanitizedTitle = currentNote.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
        pdfFileName = sanitizedTitle + ".pdf";
        return new File(getExternalFilesDir("app_files"), pdfFileName).getAbsolutePath();
    }

    private void createPdfFromBitmap(Bitmap bitmap, String pdfPath) throws Exception {
        ScreenshotUtil.createPdfWithScreenshot(bitmap, pdfPath);
    }

    private void sharePdfFile(String pdfPath) {
        File file = new File(pdfPath);
        if (file.exists()) {
            Uri uri = FileProvider.getUriForFile(this, "com.xmobile.project0.fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(shareIntent, getString(R.string.share_by)), 100);
        } else {
            Toast.makeText(this, "File PDF không tồn tại", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("RestrictedApi")
    private void optionMenu(View anchorView) {
        applyMenuButtonStyleBasedOnTheme();

        menuBuilder2 = new MenuBuilder(this);
        new MenuInflater(this).inflate(R.menu.menu_option_modify_in_note, menuBuilder2);

        MenuPopupHelper menuPopup = new MenuPopupHelper(this, menuBuilder2, anchorView);
        menuPopup.setForceShowIcon(true);
        menuPopup.setGravity(Gravity.END);

        menuBuilder2.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                return handleMenuItemSelected(item, anchorView);
            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {
                // No action needed
            }
        });

        menuPopup.setOnDismissListener(() -> resetMenuButtonStyle());
        menuPopup.show();
    }

    private void applyMenuButtonStyleBasedOnTheme() {
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == Configuration.UI_MODE_NIGHT_YES) {
            binding.menuNote.setBackgroundResource(R.drawable.bg_pale_darkblue);
        } else {
            binding.menuNote.setBackgroundResource(R.drawable.bg_pale_blue);
        }
        binding.menuNote.setColorFilter(ContextCompat.getColor(this, R.color.pale_lightGray), PorterDuff.Mode.SRC_IN);
    }

    private boolean handleMenuItemSelected(MenuItem item, View anchorView) {
        //Đổi sang dạng khác
        if (item.getItemId() == R.id.menu_search_in_note){
            searchInNote(anchorView);
            return true;
            //Chọn note
        } else if (item.getItemId() == R.id.menu_move_in_note) {
            moveNote();
            return true;
        } else if (item.getItemId() == R.id.menu_delete_in_note) {
            isSave = true;
            deleteNote();
            return true;
        }
        return false;
    }

    private void resetMenuButtonStyle() {
        binding.menuNote.setBackgroundResource(R.drawable.bg_round_btn);
        binding.menuNote.setColorFilter(ContextCompat.getColor(this, R.color.lightGray), PorterDuff.Mode.SRC_IN);
    }

    private void searchInNote(View anchorView) {
        showSearchUI(anchorView);
        setupSearchInputListener();
        setupClearButton();
        setupDoneButton();
    }

    private void showSearchUI(View anchorView) {
        binding.searchLayout.setVisibility(View.VISIBLE);
        binding.horizontalScrollView.setVisibility(View.GONE);
        binding.editor.setInputEnabled(false);
        binding.searchInput.requestFocus();

        oldContent = binding.editor.getHtml();

        InputMethodManager imm = (InputMethodManager) anchorView.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(binding.editor, InputMethodManager.SHOW_IMPLICIT);
    }

    private void setupSearchInputListener() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String keyword = s.toString();
                if (keyword.isEmpty()) {
                    binding.clearBtn.setVisibility(View.GONE);
                    binding.editor.setHtml(oldContent);
                } else {
                    binding.clearBtn.setVisibility(View.VISIBLE);
                    highlightKeyword(keyword);
                }
            }
        });
    }

    private void highlightKeyword(String keyword) {
        // Escape keyword để tránh lỗi regex
        String escapedKeyword = Pattern.quote(keyword);
        String highlightedContent = oldContent.replaceAll(
                "(?i)(" + escapedKeyword + ")",
                "<mark>$1</mark>"
        );
        binding.editor.setHtml(highlightedContent);
    }

    private void setupClearButton() {
        binding.clearBtn.setOnClickListener(v -> binding.searchInput.setText(""));
    }

    private void setupDoneButton() {
        binding.txtDone.setOnClickListener(v -> {
            binding.editor.setHtml(oldContent);
            binding.searchLayout.setVisibility(View.GONE);

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(binding.editor.getWindowToken(), 0);

            binding.editor.setInputEnabled(true);
        });
    }

    private void moveNote() {
        hideKeyboard();
        setupBottomSheet();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editor.getWindowToken(), 0);
    }

    private void setupBottomSheet() {
        ConstraintLayout bottomSheet = findViewById(R.id.bottomSheetMove);
        View dimView = findViewById(R.id.dimView);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        dimView.setVisibility(View.VISIBLE);
        dimView.setOnTouchListener((v, event) -> true);  // Ngăn touch lan truyền

        TextView txtCancel = bottomSheet.findViewById(R.id.txtCancel);
        TextView titleMove = bottomSheet.findViewById(R.id.titleMove);
        TextView countMove = bottomSheet.findViewById(R.id.countMove);
        recyclerViewFolderMove = bottomSheet.findViewById(R.id.recyclerViewFolderMove);

        txtCancel.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
        titleMove.setText(currentNote.getTitle());
        countMove.setText(getString(R.string.one_note));

        listFolderMove();

        setupRecyclerViewClick(bottomSheetBehavior);

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    dimView.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    dimView.setVisibility(View.GONE);
                    binding.editor.clearFocus();
                    hideKeyboard();
                    bottomSheetBehavior.removeBottomSheetCallback(this);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dimView.setAlpha(slideOffset);
            }
        });
    }

    private void setupRecyclerViewClick(BottomSheetBehavior<View> bottomSheetBehavior) {
        recyclerViewFolderMove.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());
                    if (childView != null) {
                        int position = rv.getChildAdapterPosition(childView);
                        updateNoteFolder(position, bottomSheetBehavior);
                        return true;
                    }
                }
                return false;
            }

            @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) { }
            @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) { }
        });
    }

    private void updateNoteFolder(int position, BottomSheetBehavior<View> bottomSheetBehavior) {
        String selectedFolderName = folders.get(position).getName();
        if (selectedFolderName.equals(getString(R.string.all_notes))) return;

        int noteId = currentNote.getId();
        int folderId = folders.get(position).getId();

        Completable completable = NoteDatabase.getDatabase(NoteActivity.this)
                .noteDao()
                .updateFolderId(noteId, folderId);

        compositeDisposable.add(
                completable
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED),
                                throwable -> Log.e("UpdateFolderId", "Lỗi khi cập nhật FolderId", throwable)
                        )
        );
    }

    private void listFolderMove() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).folderDao().getAllFolders()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                folders -> {
                                    recyclerViewFolderMove.setLayoutManager(new LinearLayoutManager(this));
                                    folderAdapter = new FolderAdapter(folders, this, this);
                                    folderAdapter.setState(2);
                                    recyclerViewFolderMove.setAdapter(folderAdapter);
                                },
                                throwable -> Log.e("RxJava", "listFolderMove error", throwable)
                        )
        );
    }


    private void deleteNote() {
        NoteDatabase db = NoteDatabase.getDatabase(NoteActivity.this);
        int noteId = currentNote.getId();

        Completable deleteNote = db.noteDao().deleteNote(noteId);
        Completable deleteNoti = db.notiDao().deleteNotiWithIdNote(noteId);
        Completable deleteTags = db.tagDao().deleteAllTagWithId(noteId);
        Completable deleteLinks = db.noteLinkDao().deleteAllNoteLinksWithNoteId(noteId);

        compositeDisposable.add(
                Completable.mergeArray(deleteNote, deleteNoti, deleteTags, deleteLinks)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    if (getIntent().getBooleanExtra("fromCalendarFragment", false)) {
                                        Intent intent = new Intent();
                                        intent.putExtra("isDelete", true);
                                        setResult(RESULT_OK, intent);
                                        finish();
                                    } else {
                                        Intent intent = new Intent(NoteActivity.this, ListNoteActivity.class);
                                        intent.putExtra("folderId", idFolder);
                                        intent.putExtra("folderName", nameFolder);
                                        setResult(RESULT_OK, intent);
                                        startActivity(intent);
                                        finish();
                                    }
                                },
                                throwable -> Log.e("DeleteNoteRx", "Lỗi khi xóa ghi chú", throwable)
                        )
        );
    }

    public List<String> extractTagAfterHash(String editorContent) {
        List<String> tags = new ArrayList<>();

        if (editorContent == null || editorContent.isEmpty()) return tags;

        // Sử dụng regex để tìm tag bắt đầu với dấu #
        Pattern pattern = Pattern.compile("#([\\w-]+)");
        Matcher matcher = pattern.matcher(editorContent);

        while (matcher.find()) {
            tags.add(matcher.group(1));  // Group 1 là phần sau dấu #
        }

        return tags;
    }

    public void extractAndSaveTag(){
        String editorContent = binding.editor.getHtml();
        if (editorContent == null) return;

        List<String> tags = extractTagAfterHash(editorContent);
        if (!tags.isEmpty()) {
            filterAndSaveNewTags(tags);
        }
    }

    private void filterAndSaveNewTags(List<String> tags) {

        for (String tag : tags) {
            if (!existTags_name.contains(tag)) {
                newTags.add(new NewTag(tag));
                Log.d("check tag", tag);
            }
        }

        if (currentNote.getId() != 0){
            TagDao tagDao = NoteDatabase.getDatabase(NoteActivity.this).tagDao();

            List<Completable> tasks = new ArrayList<>();
            for (NewTag newTag : newTags) {
                Tag tag = new Tag(currentNote.getId(), newTag.getName());
                tasks.add(tagDao.insertTag(tag));
            }

            compositeDisposable.add(
                    Completable.merge(tasks)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> Log.d("check tag", "update"),
                                    throwable -> Log.e("UpdateNewTagsRx", "Lỗi khi cập nhật tag", throwable)
                            )
            );
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ClickableViewAccessibility")
    private void contentEditor() {
        setupColorsBasedOnMode();

        binding.editor.setEditorFontSize(22);
        binding.editor.setEditorFontColor(Color.parseColor(selectedTxTColor));
        binding.editor.setEditorBackgroundColor(Color.parseColor(selectedBgColor));
        binding.editor.setPlaceholder(getString(R.string.content));
        binding.editor.setInputEnabled(false);

        binding.editor.setOnFocusChangeListener(this::handleEditorFocusChange);

        toolEdit();
    }

    private void setupColorsBasedOnMode() {
        int currentMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentMode == Configuration.UI_MODE_NIGHT_NO) {
            selectedBgColor = "#F2F2F7";
            selectedTxTColor = "#000000";
            bgColorSystem = selectedBgColor;
            txtColorSystem = selectedTxTColor;
        }
    }

    private void handleEditorFocusChange(View v, boolean hasFocus) {
        InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (hasFocus) {
            binding.horizontalScrollView.setVisibility(View.VISIBLE);
            binding.editor.setInputEnabled(true);
            imm.showSoftInput(binding.editor, InputMethodManager.SHOW_IMPLICIT);
        } else {
            binding.horizontalScrollView.setVisibility(View.GONE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void countNoTitle() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).noteDao().countNoTitle(idFolder)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> countNoTitle = result,
                                throwable -> Log.e("RxJava", "countNoTitle error", throwable)
                        )
        );
    }


    private void saveNoteToCache() {
        String currentTitle = binding.titleNote.getText().toString().trim();
        String currentDescription = binding.descriptionNote.getText().toString().trim();
        String currentContent = binding.editor.getHtml();

        if (currentContent != null) {
            currentContent = currentContent.replaceAll("<mark>(.*?)</mark>", "$1");
        } else {
            currentContent = "";
        }

        if (currentTitle.isEmpty()) {
            currentTitle = countNoTitle == 0 ? "Không có tiêu đề" : "Không có tiêu đề " + (countNoTitle + 1);
        }

        currentNote.setTitle(currentTitle);
        currentNote.setDescription(currentDescription.isEmpty() ? null : currentDescription);
        currentNote.setContent(currentContent.isEmpty() ? null : currentContent);

        String date = new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN"))
                .format(System.currentTimeMillis());
        currentNote.setDateTime(date);
        currentNote.setFolderId(idFolder);
        currentNote.setFolderName(nameFolder);

        cache.put("currentNote", currentNote);
    }

    private void saveCacheToDatabase() {
        if (!cache.containsKey("currentNote")) return;

        Note note = cache.get("currentNote");

        boolean isEmptyNote = (note.getTitle() == null || note.getTitle().isEmpty())
                && (note.getDescription() == null || note.getDescription().isEmpty())
                && (note.getContent() == null || note.getContent().isEmpty());

        if (isEmptyNote) return;

        // Cập nhật dữ liệu mới nhất từ currentNote
        note.setTitle(currentNote.getTitle());
        note.setDescription(currentNote.getDescription());
        note.setContent(currentNote.getContent());
        note.setDateTime(currentNote.getDateTime());
        note.setFolderId(currentNote.getFolderId());
        note.setFolderName(currentNote.getFolderName());

        NoteDao noteDao = NoteDatabase.getDatabase(NoteActivity.this).noteDao();

        compositeDisposable.add(
                noteDao.insertNote(note)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d("SaveNote", "Lưu ghi chú thành công"),
                                throwable -> Log.e("SaveNote", "Lỗi khi lưu ghi chú", throwable)
                        )
        );
    }

    private void saveCachetoSharedReference() {
        Note note = cache.get("currentNote");
        if (note == null) return;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("title", note.getTitle());
        editor.putString("description", note.getDescription());
        editor.putString("content", note.getContent());
        editor.putString("dateTime", note.getDateTime());
        editor.putInt("folderId", note.getFolderId());
        editor.putString("folderName", note.getFolderName());
        editor.apply();

    }

    private void loadCacheFromSharedReference() {
        sharedPreferences = getSharedPreferences("cache", MODE_PRIVATE);

        currentNote.setTitle(sharedPreferences.getString("title", ""));
        currentNote.setDescription(sharedPreferences.getString("description", ""));
        currentNote.setContent(sharedPreferences.getString("content", ""));
        currentNote.setDateTime(sharedPreferences.getString("dateTime", ""));
        currentNote.setFolderId(sharedPreferences.getInt("folderId", 0));
        currentNote.setFolderName(sharedPreferences.getString("folderName", ""));
    }

    private void setupButtons() {
        setupToggleAction(binding.actionBold, () -> binding.editor.setBold(), 1);
        setupToggleAction(binding.actionItalic, () -> binding.editor.setItalic(), 1);
        setupToggleAction(binding.actionUnderline, () -> binding.editor.setUnderline(), 1);
        setupToggleAction(binding.actionStrikethrough, () -> binding.editor.setStrikeThrough(), 1);
        setupToggleAction(binding.actionHeading1, () -> binding.editor.setHeading(1), 2);
        setupToggleAction(binding.actionHeading2, () -> binding.editor.setHeading(2), 2);
        setupToggleAction(binding.actionHeading3, () -> binding.editor.setHeading(3), 2);
        setupToggleAction(binding.actionHeading4, () -> binding.editor.setHeading(4), 2);
        setupToggleAction(binding.actionHeading5, () -> binding.editor.setHeading(5), 2);
        setupToggleAction(binding.actionHeading6, () -> binding.editor.setHeading(6), 2);
        setupToggleAction(binding.actionIndent, () -> binding.editor.setIndent(), 2);
        setupToggleAction(binding.actionOutdent, () -> binding.editor.setOutdent(), 2);
        setupToggleAction(binding.actionAlignLeft, () -> binding.editor.setAlignLeft(), 2);
        setupToggleAction(binding.actionAlignCenter, () -> binding.editor.setAlignCenter(), 2);
        setupToggleAction(binding.actionAlignRight, () -> binding.editor.setAlignRight(), 2);
        setupToggleAction(binding.actionInsertBullets, () -> binding.editor.setBullets(), 2);
        setupToggleAction(binding.actionInsertNumbers, () -> binding.editor.setNumbers(), 2);
        setupToggleAction(binding.actionInsertCheckbox, () -> binding.editor.insertTodo(), 2);
        setupToggleAction(binding.actionBlockquote, () -> binding.editor.setBlockquote(), 2);

    }

    private void setupToggleAction(View button, Runnable action, int type) {
        button.setOnClickListener(new View.OnClickListener() {
            private boolean isActive = false;

            @Override
            public void onClick(View v) {
                action.run();
                if (type == 1){
                    toggleButtonBackground(v, isActive);
                    isActive = !isActive;
                }
            }
        });
    }

    private void toggleButtonBackground(View button, boolean isActive) {
        button.setBackgroundResource(isActive ? 0 : R.drawable.bg_search2);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint({"RestrictedApi", "SetJavaScriptEnabled"})
    private void toolEdit() {
        setupEditorJavascriptInterfaces();
        setupBasicActions();
        setupColorPickers();
        setupTagInsertion();
        setupImageInsertion();
        setupLinkInsertion();
    }

    // JavaScript interfaces để xử lý link và file
    private void setupEditorJavascriptInterfaces() {
        binding.editor.getSettings().setJavaScriptEnabled(true);
        WebSettings webSettings = binding.editor.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setSupportZoom(false); // tắt zoom nếu không cần thiết
//        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        binding.editor.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onLinkClicked(String href) {
                if (href.startsWith("note://")) {
                    String noteId = href.substring("note://".length());
                    openNoteById(Integer.parseInt(noteId), noteTitle);
                } else {
                    openWebLink(href);
                }
            }
        }, "AndroidBridge");

        binding.editor.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void onFileClicked(String filePath) {
                Log.d("File Clicked", "Path: " + filePath);
                openFile(filePath);
            }
        }, "AndroidBridge2");
    }

    // Undo/Redo + setup nút định dạng
    private void setupBasicActions() {
        binding.actionUndo.setOnClickListener(v -> binding.editor.undo());
        binding.actionRedo.setOnClickListener(v -> binding.editor.redo());
        setupButtons();
    }

    // Hiển thị và áp dụng màu chữ/nền
    private void setupColorPickers() {
        setupToggleColorPicker(binding.actionTxtColor, true);
        setupToggleColorPicker(binding.actionBgColor, false);

        setupColorAction(binding.actionColorRed, "#FF0000");
        setupColorAction(binding.actionColorOrange, "#FF5722");
        setupColorAction(binding.actionColorYellow, "#FFEB3B");
        setupColorAction(binding.actionColorGreen, "#00FF00");
        setupColorAction(binding.actionColorBlue, "#007AFF");
        setupColorAction(binding.actionColorIndigo, "#4B0082");
        setupColorAction(binding.actionColorViolet, "#8F00FF");
    }

    private void setupToggleColorPicker(View button, boolean isText) {
        button.setOnClickListener(new View.OnClickListener() {
            boolean isClick = true;
            @Override
            public void onClick(View v) {
                binding.horizontalScrollViewColor.setVisibility(isClick ? View.VISIBLE : View.GONE);
                isClick = !isClick;
                isTxt = isText;
                isBg = !isText;
            }
        });
    }

    private void setupColorAction(View button, String colorHex) {
        button.setOnClickListener(new View.OnClickListener() {
            boolean isChanged = false;
            @Override
            public void onClick(View v) {
                if (isTxt) {
                    selectedTxTColor = colorHex;
                    binding.editor.setTextColor(isChanged ? Color.parseColor(txtColorSystem) : Color.parseColor(selectedTxTColor));
                    isTxt = false;
                } else {
                    selectedBgColor = colorHex;
                    binding.editor.setTextBackgroundColor(isChanged ? Color.parseColor(bgColorSystem) : Color.parseColor(selectedBgColor));
                    isBg = false;
                }
                binding.horizontalScrollViewColor.setVisibility(View.GONE);
                isChanged = !isChanged;
            }
        });
    }

    // Nút chèn tag
    private void setupTagInsertion() {
        binding.actionTag.setOnClickListener(v -> {
            String jsCode = "javascript:(function() {" +
                    "var tagSymbol = '#';" +
                    "document.execCommand('insertText', false, tagSymbol);" +
                    "})()";
            binding.editor.evaluateJavascript(jsCode, null);
            showTags(v);
        });
    }

    // Nút chèn hình ảnh
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void setupImageInsertion() {
        binding.actionInsertImage.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(NoteActivity.this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(NoteActivity.this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_CODE_STORAGE_PERMISSION);
            } else {
                insertImage();
            }
        });
    }

    // Nút chèn link
    @SuppressLint("RestrictedApi")
    private void setupLinkInsertion() {
        menuBuilder = new MenuBuilder(this);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.menu_option_link, menuBuilder);
        binding.actionInsertLink.setOnClickListener(this::insertLink);
    }

    private void showTags(View anchorView) {
        Context context = anchorView.getContext();
        View layout = inflateTagPopupLayout(context);
        RecyclerView recyclerView = setupTagRecyclerView(layout, context);

        PopupWindow popupWindow = createPopupAboveView(layout, anchorView);

        handleTagItemClick(recyclerView, popupWindow);
    }

    private View inflateTagPopupLayout(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.layout_list_tags, null);
    }

    private RecyclerView setupTagRecyclerView(View layout, Context context) {
        RecyclerView recyclerView = layout.findViewById(R.id.recyclerViewTags);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        tagAdapter = new TagAdapter(existTags, NoteActivity.this);
        recyclerView.setAdapter(tagAdapter);
        return recyclerView;
    }

    private PopupWindow createPopupAboveView(View layout, View anchorView) {
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        layout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupHeight = layout.getMeasuredHeight();

        PopupWindow popupWindow = new PopupWindow(
                layout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.showAtLocation(anchorView, Gravity.NO_GRAVITY, x, y - popupHeight);
        return popupWindow;
    }

    private void handleTagItemClick(RecyclerView recyclerView, PopupWindow popupWindow) {
        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View childView = rv.findChildViewUnder(e.getX(), e.getY());
                if (childView != null) {
                    int position = rv.getChildAdapterPosition(childView);
                    insertTagAtCursor(existTags.get(position).getNameTag());
                    popupWindow.dismiss();
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void insertTagAtCursor(String nameTag) {
        String jsCode = "javascript:(function() {" +
                "document.execCommand('insertText', false, '" + nameTag + "');" +
                "})()";
        binding.editor.evaluateJavascript(jsCode, null);
        focusEditorWithKeyboard();
    }

    private void focusEditorWithKeyboard() {
        binding.editor.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.editor, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void insertImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
    }

    @SuppressLint("RestrictedApi")
    private void insertLink(View v) {
        MenuPopupHelper menu_option = new MenuPopupHelper(NoteActivity.this, menuBuilder, v);
        menu_option.setForceShowIcon(true);
        menu_option.setGravity(Gravity.FILL);

        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_web_link){
                    insertWebLink();
                    return true;
                } else if (item.getItemId() == R.id.menu_attach_file) {
                    insertFile();
                    return true;
                } else if (item.getItemId() == R.id.menu_note_link) {
                    insertNoteLink();
                    return true;
                }
                return false;
            }

            @Override
            public void onMenuModeChange(@NonNull MenuBuilder menu) {

            }
        });
        menu_option.show();
    }

    private void insertFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*"); // Cho phép chọn bất kỳ tệp nào
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    private void insertNoteLink() {
        hideKeyboard();
        setupBottomSheetUI();
        loadNoteListToBottomSheet();
        setupRecyclerViewItemClickListener();
        setupBottomSheetBehavior();
    }

    private void setupBottomSheetBehavior() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    dimView.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    dimView.setVisibility(View.GONE);
                    binding.editor.clearFocus();
                    hideKeyboard();
                    bottomSheetBehavior.removeBottomSheetCallback(this);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dimView.setAlpha(slideOffset);
            }
        });
    }

    private void setupRecyclerViewItemClickListener() {
        recyclerViewNoteLink.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_UP) {
                View child = rv.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    int position = rv.getChildAdapterPosition(child);
                    handleNoteItemClick(position);
                    return true;
                }
            }
            return false;
        }

        @Override public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
        @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
    });
    }

    private void handleNoteItemClick(int position) {
        Note note = notes.get(position);
        noteTitle = note.getTitle();
        insertNoteLinkToEditor(note.getTitle(), note.getId());
        Toast.makeText(this, "Đã chọn ghi chú: " + note.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void setupBottomSheetUI() {
        bottomSheet = findViewById(R.id.bottomSheetListNotes);
        dimView = findViewById(R.id.dimView);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        dimView.setVisibility(View.VISIBLE);
        dimView.setOnTouchListener((v, event) -> true);  // Chặn touch

        TextView txtCancel = bottomSheet.findViewById(R.id.txtCancel);
        recyclerViewNoteLink = bottomSheet.findViewById(R.id.recyclerViewNote);

        txtCancel.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
    }

    private void insertNoteLinkToEditor(String noteTitle, int noteId) {
        @SuppressLint("DefaultLocale")
        String linkHtml = String.format(
                "<a href='note://%d' onclick='AndroidBridge.onLinkClicked(this.href); return false;'>%s</a>",
                noteId, noteTitle
        );
        String currentContent = binding.editor.getHtml();
        if (currentContent == null) currentContent = "";
        binding.editor.setHtml(currentContent + linkHtml);

        //đưa con trỏ vào cuối editor
        binding.editor.loadUrl("javascript:var editor = document.getElementById('editor');" +
                "if (editor) { var range = document.createRange();" +
                "range.selectNodeContents(editor);" +
                "range.collapse(false);" +
                "var selection = window.getSelection();" +
                "selection.removeAllRanges();" +
                "selection.addRange(range); }");
    }

    private void openNoteById(int noteId, String noteTitle) {
        Intent intent = new Intent(this, NoteActivity.class);

        if (NoteDatabase.getDatabase(NoteActivity.this).noteDao().getNoteWithId(noteId) == null){
            Note note = new Note();
            Log.d("check note cc", "ko có note");
            note.setTitle(noteTitle);
            note.setId(noteId);
            NoteDatabase.getDatabase(NoteActivity.this).noteDao().insertNote(note);
        }

        intent.putExtra("idNote", noteId);
        intent.putExtra("noteTitleLink", currentNote.getTitle());
        intent.putExtra("isUpdate", true);
        startActivity(intent);
    }

    private List<Integer> extractLinkedNoteIdsFromEditor() {
        String htmlContent = binding.editor.getHtml();
        if (htmlContent == null) return Collections.emptyList();

        List<Integer> noteIds = new ArrayList<>();
        Pattern pattern = Pattern.compile("href=['\"]note://(\\d+)['\"]");
        Matcher matcher = pattern.matcher(htmlContent);

        while (matcher.find()) {
            int noteId = Integer.parseInt(matcher.group(1));
            noteIds.add(noteId);
        }

        return noteIds;
    }

    private void loadNoteListToBottomSheet() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).noteDao().getAllNotesWithPos()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                notes -> {
                                    this.notes = notes;
                                    recyclerViewNoteLink.setLayoutManager(new LinearLayoutManager(this));
                                    adapter = new NoteAdapter(notes);
                                    adapter.setTypeList(1);
                                    adapter.setTypeNote(2);
                                    recyclerViewNoteLink.setAdapter(adapter);
                                },
                                throwable -> Log.e("RxJava", "loadNoteListToBottomSheet error", throwable)
                        )
        );
    }


    private void insertWebLink() {
        if (dialogAddWebLink == null) {
            createInsertWebLinkDialog();
        }
        dialogAddWebLink.show();
    }

    private void createInsertWebLinkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(NoteActivity.this);
        View view = LayoutInflater.from(NoteActivity.this).inflate(R.layout.layout_insert_weblink, null);
        builder.setView(view);
        dialogAddWebLink = builder.create();

        TextInputLayout inputName = view.findViewById(R.id.edtURL);
        TextView checkURL = view.findViewById(R.id.checkURL);
        EditText edtNameAlt = view.findViewById(R.id.edtNameAlt);
        TextView txtDone = view.findViewById(R.id.txtDone);
        TextView txtCancel = view.findViewById(R.id.txtCancel);

        txtDone.setOnClickListener(v -> handleInsertLink(inputName, checkURL, edtNameAlt));
        txtCancel.setOnClickListener(v -> dialogAddWebLink.dismiss());

        dialogAddWebLink.setOnDismissListener(dialog -> clearInsertWebLinkDialog(inputName, checkURL));
    }

    private void handleInsertLink(TextInputLayout inputName, TextView checkURL, EditText edtNameAlt) {
        String url = Objects.requireNonNull(inputName.getEditText()).getText().toString().trim();

        if (url.isEmpty()) {
            checkURL.setVisibility(View.VISIBLE);
            checkURL.setText("Vui lòng nhập URL");
            return;
        }

        if (!Patterns.WEB_URL.matcher(url).matches()) {
            checkURL.setVisibility(View.VISIBLE);
            checkURL.setText("URL không hợp lệ");
            return;
        }

        String nameAlt = edtNameAlt.getText().toString().trim();
        insertLinkToEditor(url, nameAlt.isEmpty() ? url : nameAlt);
        dialogAddWebLink.dismiss();
    }

    private void clearInsertWebLinkDialog(TextInputLayout inputName, TextView checkURL) {
        checkURL.setVisibility(View.INVISIBLE);
        Objects.requireNonNull(inputName.getEditText()).setText(null);
        inputName.clearFocus();
    }

    //thêm link web vào editor
    private void insertLinkToEditor(String URL, String nameAlt){
        String linkHtml = String.format(
                "<a href='%s' onclick='AndroidBridge.onLinkClicked(this.href); return false;'>%s</a>",
                URL, nameAlt
        );
        String currentContent = binding.editor.getHtml();
        if (currentContent == null) currentContent = "";
        binding.editor.setHtml(currentContent + linkHtml);

        binding.editor.loadUrl("javascript:var editor = document.getElementById('editor');" +
                "if (editor) { var range = document.createRange();" +
                "range.selectNodeContents(editor);" +
                "range.collapse(false);" +
                "var selection = window.getSelection();" +
                "selection.removeAllRanges();" +
                "selection.addRange(range); }");
    }

    //thêm link file vào editor
    private void insertFileLinkInEditor(Uri fileUri, String fileName) {
        String fileLink = fileUri.toString(); // Chuyển URI thành chuỗi để làm đường dẫn
        String linkHtml = String.format(
                "<a href='%s' onclick='AndroidBridge2.onFileClicked(this.href); return false;'>%s</a>",
                fileLink, fileName
        );
        String currentContent = binding.editor.getHtml();
        if (currentContent == null) currentContent = "";
        binding.editor.setHtml(currentContent + linkHtml);

        binding.editor.loadUrl("javascript:var editor = document.getElementById('editor');" +
                "if (editor) { var range = document.createRange();" +
                "range.selectNodeContents(editor);" +
                "range.collapse(false);" +
                "var selection = window.getSelection();" +
                "selection.removeAllRanges();" +
                "selection.addRange(range); }");
    }

    //lấy đường dẫn ảnh
    private String getPathImageFromUri(Context context, Uri uri) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            if (idx != -1) {
                path = cursor.getString(idx);
            }
            cursor.close();
        }
        return path;
    }

    //lấy tên file
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    fileName = cursor.getString(nameIndex);
                }
            }
        } else if (uri.getScheme().equals("file")) {
            fileName = new File(uri.getPath()).getName();
        }
        return fileName;
    }

    //mở link web
    private void openWebLink(String url) {
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Invalid URL", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    //mở file
    private void openFile(String filePath) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(filePath), "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show();
        }
    }

    //Kiểm tra quyền của thiết bị
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                insertImage();
            } else {
                Log.d("NoteActivity", "Permission denied");
            }
        }
    }

    //xử lý kết quả trả về
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) return;

        switch (requestCode) {
            case REQUEST_CODE_SELECT_IMAGE:
                handleImageSelection(data);
                break;

            case REQUEST_CODE_PICK_FILE:
                handleFileSelection(data);
                break;

            case REQUEST_CODE_ADD_NOTI:
                handleAddNoti();
                break;

            case 100:
                handleExportResult(resultCode);
                break;
        }
    }

    private void handleImageSelection(Intent data) {
        Uri selectedImageUri = data.getData();
        if (selectedImageUri == null) return;

        try {
            String imagePath = getPathImageFromUri(this, selectedImageUri);
            currentNote.setPathImage(imagePath);

            String resizedPath = resizeAndSaveImage(imagePath, 300, 300);
            if (resizedPath != null) imagePath = resizedPath;

            Uri imageUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    new File(imagePath)
            );

            insertImageInEditor(imageUri);

        } catch (Exception e) {
            Log.d("NoteActivity", "Error: " + e.getMessage());
        }
    }

    private void insertImageInEditor(Uri imageUri) {
        String htmlImageTag = "<img src=\"" + imageUri + "\" alt=\"Image Note\">";
        String currentContent = binding.editor.getHtml();
        if (currentContent == null) currentContent = "";
        binding.editor.setHtml(currentContent + htmlImageTag);

        // Đảm bảo trỏ con trỏ về cuối editor
        binding.editor.loadUrl(
                "javascript:var editor = document.getElementById('editor');" +
                        "if (editor) {" +
                        "var range = document.createRange();" +
                        "range.selectNodeContents(editor);" +
                        "range.collapse(false);" +
                        "var selection = window.getSelection();" +
                        "selection.removeAllRanges();" +
                        "selection.addRange(range);" +
                        "}"
        );
    }

    private void handleFileSelection(Intent data) {
        Uri fileUri = data.getData();
        if (fileUri == null) return;

        String fileName = getFileName(fileUri);
        insertFileLinkInEditor(fileUri, fileName);
    }

    private void handleAddNoti() {
        currentNote.setNoti(true);
        binding.btnNoti.setImageResource(R.drawable.ic_delete_noti);
    }

    private void handleExportResult(int resultCode) {
        if (resultCode != RESULT_OK) {
            deletePdf();
        }

        if (getIntent().getBooleanExtra("isExport", false)) {
            finish();
        }
    }

    private void deletePdf() {
        String path = getExternalFilesDir("app_files") + pdfFileName;
        File file = new File(path);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d("NoteActivity", "PDF file deleted successfully");
            } else {
                Log.d("NoteActivity", "Failed to delete PDF file");
            }
        }
    }

    public String resizeAndSaveImage(String imagePath, int maxWidth, int maxHeight) {
        // Đọc ảnh từ đường dẫn
        Bitmap originalBitmap = BitmapFactory.decodeFile(imagePath);

        // Tính toán tỷ lệ thu nhỏ
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) (maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) (maxWidth / ratioBitmap);
        }

        // Tạo ảnh thu nhỏ
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);

        // Lưu ảnh thu nhỏ vào file mới
        File resizedImageFile = new File(getCacheDir(), "resized_image.jpg");
        try (FileOutputStream out = new FileOutputStream(resizedImageFile)) {
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            return resizedImageFile.getAbsolutePath(); // trả về đường dẫn ảnh đã thu nhỏ
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            loadCacheFromSharedReference();
        }, 100);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveCachetoSharedReference();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isSave == false){
            new Thread(() -> {
                saveNoteToCache();
                saveCacheToDatabase();
            }).start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.editor.destroy();
        NoteDatabase.getDatabase(this).close();
        NoteDatabase.destroyInstance();
        if (dialogAddWebLink != null) dialogAddWebLink.dismiss();
        if (dialogDeleteNoti != null) dialogDeleteNoti.dismiss();
        if (adapter != null) adapter.clear();
        adapter = null;
        if (folderAdapter != null) folderAdapter.clear();
        folderAdapter = null;
        if (tagAdapter != null) tagAdapter.clear();
        tagAdapter = null;
        bottomSheet = null;
        dimView = null;
        bottomSheetBehavior = null;
        recyclerViewFolderMove = null;
        recyclerViewNoteLink = null;
        binding = null;
        compositeDisposable.clear();
    }

    @Override
    public void onFinishRequested() {
        finish();
    }
}

//Soạn thảo văn bản: Rich Text Editor, SpannableString (soạn thảo thông thường), Markwon (markdown) -> Option soạn thảo trong phần setting, mặc định là soạn thảo
/*tự động lưu + nút back/ahead:
    thời điểm lưu: sau một hành động cụ thể (sử dụng debouncing)
    sử dụng addTextChangedListener -> mỗi hành động thay đổi văn bản sẽ được lưu vào cache -> chuyển trạng thái (ấn quay lại thư mục, thoát app) thì lưu cache vào database (onPause/onStop)
        onPause (mở dialog, ...) -> lưu cache vào shared preference -> onResume -> load cache từ shared preference
        onStop (thoát app, chuyển activity) -> lưu cache vào database
    cache rỗng -> khi thoát app, chuyển activity không lưu cache vào database
 */

//lưu image -> tùy theo cơ chế lưu note (hiện tại đang là ấn rồi lưu)
//attach file -> link đến path file/dùng thư viện hiển thị file ở giữa description và content

//phần đặt color và background cho text -> ấn vào nút hiển thị các màu để chọn
//thêm bg khi chọn style cho text, đổi màu của icon cho phần đặt màu và background
//chia luồng để tăng hiệu suất
//phần ảnh: thêm ảnh từ thiết bị
//phần link: thêm link web hoặc link đến một note khác
//lưu trữ và database: dạng html, một số trường hợp đặc biệt xử lí khác

//thêm phần tag -> ấn vào nút tag, ấn # -> hiển thị danh sách tag hiện có để chọn hoặc tự nhập -> kiểm tra xem tag có tồn tại chưa, chưa thì tạo tag mới
//phần đặt color và background cho text -> ấn vào nút hiển thị các màu để chọn
//phần option menu: tìm kiếm (trong note), di chuyển, xóa, export (pdf)
//tìm kiếm: tô bg cho text được tìm thấy

//khi mở note: kiểm tra các liên kết hiện có
//khi thoát note (save note): tổng hợp các liên kết -> so sánh với list lúc mở note -> cập nhật sự thay đổi vào db