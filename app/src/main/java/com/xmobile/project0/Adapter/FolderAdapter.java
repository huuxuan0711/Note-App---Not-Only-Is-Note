package com.xmobile.project0.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;
import com.xmobile.project0.Activity.ListNoteActivity;
import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Folder;
import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Helper.FinishRequestListener;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemTouchHelperAdapter;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> implements ItemTouchHelperAdapter {
    private List<Folder> folders;
    private Context context;
    private int state = 1;
    private ItemTouchHelper touchHelper;
    private AlertDialog dialogAddFolder;
    private FinishRequestListener listener;

    @SuppressLint("RestrictedApi")
    private MenuBuilder menuBuilder;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private NoteDatabase db;


    public FolderAdapter(List<Folder> folders, Context context, FinishRequestListener listener) {
        this.folders = folders;
        this.context = context;
        this.listener = listener;
    }

    public void clear(){
        context = null;
        touchHelper = null;
        dialogAddFolder = null;
        menuBuilder = null;
    }

    public void setState(int value) {
        state = value;
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        compositeDisposable.clear();
    }

    @NonNull
    @Override
    public FolderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        db = NoteDatabase.getDatabase(context);
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_folder_list, parent, false));
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onBindViewHolder(@NonNull FolderAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.folderName.setText(folders.get(position).getName());
        int id = folders.get(position).getId();
        Folder folder = folders.get(position);

        Single<Integer> sizeSingle;
        if (folder.getName().equals(context.getString(R.string.all_notes))){
            sizeSingle = db.folderDao().sizeFolder();
        }else sizeSingle = db.folderDao().sizeFolderWithId(id);
        compositeDisposable.add(
                sizeSingle
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                size -> {
                                    folder.setSize(size);
                                    holder.folderSize.setText(String.valueOf(size));
                                },
                                throwable -> Log.e("FolderAdapter", "Error getting folder size", throwable)
                        )
        );

        //trạng thái mở option
        if (state == 0){
            if (folders.get(position) != folders.get(0)){
                holder.btnOption.setVisibility(View.VISIBLE);
            }
            holder.btnNext.setVisibility(View.INVISIBLE);
            holder.folderSize.setVisibility(View.INVISIBLE);

            menuBuilder = new MenuBuilder(context);
            MenuInflater inflater = new MenuInflater(context);
            inflater.inflate(R.menu.menu_option_modify_folder, menuBuilder);

            //mở menu
            holder.btnOption.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.btnOption.setBackgroundResource(R.drawable.bg_stroke_oval_paleblue);
                    holder.btnOption.setColorFilter(R.color.paleBlue);

                    MenuPopupHelper menu_option = new MenuPopupHelper(context, menuBuilder, v);
                    menu_option.setForceShowIcon(true);
                    menu_option.setGravity(Gravity.END);

                    menuBuilder.setCallback(new MenuBuilder.Callback() {
                        @Override
                        public boolean onMenuItemSelected(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
                            //Chọn đổi tên
                            if (item.getItemId() == R.id.menu_rename){
                                if (dialogAddFolder == null){
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.layout_renamefolder, null);
                                    builder.setView(view);
                                    dialogAddFolder = builder.create();

                                    final TextInputLayout inputName = view.findViewById(R.id.edtNameFolder);
                                    final TextView checkFolder = view.findViewById(R.id.checkFolder);

                                    view.findViewById(R.id.txtConfirm).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            if (inputName.getEditText().getText().toString().isEmpty()) {
                                                checkFolder.setVisibility(View.VISIBLE);
                                                checkFolder.setText(context.getString(R.string.please_enter_folder_name));
                                            }else if(inputName.getEditText().getText().toString().length() > 20){
                                                checkFolder.setVisibility(View.VISIBLE);
                                                checkFolder.setText(context.getString(R.string.folder_name_is_so_long));
                                            }else {
                                                String nameFolder = inputName.getEditText().getText().toString().trim();

                                                Single<Integer> checkSingle = db.folderDao().checkFolder(nameFolder);
                                                Folder folder = folders.get(position);
                                                compositeDisposable.add(
                                                        checkSingle
                                                                .flatMap(
                                                                        check -> {
                                                                                if (check == 0) {
                                                                                    folder.setName(nameFolder);
                                                                                    return db.folderDao().updateFolder(folder.getId(), nameFolder).andThen(Single.just(0));
                                                                                }else return Single.just(check);
                                                                            }
                                                                        )
                                                                .subscribeOn(Schedulers.io())
                                                                .observeOn(AndroidSchedulers.mainThread())
                                                                .subscribe(
                                                                        check -> {
                                                                            if (check == 0) {
                                                                                checkFolder.setVisibility(View.INVISIBLE);
                                                                                inputName.getEditText().setText(null);
                                                                                inputName.clearFocus();
                                                                                dialogAddFolder.dismiss();
                                                                                notifyDataSetChanged();
                                                                            } else {
                                                                                checkFolder.setVisibility(View.VISIBLE);
                                                                                checkFolder.setText(context.getString(R.string.folder_name_is_existed));
                                                                            }
                                                                        },
                                                                        throwable -> Log.e("RenameFolder", "Error renaming folder", throwable)
                                                                )
                                                );
                                            }
                                        }
                                    });

                                    view.findViewById(R.id.txtCancel).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            checkFolder.setVisibility(View.INVISIBLE);
                                            inputName.getEditText().setText(null);
                                            inputName.clearFocus();
                                            dialogAddFolder.dismiss();
                                        }
                                    });
                                }
                                dialogAddFolder.show();
                                return true;
                                //Chọn xóa
                            } else if (item.getItemId() == R.id.menu_delelte) {
                                if (dialogAddFolder == null){
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                                    View view = LayoutInflater.from(context).inflate(R.layout.layout_deletefolder, null);
                                    builder.setView(view);
                                    dialogAddFolder = builder.create();

                                    view.findViewById(R.id.txtConfirm).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Single<List<Note>> noteSingle = db.noteDao().getAllNotesWithPosAndId(folders.get(position).getId());
                                            compositeDisposable.add(
                                                noteSingle
                                                        .flatMapCompletable(
                                                        notes -> Completable.fromAction(() -> {
                                                            db.folderDao().deleteFolder(folders.get(position).getId());
                                                            for (Note note : notes) {
                                                                int idNote = note.getId();
                                                                db.noteDao().deleteNote(idNote);
                                                                db.tagDao().deleteAllTagWithId(idNote);
                                                                db.noteLinkDao().deleteAllNoteLinksWithNoteId(idNote);
                                                                db.notiDao().deleteNotiWithIdNote(idNote);
                                                            }
                                                        })
                                                )
                                                        .subscribeOn(Schedulers.io())
                                                        .observeOn(AndroidSchedulers.mainThread())
                                                        .subscribe(
                                                                () -> {
                                                                    folders.remove(position);
                                                                    notifyDataSetChanged();
                                                                    dialogAddFolder.dismiss();
                                                                },
                                                                throwable -> {
                                                                    Log.e("DelFolder", "Error deleting folder", throwable);
                                                                }
                                                        )
                                            );
                                        }
                                    });

                                    view.findViewById(R.id.txtCancel).setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            dialogAddFolder.dismiss();
                                        }
                                    });
                                }
                                dialogAddFolder.show();
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void onMenuModeChange(@NonNull MenuBuilder menu) {

                        }
                    });

                    //Đặt lại trạng thái
                    menu_option.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            holder.btnOption.setBackgroundResource(R.drawable.bg_stroke_oval_blue);
                            holder.btnOption.setColorFilter(R.color.blue);
                        }
                    });
                    menu_option.show();
                }
            });
            //trạng thái ko mở option
        }else if(state == 1){
            holder.btnOption.setVisibility(View.GONE);
            holder.btnOption.setBackgroundResource(R.drawable.bg_stroke_oval_blue);
            holder.btnOption.setColorFilter(R.color.blue);
            holder.btnNext.setVisibility(View.VISIBLE);
            holder.folderSize.setVisibility(View.VISIBLE);
        }

        //Mở danh sách note
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == 1){
                    Intent intent = new Intent(context, ListNoteActivity.class);
                    intent.putExtra("folderId", folders.get(position).getId());
                    intent.putExtra("folderName", folders.get(position).getName());
                    context.startActivity(intent);
                    listener.onFinishRequested();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        Folder fromFolder = folders.get(fromPosition);
        folders.remove(fromFolder);
        folders.add(toPosition, fromFolder);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public void onItemSwiped(int position) {

    }

    public void setTouchHelper(ItemTouchHelper itemTouchHelper){
        this.touchHelper = itemTouchHelper;
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements GestureDetector.OnGestureListener{
        GestureDetector gestureDetector;
        TextView folderName, folderSize;
        ImageView btnOption, btnNext;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            gestureDetector = new GestureDetector(itemView.getContext(), this);
            folderName = itemView.findViewById(R.id.folder_name);
            folderSize = itemView.findViewById(R.id.folder_size);
            btnOption = itemView.findViewById(R.id.btn_option);
            btnNext = itemView.findViewById(R.id.btn_next);
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
}
