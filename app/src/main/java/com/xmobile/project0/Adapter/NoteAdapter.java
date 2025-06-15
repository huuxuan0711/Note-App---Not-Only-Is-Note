package com.xmobile.project0.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.DAO.NoteDao;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;
import com.xmobile.project0.Helper.ItemLongClickListener;
import com.xmobile.project0.Helper.ItemTouchHelperAdapter;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class NoteAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements ItemTouchHelperAdapter {
    private List<Note> notes;
    private Context context;
    private int typeList = 1;
    private int typeNote = 1;
    private ItemTouchHelper touchHelper;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private ItemLongClickListener listener1;
    private ItemClickListener listener2;

    public NoteAdapter(List<Note> notes, ItemLongClickListener listener1, ItemClickListener listener2) {
        this.notes = notes;
        this.listener1 = listener1;
        this.listener2 = listener2;
    }

    public NoteAdapter(List<Note> notes, ItemClickListener listener2) {
        this.notes = notes;
        this.listener2 = listener2;
    }

    public NoteAdapter(List<Note> notes) {
        this.notes = notes;
    }

    public void clear(){
        context = null;
        touchHelper = null;
        listener1 = null;
        listener2 = null;
    }

    public void setTypeList(int value) {
        typeList = value;
        notifyDataSetChanged();
    }

    public void setTypeNote(int value){
        typeNote = value;
        notifyDataSetChanged();
    }

    public void setNotes(List<Note> notes) {
        this.notes.clear();
        this.notes.addAll(notes);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        compositeDisposable.clear();
    }


    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Note fromNote = notes.get(fromPosition);
        Note toNote = notes.get(toPosition);
        Log.d("TAG", "onItemMove: " + fromPosition + " " + toPosition);
        notes.remove(fromNote);
        notes.add(toPosition, fromNote);

        //lưu thay đổi vào database
        NoteDao noteDao = NoteDatabase.getDatabase(context).noteDao();

        Completable update1 = noteDao.updatePos(toNote.getId(), fromNote.getPos());
        Completable update2 = noteDao.updatePos(fromNote.getId(), toNote.getPos());

        compositeDisposable.add(
                Completable.mergeArray(update1, update2)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> Log.d("UpdateMove", "Hoán đổi vị trí thành công"),
                                throwable -> Log.e("UpdateMove", "Lỗi khi hoán đổi vị trí", throwable)
                        )
        );

        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemSwiped(int position) {

    }

    public void setTouchHelper(ItemTouchHelper itemTouchHelper){
        touchHelper = itemTouchHelper;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        if (typeList == 1) {
            return new ListNoteViewHolder(LayoutInflater.from(context).inflate(R.layout.item_note_list, parent, false));
        } else if (typeList == 0){
            return new GridNoteViewHolder(LayoutInflater.from(context).inflate(R.layout.item_note_grid, parent, false));
        }
        return new ListNoteViewHolder(LayoutInflater.from(context).inflate(R.layout.item_note_list, parent, false));
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Note note = notes.get(position);

        if (note.getPos() == 0) {
            compositeDisposable.add(
                    NoteDatabase.getDatabase(context).noteDao()
                            .updatePos(note.getId(), note.getId())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    () -> Log.d("UpdatePos", "Cập nhật vị trí thành công"),
                                    throwable -> Log.e("UpdatePos", "Lỗi cập nhật vị trí", throwable)
                            )
            );
        }

        if (holder instanceof ListNoteViewHolder) {
            ListNoteViewHolder listNoteViewHolder = (ListNoteViewHolder) holder;

            listNoteViewHolder.noteTitle.setText(note.getTitle());

            if (note.getDescription() == null){
                listNoteViewHolder.noteDescription.setVisibility(View.GONE);
            }else listNoteViewHolder.noteDescription.setText(note.getDescription());

            listNoteViewHolder.noteDate.setText(note.getDateTime());

            if (note.getPathImage() != null){
                listNoteViewHolder.noteImage.setImageBitmap(BitmapFactory.decodeFile(note.getPathImage()));
                listNoteViewHolder.noteImage.setVisibility(View.VISIBLE);
            }else {
                listNoteViewHolder.noteImage.setVisibility(View.GONE);
            }

            if (typeNote == 1){
                listNoteViewHolder.layoutChecked.setVisibility(View.GONE);
                //chế độ chọn
            }else if (typeNote == 0) {
                listNoteViewHolder.layoutChecked.setVisibility(View.VISIBLE);
                listNoteViewHolder.imgChecked.setVisibility(note.isChecked() ? View.VISIBLE : View.GONE);
                listNoteViewHolder.imgUnchecked.setVisibility(note.isChecked() ? View.GONE : View.VISIBLE);
            }

        }else if (holder instanceof GridNoteViewHolder){
            GridNoteViewHolder gridNoteViewHolder = (GridNoteViewHolder) holder;

            gridNoteViewHolder.noteTitle.setText(note.getTitle());

            if (note.getDescription() == null){
                gridNoteViewHolder.noteDescription.setVisibility(View.GONE);
            }else gridNoteViewHolder.noteDescription.setText(note.getDescription());

            gridNoteViewHolder.noteDate.setText(note.getDateTime());

            if (note.getPathImage() != null){
                gridNoteViewHolder.noteImage.setImageBitmap(BitmapFactory.decodeFile(note.getPathImage()));
                gridNoteViewHolder.noteImage.setVisibility(View.VISIBLE);
            }else {
                gridNoteViewHolder.noteImage.setVisibility(View.GONE);
            }

            if (typeNote == 1){
                gridNoteViewHolder.imgUnchecked.setVisibility(View.GONE);
                gridNoteViewHolder.imgChecked.setVisibility(View.GONE);
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        listener1.onItemLongClick(position, v);
                        return true;
                    }
                });
                //chế độ chọn
            } else if (typeNote == 0) {
                gridNoteViewHolder.imgChecked.setVisibility(note.isChecked() ? View.VISIBLE : View.GONE);
                gridNoteViewHolder.imgUnchecked.setVisibility(note.isChecked() ? View.GONE : View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (typeNote == 1) {
                    // Chế độ xem
                    listener2.onNoteClicked(note);
                } else if (typeNote == 0) {
                    // Chế độ chọn
                    note.setChecked(!note.isChecked());
                    compositeDisposable.add(
                            NoteDatabase.getDatabase(context)
                                    .noteDao()
                                    .updateChecked(note.getId(), note.isChecked())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(
                                            () -> {
                                                // Thành công, không cần làm gì hoặc log
                                            },
                                            throwable -> Log.e("RxJava", "Lỗi updateChecked", throwable)
                                    )
                    );

                    // Cập nhật giao diện
                    if (holder instanceof ListNoteViewHolder) {
                        ((ListNoteViewHolder) holder).imgChecked.setVisibility(note.isChecked() ? View.VISIBLE : View.GONE);
                        ((ListNoteViewHolder) holder).imgUnchecked.setVisibility(note.isChecked() ? View.GONE : View.VISIBLE);
                    } else if (holder instanceof GridNoteViewHolder) {
                        ((GridNoteViewHolder) holder).imgChecked.setVisibility(note.isChecked() ? View.VISIBLE : View.GONE);
                        ((GridNoteViewHolder) holder).imgUnchecked.setVisibility(note.isChecked() ? View.GONE : View.VISIBLE);
                    }
                }
            }
        });


    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (typeList == 1) {
            return 1;
        } else if (typeList == 0) {
            return 0;
        }
        return 1;
    }

    public class ListNoteViewHolder extends RecyclerView.ViewHolder implements GestureDetector.OnGestureListener {
        GestureDetector gestureDetector;
        TextView noteTitle, noteDescription, noteDate;
        ImageView noteImage, imgChecked, imgUnchecked;
        LinearLayout layoutChecked;

        public ListNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            gestureDetector = new GestureDetector(itemView.getContext(), this);
            noteTitle = itemView.findViewById(R.id.note_title_list);
            noteDescription = itemView.findViewById(R.id.note_description);
            noteDate = itemView.findViewById(R.id.note_date);
            noteImage = itemView.findViewById(R.id.note_image);
            layoutChecked = itemView.findViewById(R.id.layoutChecked);
            imgChecked = itemView.findViewById(R.id.imgChecked);
            imgUnchecked = itemView.findViewById(R.id.imgUnchecked);
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(@NonNull MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            touchHelper.startDrag(this);
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }

    public class GridNoteViewHolder extends RecyclerView.ViewHolder implements GestureDetector.OnGestureListener{
        GestureDetector gestureDetector;
        TextView noteTitle, noteDescription, noteDate;
        ImageView noteImage, imgChecked, imgUnchecked;

        public GridNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            gestureDetector = new GestureDetector(itemView.getContext(), this);
            noteTitle = itemView.findViewById(R.id.note_title_grid);
            noteDescription = itemView.findViewById(R.id.note_description);
            noteDate = itemView.findViewById(R.id.note_date);
            noteImage = itemView.findViewById(R.id.note_image);
            imgChecked = itemView.findViewById(R.id.imgChecked);
            imgUnchecked = itemView.findViewById(R.id.imgUnchecked);
        }

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(@NonNull MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            touchHelper.startDrag(this);
        }

        @Override
        public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}
