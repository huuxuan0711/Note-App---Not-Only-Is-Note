package com.xmobile.project0.Helper;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.xmobile.project0.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public abstract class MySwipeHelper extends ItemTouchHelper.SimpleCallback {

    int buttonWidth;
    private RecyclerView recyclerView;
    private List<MyButton> buttonList;
    private GestureDetector gestureDetector;
    private int swipePosition = -1; //vị trí item đang swipe
    private float swipeThreshold = 0.5f; //ngưỡng kích hoạt
    private Map<Integer, List<MyButton>> buttonBuffer; //chứa các button đang hiển thị
    private Queue<Integer> removeQueue; //chứa vị trí item bị swipe

    //hỗ trợ click
    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            for (MyButton button : buttonList){
                if (button.onClick(e.getX(), e.getY()))
                    break;
            }
            return true;
        }
    };

    //xử lý chạm trên item được swipe
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (swipePosition < 0) return false;
            Point point = new Point((int) event.getRawX(), (int) event.getRawY());

            RecyclerView.ViewHolder swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition);
            View swipedItem = swipeViewHolder.itemView;
            Rect rect = new Rect();
            swipedItem.getGlobalVisibleRect(rect);

            if (event.getAction() == MotionEvent.ACTION_DOWN ||
                    event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_MOVE){
                if (rect.top < point.y && rect.bottom > point.y){ //chạm bên trong
                    gestureDetector.onTouchEvent(event);
                }else { //chạm bên ngoài -> khôi phục trạng thái
                    removeQueue.add(swipePosition);
                    swipePosition = -1;
                    recoverSwipedItem();
                    swipeViewHolder.itemView.setBackgroundResource(R.drawable.bg_note);
                }
            }
            return false;
        }
    };

    //khởi tạo swipe helper
    public MySwipeHelper(Context context, RecyclerView recyclerView, int buttonWidth) {
        super(0, ItemTouchHelper.LEFT);
        this.recyclerView = recyclerView;
        this.buttonList = new ArrayList<>();
        this.gestureDetector = new GestureDetector(context, gestureListener);
        this.recyclerView.setOnTouchListener(onTouchListener);
        this.buttonBuffer = new HashMap<>();
        this.buttonWidth = buttonWidth;

        removeQueue = new LinkedList<Integer>(){
            @Override
            public boolean add(Integer integer) {
                if (contains(integer)){
                    return false;
                }else
                    return super.add(integer);
            }
        };
        attachSwipe();
    }

    private void attachSwipe() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public class MyButton {
        private String text;
        private int imageResId, textSize, color, pos;
        private RectF clickRegion;
        private MyButtonClickListener listener;
        private Context context;
        private Resources resources;

        public MyButton(Context context, String text,int textSize, int imageResId, int color, MyButtonClickListener listener) {
            this.text = text;
            this.imageResId = imageResId;
            this.textSize = textSize;
            this.color = color;
            this.listener = listener;
            this.context = context;
            resources = context.getResources();
        }

        //thực hiện click
        public boolean onClick(float x, float y){
            if (clickRegion != null && clickRegion.contains(x, y)){
                listener.onClick(pos);
                return true;
            }
            return false;
        }

        //vẽ hình chữ nhật lên canvas có chứa chữ/hình ảnh được căn giữa
        public void onDraw(Canvas c, RectF rectF, int pos){
            Paint p = new Paint();
            p.setColor(color);
            c.drawRect(rectF, p);
            //text
            p.setColor(Color.WHITE);
            p.setTextSize(textSize);

            Rect r = new Rect();
            float cHeight = rectF.height();
            float cWidth = rectF.width();
            p.setTextAlign(Paint.Align.LEFT);
            p.getTextBounds(text, 0, text.length(), r);
            float x = 0, y = 0;
            if (imageResId == 0) { //if just show text
                x = cWidth / 2f - r.width() / 2f - r.left;
                y = cHeight / 2f + r.height() / 2f - r.bottom;
                c.drawText(text, rectF.left + x, rectF.top + y, p);
            }else{
                Drawable d = ContextCompat.getDrawable(context, imageResId);
                d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                Bitmap bitmap = drawableToBitmap(d);
                float bitmapX = (rectF.left + rectF.right) / 2 - ((float) bitmap.getWidth() / 2);
                float bitmapY = (rectF.top + rectF.bottom) / 2 - ((float) bitmap.getHeight() / 2);
                c.drawBitmap(bitmap, bitmapX, bitmapY, p);
            }
            clickRegion = rectF;
            this.pos = pos;
        }
    }

    //vẽ drawable lên bitmap
    private Bitmap drawableToBitmap(Drawable d) {
        if (d instanceof BitmapDrawable){
            return ((BitmapDrawable) d).getBitmap();
        }
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(),
                d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return bitmap;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    //xử lý trạng thái
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAbsoluteAdapterPosition(); //lấy vị trí bị swipe
        viewHolder.itemView.setBackgroundResource(R.drawable.bg_note_swipe);
        if (swipePosition != pos){
            removeQueue.add(swipePosition); //thêm vị trí cũ vào hàng đợi để khôi phục trạng thái
        }
        swipePosition = pos; //cập nhật vị trí
        if (buttonBuffer.containsKey(swipePosition)){
            buttonList = buttonBuffer.get(swipePosition); //lấy từ bộ đệm
        }else {
            buttonList.clear();
        }
        buttonBuffer.clear(); //giải phóng bộ đệm
        swipeThreshold = 0.5f * buttonList.size() * buttonWidth; //tính toán ngưỡng kích hoạt
        recoverSwipedItem();
    }



    //phục hồi item đã bị swipe
    private synchronized void recoverSwipedItem() {
        while (!removeQueue.isEmpty()){
            int pos = removeQueue.poll();
            if (pos > -1){ //vị trí hợp lệ
                recyclerView.getAdapter().notifyItemChanged(pos);
            }
        }
    }

    public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
        return swipeThreshold;
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return 0.1f * defaultValue;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return 0.5f * defaultValue;
    }

    //xử lý hiển thị button khi vuốt
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        int pos = viewHolder.getAbsoluteAdapterPosition();
        float translationX = dX; //độ dịch chuyển
        View itemView = viewHolder.itemView;
        if (pos < 0){ //vị trí ở không hợp lệ
            swipePosition = pos;
            return;
        }
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE){
            if (dX < 0){
                List<MyButton> buffer = new ArrayList<>();
                if (!buttonBuffer.containsKey(pos)){
                    instantiateMyButton(viewHolder, buffer); //tạo các nút
                    buttonBuffer.put(pos, buffer); //thêm pos và list button vào bộ đệm
                }else {
                    buffer = buttonBuffer.get(pos); //đã có -> lấy từ bộ đệm
                }
                translationX = dX * buffer.size() * buttonWidth / itemView.getWidth();
                drawButton(c, itemView, buffer, pos, translationX);
            }else if (dX == 0){
                viewHolder.itemView.setBackgroundResource(R.drawable.bg_note);
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive);
    }

    //vẽ các nút
    private void drawButton(Canvas c, View itemView, List<MyButton> buffer, int pos, float translationX) {
        float right = itemView.getRight();
        float dButtonWidth = -1 * translationX / buffer.size();
        for (MyButton button:buffer){
            float left = right - dButtonWidth;
            button.onDraw(c, new RectF(left, itemView.getTop(), right, itemView.getBottom()), pos);
            right = left;
        }
    }

    public abstract void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buffer);
}
