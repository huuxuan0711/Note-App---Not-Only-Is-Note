package com.xmobile.project0.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.Database.NoteDatabase;
import com.xmobile.project0.Entities.Tag;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.ViewHolder> {
    private List<Tag> tags;
    private Context context;
    private ItemClickListener itemClickListener;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public TagAdapter(List<Tag> tags, Context context, ItemClickListener itemClickListener) {
        this.tags = tags;
        this.context = context;
        this.itemClickListener = itemClickListener;
    }

    public TagAdapter(List<Tag> tags, Context context) {
        this.tags = tags;
        this.context = context;
    }

    public void clear(){
        context = null;
        itemClickListener = null;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        compositeDisposable.clear();
    }


    @NonNull
    @Override
    public TagAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TagAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.nameTag.setText(tags.get(position).getNameTag());

        compositeDisposable.add(
                NoteDatabase.getDatabase(context)
                        .tagDao()
                        .countNoteWithTag(tags.get(position).getNameTag())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                count -> holder.countNote.setText(String.valueOf(count)),
                                throwable -> Log.e("RxJava", "Lỗi đếm số note theo tag", throwable)
                        )
        );


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onTagClicked(tags.get(position).getNameTag(), position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTag, countNote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTag = itemView.findViewById(R.id.tag_name);
            countNote = itemView.findViewById(R.id.count_note);
        }
    }
}
