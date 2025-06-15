package com.xmobile.project0.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.Entities.Noti;
import com.xmobile.project0.R;
import com.xmobile.project0.Helper.ItemClickListener;

import java.util.List;

public class NotiAdapter extends RecyclerView.Adapter<NotiAdapter.ViewHolder> {
    List<Noti> notis;
    Context context;
    ItemClickListener listener;
    int type;

    public NotiAdapter(List<Noti> notis, int type, Context context, ItemClickListener listener) {
        this.notis = notis;
        this.type = type;
        this.context = context;
        this.listener = listener;
    }

    public void clear(){
        context = null;
        listener = null;
    }

    @NonNull
    @Override
    public NotiAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_noti, parent, false));
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull NotiAdapter.ViewHolder holder, int position) {
        Noti noti = notis.get(position);

        holder.noteTitle.setText(noti.getTitle());
        if (noti.getDescriptionNote() != null){
            holder.noteDescription.setText(noti.getDescriptionNote());
        }else {
            holder.noteDescription.setVisibility(View.GONE);
        }
        holder.notiDate.setText(noti.getDate() + ", " + noti.getHour());
        if (noti.getContent() != null){
            holder.notiContent.setText(context.getString(R.string.content_reminder) + ": " + noti.getContent());
        }
        holder.notiOptionRepetition.setText(context.getString(R.string.repeat) + ": " + noti.getOption());
        if (noti.getPathImage() != null){
            holder.noteImage.setImageBitmap(BitmapFactory.decodeFile(noti.getPathImage()));
            holder.noteImage.setVisibility(View.VISIBLE);
        }else {
            holder.noteImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onNotiClicked(noti, type);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notis.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView noteTitle, noteDescription, notiDate, notiContent, notiOptionRepetition;
        ImageView noteImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.note_title);
            noteDescription = itemView.findViewById(R.id.note_description);
            notiDate = itemView.findViewById(R.id.noti_date);
            notiContent = itemView.findViewById(R.id.noti_content);
            noteImage = itemView.findViewById(R.id.note_image);
            notiOptionRepetition = itemView.findViewById(R.id.noti_option_repetition);
        }
    }
}
