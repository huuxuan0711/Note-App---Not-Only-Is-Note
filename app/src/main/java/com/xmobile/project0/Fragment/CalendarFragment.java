package com.xmobile.project0.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.ViewContainer;
import com.xmobile.project0.Activity.AddNotiActivity;
import com.xmobile.project0.Activity.ListNoteActivity;
import com.xmobile.project0.Activity.NoteActivity;
import com.xmobile.project0.Adapter.NoteAdapter;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.Helper.MySwipeHelper;
import com.xmobile.project0.databinding.FragmentCalendarBinding;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CalendarFragment extends BaseFragment implements ItemClickListener {
    private FragmentCalendarBinding binding;
    private YearMonth currentMonth;
    private List<Note> notes = new ArrayList<>();
    private NoteAdapter adapter;
    private LinearLayout layoutCountedNote2, tempLayout;
    private TextView countNote2, tempCount;
    private String today2;
    private MySwipeHelper mySwipeHelper;
    private View previosTextView;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    private static final int REQUEST_CODE_ADD_NOTI = 4;
    public static final int REQUEST_CODE_UPDATE_NOTI = 5;


    public CalendarFragment() {

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initControl();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initControl() {
        LocalDate today = LocalDate.now();
        currentMonth = YearMonth.from(today);

        setupCalendarView(today);
        updateMonthTitle(binding.txtMonth, currentMonth);

        binding.btnBackMonth.setOnClickListener(v -> changeMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> changeMonth(1));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupCalendarView(LocalDate today) {
        binding.calendarView.setup(currentMonth, currentMonth, DayOfWeek.SUNDAY);
        binding.calendarView.scrollToMonth(currentMonth);

        binding.calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @Override
            public void bind(@NonNull DayViewContainer container, CalendarDay calendarDay) {
                setupDayView(container, calendarDay, today);
            }

            @NonNull
            @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupDayView(DayViewContainer container, CalendarDay calendarDay, LocalDate today) {
        TextView textView = container.textView;
        TextView countNote = container.countNote;
        LinearLayout layoutCountedNote = container.layoutCountedNote;

        textView.setText(String.valueOf(calendarDay.getDate().getDayOfMonth()));
        String formattedDate = getFormattedDate(calendarDay.getDate());

        if (calendarDay.getDate().equals(today)) {
            container.getView().setBackgroundResource(R.drawable.bg_today);
            today2 = formattedDate;
            listNote(formattedDate);
        } else if (calendarDay.getPosition() != DayPosition.MonthDate) {
            textView.setTextColor(getResources().getColor(R.color.lightGray));
            container.getView().setBackgroundResource(R.drawable.bg_unselected_day);
        } else {
            container.getView().setBackgroundResource(R.drawable.bg_unselected_day);
        }

        countNotes(formattedDate, countNote, layoutCountedNote, calendarDay, today);

        container.getView().setOnClickListener(new View.OnClickListener() {
            boolean isSelect = false;

            @Override
            public void onClick(View v) {
                if (!calendarDay.getDate().equals(today) && calendarDay.getPosition() == DayPosition.MonthDate) {
                    container.getView().setBackgroundResource(isSelect ? R.drawable.bg_unselected_day : R.drawable.bg_selected_day);

                    if (!isSelect) {
                        String selectedDate = getFormattedDate(calendarDay.getDate());
                        listNote(selectedDate);
                        countNote2 = container.countNote;
                        layoutCountedNote2 = container.layoutCountedNote;
                    } else {
                        listNote(getFormattedDate(today));
                        countNote2 = tempCount;
                        layoutCountedNote2 = tempLayout;
                    }

                    isSelect = !isSelect;

                    if (previosTextView != null && previosTextView != container.getView()) {
                        previosTextView.setBackgroundResource(R.drawable.bg_unselected_day);
                        isSelect = false;
                    }

                    previosTextView = container.getView();
                }
            }
        });
    }

    private void countNotes(String date, TextView countNote, LinearLayout layoutCountedNote, CalendarDay calendarDay, LocalDate today) {
        compositeDisposable.add(
                NoteDatabase.getDatabase(requireContext())
                        .noteDao()
                        .countNotesWithDay(date)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                sum -> {
                                    if (sum == 0) {
                                        layoutCountedNote.setVisibility(View.GONE);
                                    } else {
                                        layoutCountedNote.setVisibility(View.VISIBLE);
                                        countNote.setText(String.valueOf(sum));

                                        if (calendarDay.getDate().equals(today)) {
                                            countNote2 = countNote;
                                            tempCount = countNote2;
                                            layoutCountedNote2 = layoutCountedNote;
                                            tempLayout = layoutCountedNote2;
                                        }
                                    }
                                },
                                throwable -> Log.e("RxJava", "Lỗi đếm note theo ngày", throwable)
                        )
        );

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void changeMonth(int offset) {
        currentMonth = currentMonth.plusMonths(offset);
        binding.calendarView.setup(currentMonth, currentMonth, DayOfWeek.SUNDAY);
        binding.calendarView.scrollToMonth(currentMonth);
        updateMonthTitle(binding.txtMonth, currentMonth);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMonthTitle(TextView monthTitle, YearMonth currentMonth) {
        String monthName = currentMonth.getMonth().name().substring(0, 1).toUpperCase() + currentMonth.getMonth().name().substring(1).toLowerCase();
        monthTitle.setText(monthName + " " + currentMonth.getYear());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String getFormattedDate(LocalDate date) {
        // Định dạng ngày theo dd/MM/yyyy
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return "%"+ date.format(formatter);
    }

    static class DayViewContainer extends ViewContainer {
        TextView textView, countNote;
        LinearLayout layoutCountedNote;

        public DayViewContainer(@NonNull View view) {
            super(view);
            textView = view.findViewById(R.id.dayText);
            countNote = view.findViewById(R.id.countNote);
            layoutCountedNote = view.findViewById(R.id.layout_count_note);
        }
    }

    private void listNote(String day){
        compositeDisposable.add(
                NoteDatabase.getDatabase(requireContext())
                        .noteDao()
                        .getAllNotesWithDay(day)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                result -> {
                                    notes = result;
                                    binding.recyclerViewListNoteWithDay.setLayoutManager(new LinearLayoutManager(requireContext()));
                                    adapter = new NoteAdapter(notes, CalendarFragment.this);
                                    attachSwipe();
                                    binding.recyclerViewListNoteWithDay.setAdapter(adapter);
                                },
                                throwable -> Log.e("RxJava", "Lỗi load note theo ngày", throwable)
                        )
        );


    }

    private void attachSwipe() {
        if (mySwipeHelper == null){
            mySwipeHelper = new MySwipeHelper(requireContext(), binding.recyclerViewListNoteWithDay, 200) {
                @Override
                public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer) {
                    buffer.clear();
                    buffer.add(new MyButton(requireContext(),
                            getString(R.string.delete),
                            30,
                            R.drawable.ic_delete,
                            Color.parseColor("#FF0000"),
                            pos -> {
                                deleteNoteFromSwipe(pos);
                            }));
                    buffer.add(new MyButton(requireContext(),
                            getString(R.string.reminder),
                            30,
                            R.drawable.ic_noti,
                            Color.parseColor("#007AFF"),
                            pos -> {
                                alarmNoteFromSwipe(pos);
                            }));
                }
            };
        }
    }

    private void alarmNoteFromSwipe(int pos) {
        Note currentNote = notes.get(pos);
        if (currentNote.isNoti()){
            compositeDisposable.add(
                    NoteDatabase.getDatabase(requireContext())
                            .notiDao()
                            .getNotisByNoteId(currentNote.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    notis -> {
                                        if (!notis.isEmpty()) {
                                            Noti noti = notis.get(0);
                                            Intent intent = new Intent(requireContext(), AddNotiActivity.class);
                                            intent.putExtra("noti", noti);
                                            startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTI);
                                        } else {
                                            Log.w("RxJava", "Không có noti nào cho note này");
                                        }
                                    },
                                    throwable -> Log.e("RxJava", "Lỗi khi lấy noti", throwable)
                            )
            );

        }else {
            Intent intent = new Intent(requireContext(), AddNotiActivity.class);
            intent.putExtra("idNote", currentNote.getId());
            intent.putExtra("titleNote", currentNote.getTitle());
            intent.putExtra("descriptionNote", currentNote.getDescription());
            intent.putExtra("pathImageNote", currentNote.getPathImage());
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTI);
        }
    }

    private void deleteNoteFromSwipe(int pos) {
        Note note = notes.get(pos);
        int noteId = note.getId();

        compositeDisposable.add(
                Completable.fromAction(() -> {
                            NoteDatabase db = NoteDatabase.getDatabase(requireContext());
                            db.noteDao().deleteNote(noteId);
                            db.notiDao().deleteNotiWithIdNote(noteId);
                            db.tagDao().deleteAllTagWithId(noteId);
                            db.noteLinkDao().deleteAllNoteLinksWithNoteId(noteId);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> {
                                    // ✅ On success: cập nhật UI
                                    notes.remove(pos);
                                    adapter.notifyItemRemoved(pos);
                                    int count = Integer.parseInt(countNote2.getText().toString());
                                    if (count - 1 == 0) {
                                        layoutCountedNote2.setVisibility(View.GONE);
                                    }
                                    countNote2.setText(String.valueOf(count - 1));
                                },
                                throwable -> {
                                    // ⚠️ Bắt lỗi để không crash
                                    Log.e("DelNoteRx", "Lỗi xóa ghi chú", throwable);
                                    Toast.makeText(requireContext(), "Lỗi khi xóa ghi chú", Toast.LENGTH_SHORT).show();
                                }
                        )
        );
    }

    @Override
    public void onNoteClicked(Note note) {
        Intent intent = new Intent(requireContext(), NoteActivity.class);
        intent.putExtra("note", note);
        intent.putExtra("isUpdate", true);
        intent.putExtra("fromCalendarFragment", true);
        startActivityForResult(intent, REQUEST_CODE_UPDATE_NOTE);
        requireActivity().finish();
    }

    @Override
    public void onTagClicked(String tag, int position) {

    }

    @Override
    public void onNotiClicked(Noti noti, int type) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_UPDATE_NOTE && resultCode == Activity.RESULT_OK) {
            if (data.getBooleanExtra("isDelete", false)) {
                listNote(today2);
                int count = Integer.parseInt(countNote2.getText().toString());
                if (count - 1 == 0){
                    layoutCountedNote2.setVisibility(View.GONE);
                }
                countNote2.setText(String.valueOf(count - 1));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null){
            listNote(today2);
            if (previosTextView != null){
                previosTextView.setBackgroundResource(R.drawable.bg_unselected_day);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (compositeDisposable != null && !compositeDisposable.isDisposed()) {
            compositeDisposable.clear();
        }
        binding = null;
    }

}

//hiển thị các note trong ngày, note được báo nhắc thì thêm icon chuông
//ấn vào 1 ngày -> hiển thị fragment chứa các note trong ngày đó, note được báo nhắc thì thêm icon chuông