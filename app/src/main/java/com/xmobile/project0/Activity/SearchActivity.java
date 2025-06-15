package com.xmobile.project0.Activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.Adapter.TagAdapter;
import com.xmobile.project0.DAO.TagDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.Entities.Tag;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.Helper.MySwipeHelper;
import com.xmobile.project0.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SearchActivity extends BaseActivity implements ItemClickListener {
    private ActivitySearchBinding binding;
    private final List<Tag> tags = new ArrayList<>();
    private final List<Note> notesSearch = new ArrayList<>();
    private List<Note> notes;
    private TagAdapter tagAdapter;
    private NoteAdapter noteAdapter;
    private MySwipeHelper mySwipeHelper;
    private String searchText = "";
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static final int REQUEST_CODE_UPDATE_NOTE = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        setupListeners();
        loadNotes();
        loadTags();
    }

    private void setupUI() {
        binding.searchInput.requestFocus();

        noteAdapter = new NoteAdapter(notesSearch, this);
        binding.recyclerViewNoteSearch.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewNoteSearch.setAdapter(noteAdapter);
    }

    private void setupListeners() {
        binding.txtCancel.setOnClickListener(v -> finish());

        binding.layoutTag.setOnClickListener(new View.OnClickListener() {
            boolean isExpanded = true;

            @Override
            public void onClick(View v) {
                binding.imgExpand.setImageResource(isExpanded ? R.drawable.ic_collapse : R.drawable.ic_expand);
                binding.recyclerViewTag.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                isExpanded = !isExpanded;
            }
        });

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();
                boolean isEmpty = input.isEmpty();

                binding.layoutTag.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                binding.recyclerViewTag.setVisibility(View.GONE);
                binding.recyclerViewNoteSearch.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.clearBtn.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                binding.imgExpand.setImageResource(R.drawable.ic_expand);

                if (!isEmpty) searchNote(input);
            }
        });

        binding.clearBtn.setOnClickListener(v -> binding.searchInput.setText(""));
    }

    private void loadTags() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).tagDao().getAllTags()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                loadedTags -> {
                                    tags.clear();
                                    tags.addAll(loadedTags);
                                    binding.countTag.setText("(" + tags.size() + ")");
                                    setupTagRecyclerView();
                                },
                                throwable -> Log.e("RxJava", "loadTags error", throwable)
                        )
        );
    }


    private void setupTagRecyclerView() {
        binding.recyclerViewTag.setLayoutManager(new LinearLayoutManager(this));
        attachSwipeToTagList();
        tagAdapter = new TagAdapter(tags, this, this);
        binding.recyclerViewTag.setAdapter(tagAdapter);
    }

    private void searchNote(String input) {
        notesSearch.clear();
        if (notes == null) return;

        String inputLower = input.toLowerCase();
        for (Note note : notes) {
            String title = note.getTitle() != null ? note.getTitle().toLowerCase() : "";
            String description = note.getDescription() != null ? note.getDescription().toLowerCase() : "";
            String content = note.getContent() != null ? note.getContent().toLowerCase() : "";

            if (title.contains(inputLower) || description.contains(inputLower) || content.contains(inputLower)) {
                notesSearch.add(note);
                if (content.contains(inputLower)) searchText = input;
            }
        }
        noteAdapter.notifyDataSetChanged();
    }

//    private void highlightKeyword(String keyword) {
//        String content = binding.editor.getHtml(); // Lấy nội dung HTML từ editor
//
//        // Kiểm tra nếu từ khóa tồn tại
//        if (content.contains(keyword)) {
//            // Làm nổi bật từ khóa bằng cách chèn thẻ <mark>
//            String highlightedContent = content.replaceAll(
//                    "(?i)(" + keyword + ")",  // Tìm từ khóa (không phân biệt hoa thường)
//                    "<mark>$1</mark>");      // Chèn thẻ <mark> xung quanh từ khóa
//
//            // Set lại nội dung cho editor
//            binding.editor.setHtml(highlightedContent);
//            Toast.makeText(this, "Keyword highlighted!", Toast.LENGTH_SHORT).show();
//        } else {
//            Toast.makeText(this, "Keyword not found!", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void loadNotes() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(this).noteDao().getAllNotesWithPos()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                loadedNotes -> notes = loadedNotes,
                                throwable -> Log.e("RxJava", "loadNotes error", throwable)
                        )
        );
    }


    private void attachSwipeToTagList() {
        if (mySwipeHelper != null) return;

        mySwipeHelper = new MySwipeHelper(this, binding.recyclerViewTag, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer) {
                buffer.add(new MyButton(SearchActivity.this,
                        "Xóa",
                        30,
                        R.drawable.ic_delete,
                        Color.RED,
                        pos -> deleteTagAt(pos)));
            }
        };
    }

    private void deleteTagAt(int pos) {
        Tag tag = tags.get(pos);
        TagDao tagDao = NoteDatabase.getDatabase(SearchActivity.this).tagDao();

        compositeDisposable.add(
                tagDao.deleteTag(tag.getIdNote(), tag.getNameTag())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    tags.remove(pos);
                                    tagAdapter.notifyItemRemoved(pos);
                                    binding.countTag.setText("(" + tags.size() + ")");
                                },
                                throwable -> Log.e("DeleteTag", "Lỗi khi xóa tag", throwable)
                        )
        );
    }

    @Override
    public void onNoteClicked(Note note) {
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isUpdate", true);
        intent.putExtra("isSearch", true);
        intent.putExtra("searchText", searchText);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        finish();
    }

    @Override
    public void onTagClicked(String tag, int position) {
        binding.searchInput.setText("#" + tag);
    }

    @Override
    public void onNotiClicked(Noti noti, int type) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mySwipeHelper = null;
        noteAdapter.clear();
        tagAdapter.clear();
        noteAdapter = null;
        tagAdapter = null;
        notes = null;
        binding = null;
        compositeDisposable.clear();
    }
}

//tìm kiếm trên toàn bộ các note, hiển thị danh sách tag để chọn -> chọn thì gán #nametag vào phần tìm kiếm
//phần tìm kiếm rỗng -> hiển thị danh sách tag để chọn
//hiển thị danh sách note chứa chuỗi tìm kiếm, ấn vào note thì nội dung tìm kiếm được tô bg