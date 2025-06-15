package com.xmobile.project0.Fragment;

import android.graphics.Typeface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.xmobile.project0.Util.FontManager;

public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyFont(view);
    }

    // Gọi hàm applyFont sau khi inflate layout
    protected void applyFont(View root) {
        Typeface typeface;
        if (root instanceof TextView) {
            if (((TextView) root).getTypeface().getStyle() == Typeface.BOLD){
                typeface = FontManager.getTypeface(requireContext(), Typeface.BOLD);
            }else{
                typeface = FontManager.getTypeface(requireContext(), Typeface.NORMAL);
            }
            ((TextView) root).setTypeface(typeface);
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyFont(group.getChildAt(i));
            }
        } else if (root instanceof WebView) {
            // Bỏ qua để tự xử lý bằng CSS trong HTML
        }
    }
}
