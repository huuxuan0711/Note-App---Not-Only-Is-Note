package com.xmobile.project0.Fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.xmobile.project0.Activity.AddNotiActivity;
import com.xmobile.project0.Activity.NoteActivity;
import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.Adapter.NotiAdapter;
import com.xmobile.project0.BroadcastReceiver.ReminderReceiver;
import com.xmobile.project0.DAO.NoteDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.Helper.MySwipeHelper;
import com.xmobile.project0.databinding.FragmentNotiBinding;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NotiFragment extends BaseFragment implements ItemClickListener {
    private FragmentNotiBinding binding;
    private MySwipeHelper mySwipeHelper;
    private List<Noti> notis, notisComing, notisUnRead;
    private List<Note> notes;
    private NotiAdapter adapterComing, adapterUnRead;
    private NoteAdapter adapter;
    private RecyclerView recyclerViewNote;
    private ConstraintLayout bottomSheet;
    private View dimView;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_ADD_NOTI = 4;
    public static final int REQUEST_CODE_UPDATE_NOTI = 5;

    public NotiFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotiBinding.inflate(inflater, container, false);
        initControl();
        return binding.getRoot();
    }

    private void initControl() {
        binding.btnNoti.setOnClickListener(v -> addNoti());

        getListNotis();
    }

    private void addNoti() {
        setupBottomSheet();
        setupDimView();
        setupCancelButton();
        setupRecyclerViewTouchListener();
        loadNoteList();
        setupBottomSheetCallback();
    }

    private void setupBottomSheet() {
        bottomSheet = binding.getRoot().findViewById(R.id.bottomSheetListNotes);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(true);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void setupDimView() {
        dimView = binding.dimView;
        dimView.setVisibility(View.VISIBLE);
        dimView.setOnTouchListener((v, event) -> true); // chặn mọi touch
    }

    private void setupCancelButton() {
        TextView txtCancel = bottomSheet.findViewById(R.id.txtCancel);
        txtCancel.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
    }

    private void setupRecyclerViewTouchListener() {
        recyclerViewNote = bottomSheet.findViewById(R.id.recyclerViewNote);
        recyclerViewNote.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    View childView = rv.findChildViewUnder(e.getX(), e.getY());
                    if (childView != null) {
                        int position = rv.getChildAdapterPosition(childView);
                        onNoteItemClicked(position);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
    }

    private void onNoteItemClicked(int position) {
        Note note = notes.get(position);
        Intent intent = new Intent(requireContext(), AddNotiActivity.class);
        intent.putExtra("idNote", note.getId());
        intent.putExtra("titleNote", note.getTitle());
        intent.putExtra("descriptionNote", note.getDescription());
        intent.putExtra("pathImageNote", note.getPathImage());
        startActivityForResult(intent, REQUEST_CODE_ADD_NOTI);
    }

    private void setupBottomSheetCallback() {
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    dimView.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    dimView.setVisibility(View.GONE);
                    bottomSheetBehavior.removeBottomSheetCallback(this);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                dimView.setAlpha(slideOffset);
            }
        });
    }

    private void loadNoteList(){
        NoteDao noteDao = NoteDatabase.getDatabase(requireContext()).noteDao();

        compositeDisposable.add(
                noteDao.getAllNotesWithPos()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    notes = result;
                                    recyclerViewNote.setLayoutManager(
                                            new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
                                    );
                                    adapter = new NoteAdapter(notes);
                                    adapter.setTypeList(1);
                                    adapter.setTypeNote(2);
                                    recyclerViewNote.setAdapter(adapter);
                                },
                                throwable -> {
                                    Toast.makeText(requireContext(), "Lỗi khi tải ghi chú", Toast.LENGTH_SHORT).show();
                                    Log.e("loadNotes", "Error", throwable);
                                }
                        )
        );

    }

    private void getListNotis() {
        compositeDisposable.add(
                NoteDatabase.getDatabase(requireContext()).notiDao().getAllNotis()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    notis = result;
                                    listNotiComing();
                                    listNotiUnRead();
                                },
                                throwable -> {
                                    Toast.makeText(requireContext(), "Lỗi khi tải thông báo", Toast.LENGTH_SHORT).show();
                                    Log.e("getListNotis", "Error loading notifications", throwable);
                                }
                        )
        );
    }


    private void listNotiUnRead() {
        if (notisUnRead != null){
            notisUnRead.clear();
        }else notisUnRead = new ArrayList<>();
        for (Noti noti : notis) {
            if (noti.isNotified() == true) { //đã thông báo
                notisUnRead.add(noti);
            }
        }
        binding.reccyclerViewNotiUnRead.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        adapterUnRead = new NotiAdapter(notisUnRead, 2, requireContext(), NotiFragment.this);
        binding.reccyclerViewNotiUnRead.setAdapter(adapterUnRead);
    }

    private void listNotiComing() {
        if (notisComing != null){
            notisComing.clear();
        }else notisComing = new ArrayList<>();
        for (Noti noti : notis) {
            if (noti.isNotified() == false) { //chưa thông báo
                notisComing.add(noti);
            }
        }
        binding.reccyclerViewNotiComing.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        attachSwipe();
        adapterComing = new NotiAdapter(notisComing, 1, requireContext(), NotiFragment.this);
        binding.reccyclerViewNotiComing.setAdapter(adapterComing);
    }

    private void attachSwipe() {
        if (mySwipeHelper == null){
            mySwipeHelper = new MySwipeHelper(requireContext(), binding.reccyclerViewNotiComing, 200) {
                @Override
                public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer) {
                    buffer.clear();
                    buffer.add(new MyButton(requireContext(),
                            getString(R.string.delete),
                            30,
                            R.drawable.ic_delete,
                            Color.parseColor("#FF0000"),
                            pos -> {
                                deleteNotiFromSwipe(pos);
                            }));
                }
            };
        }
    }

    private void deleteNotiFromSwipe(int pos) {
        Noti noti = notisComing.get(pos);
        NoteDatabase db = NoteDatabase.getDatabase(requireContext());

        compositeDisposable.add(
                db.notiDao().getNotisByNoteId(noti.getIdNote())
                        .subscribeOn(Schedulers.io())
                        .flatMapCompletable(list -> {
                            Completable delete = db.notiDao().deleteNoti(noti.getId());

                            if (list.size() == 1) {
                                // This was the only Noti, so we need to also update the Note
                                Completable update = db.noteDao().updateCheckNoti(noti.getIdNote(), false);
                                return delete.andThen(update);
                            } else {
                                return delete;
                            }
                        })
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    // Hủy thông báo
                                    Intent intent = new Intent(requireContext(), ReminderReceiver.class);
                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                            requireContext(),
                                            noti.getId(),
                                            intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                    );
                                    AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                                    if (alarmManager != null) {
                                        alarmManager.cancel(pendingIntent);
                                    }

                                    // Cập nhật UI
                                    notisComing.remove(pos);
                                    adapterComing.notifyItemRemoved(pos);
                                },
                                throwable -> {
                                    Log.e("deleteNotiFromSwipe", "Error deleting noti", throwable);
                                    Toast.makeText(requireContext(), "Xóa thông báo thất bại", Toast.LENGTH_SHORT).show();
                                }
                        )
        );
    }


    @Override
    public void onNoteClicked(Note note) {

    }

    @Override
    public void onTagClicked(String tag, int position) {

    }

    @Override
    public void onNotiClicked(Noti noti, int type) {
        if (type == 1){
            Intent intent = new Intent(requireContext(), AddNotiActivity.class);
            intent.putExtra("noti", noti);
            startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTI);
        }else if (type == 2){
            NoteDatabase db = NoteDatabase.getDatabase(requireContext());

            Completable updateNotiCompletable;
            if (noti.getOption().equals(getString(R.string.repeat_none))) {
                // Nếu không lặp lại thì xoá noti và cập nhật Note
                updateNotiCompletable = db.noteDao().updateCheckNoti(noti.getIdNote(), false)
                        .andThen(db.notiDao().deleteNoti(noti.getId()));
            } else {
                // Nếu có lặp lại thì chỉ đánh dấu notified = false
                updateNotiCompletable = db.notiDao().updateNotified(noti.getId(), false);
            }

            compositeDisposable.add(
                    updateNotiCompletable
                            .andThen(db.noteDao().getNoteWithId(noti.getIdNote()))
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    note -> {
                                        notisUnRead.remove(noti);
                                        adapterUnRead.notifyDataSetChanged();

                                        Intent intent = new Intent(requireContext(), NoteActivity.class);
                                        intent.putExtra("note", note);
                                        intent.putExtra("isUpdate", true);
                                        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
                                        requireActivity().finish();
                                    },
                                    throwable -> {
                                        Log.e("ReadNoteRx", "Error handling noti", throwable);
                                        Toast.makeText(requireContext(), "Đã xảy ra lỗi", Toast.LENGTH_SHORT).show();
                                    }
                            )
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapterComing != null && adapterUnRead != null){
            getListNotis();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        binding = null; // nếu dùng ViewBinding
    }

}

//2 phần: note chưa thông báo và note đã thông báo, chưa xem -> vuốt ngang để xóa, note đã xem thì không hiển thị
//nút thêm thông báo