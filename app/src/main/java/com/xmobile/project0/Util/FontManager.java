package com.xmobile.project0.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import com.xmobile.project0.R;

import java.util.HashMap;
import java.util.Map;

public class FontManager {
    private static final String TAG = "FontManager";

    public static Typeface getTypeface(Context context, int style) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("choose", Context.MODE_PRIVATE);
        String font = sharedPreferences.getString("chooseFont", "");  // mặc định chuỗi rỗng
        try {
            if (font == null || font.isEmpty()) {
                if (style == Typeface.BOLD) {
                    return safeGetFont(context, R.font.roboto_bold);
                }else if (style == Typeface.NORMAL) {
                    return safeGetFont(context, R.font.roboto_regular);
                }
            }

            switch (font) {
                case "Inter":
                    if (style == Typeface.BOLD) {
                        return safeGetFont(context, R.font.inter_bold);
                    }else if (style == Typeface.NORMAL) {
                        return safeGetFont(context, R.font.inter_regular);
                    }
                case "Lato":
                    if (style == Typeface.BOLD) {
                        return safeGetFont(context, R.font.lato_bold);
                    }else if (style == Typeface.NORMAL) {
                        return safeGetFont(context, R.font.lato_regular);
                    }
                case "Nunito Sans":
                    if (style == Typeface.BOLD) {
                        return safeGetFont(context, R.font.nunito_sans_bold);
                    }else if (style == Typeface.NORMAL) {
                        return safeGetFont(context, R.font.nunito_sans_regular);
                    }
                case "Open Sans":
                    if (style == Typeface.BOLD) {
                        return safeGetFont(context, R.font.open_sans_bold);
                    }else if (style == Typeface.NORMAL) {
                        return safeGetFont(context, R.font.open_sans_regular);
                    }
                case "Roboto":
                    if (style == Typeface.BOLD) {
                        return safeGetFont(context, R.font.roboto_bold);
                    }else if (style == Typeface.NORMAL) {
                        return safeGetFont(context, R.font.roboto_regular);
                    }
                default:
                    return Typeface.SANS_SERIF;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load font: " + font, e);
            return Typeface.SANS_SERIF;  // fallback an toàn
        }
    }

    private static final Map<String, Typeface> fontCache = new HashMap<>();

    private static Typeface safeGetFont(Context context, int fontResId) {
        String key = context.getResources().getResourceEntryName(fontResId);
        if (fontCache.containsKey(key)) {
            return fontCache.get(key);
        }

        try {
            Typeface tf = ResourcesCompat.getFont(context, fontResId);
            if (tf == null) throw new NullPointerException("Typeface is null");
            fontCache.put(key, tf);
            return tf;
        } catch (Exception e) {
            return Typeface.SANS_SERIF;
        }
    }
}
