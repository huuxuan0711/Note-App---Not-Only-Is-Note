package com.xmobile.project0.Helper;

import com.xmobile.project0.Entities.Note;
import com.xmobile.project0.Entities.Noti;

public interface ItemClickListener {
    void onNoteClicked(Note note);
    void onTagClicked(String tag, int position);
    void onNotiClicked(Noti noti, int type);
}
