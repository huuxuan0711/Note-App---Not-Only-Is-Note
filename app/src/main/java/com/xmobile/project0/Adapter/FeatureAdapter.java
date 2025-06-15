package com.xmobile.project0.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.Helper.ItemFeatureClickListener;
import com.xmobile.project0.R;

import java.util.List;

public class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.ViewHolder> {
    List<String> featureChooses;
    Context context;
    int type;
    ItemFeatureClickListener listener;

    public FeatureAdapter(List<String> featureChooses, Context context, int type, ItemFeatureClickListener listener) {
        this.featureChooses = featureChooses;
        this.context = context;
        this.type = type;
        this.listener = listener;
    }

    public void clear(){
        context = null;
        listener = null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_feature_setting, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.txtNameFeature.setText(featureChooses.get(position));
        String choose = "";
        if (type == 1) { //chế độ nền
            int chooseResID = context.getSharedPreferences("choose", Context.MODE_PRIVATE).getInt("chooseTheme", R.string.light);
            choose = context.getString(chooseResID);
        }else if (type == 2){ //font hệ thống
            choose = context.getSharedPreferences("choose", Context.MODE_PRIVATE).getString("chooseFont", "Roboto");
            Typeface typeface;
            switch (featureChooses.get(position)) {
                case "Inter":
                    typeface = ResourcesCompat.getFont(context, R.font.inter_regular);
                    break;
                case "Lato":
                    typeface = ResourcesCompat.getFont(context, R.font.lato_regular);
                    break;
                case "Nunito Sans":
                    typeface = ResourcesCompat.getFont(context, R.font.nunito_sans_regular);
                    break;
                case "Open Sans":
                    typeface = ResourcesCompat.getFont(context, R.font.open_sans_regular);
                    break;
                default:
                    typeface = ResourcesCompat.getFont(context, R.font.roboto_regular);
                    break;
            }

            // Fallback nếu font bị null
            if (typeface == null) {
                typeface = Typeface.SANS_SERIF;
            }
            holder.txtNameFeature.setTypeface(typeface);
        }else if (type == 3){ //ngôn ngữ hệ thống
            int chooseResID = context.getSharedPreferences("choose", Context.MODE_PRIVATE).getInt("chooseLanguage", 0);
            if (chooseResID == 0) {
                chooseResID = R.string.vietnamese;
            }
            choose = context.getString(chooseResID);
            Log.d("choose", choose);
        }

        if (featureChooses.get(position).equals(choose)) {
            holder.imgChoose.setVisibility(View.VISIBLE);
        }else {
            holder.imgChoose.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onFeatureClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return featureChooses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNameFeature;
        ImageView imgChoose;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNameFeature = itemView.findViewById(R.id.choose_name);
            imgChoose = itemView.findViewById(R.id.imgCheck);
        }
    }
}
