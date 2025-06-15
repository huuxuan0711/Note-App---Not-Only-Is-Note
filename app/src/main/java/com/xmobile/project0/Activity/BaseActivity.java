package com.xmobile.project0.Activity;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xmobile.project0.Util.FontManager;

public class BaseActivity extends AppCompatActivity {
    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        View view = super.onCreateView(name, context, attrs);

        if (view instanceof TextView) {
            try {
                Typeface typeface = null;
                if (((TextView) view).getTypeface().getStyle() == Typeface.BOLD){
                    typeface = FontManager.getTypeface(context, Typeface.BOLD);
                }else{
                    typeface = FontManager.getTypeface(context, Typeface.NORMAL);
                }
                ((TextView) view).setTypeface(typeface);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return view;
    }
}
