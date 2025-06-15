package com.xmobile.project0.Util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.widget.ScrollView;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class ScreenshotUtil {

    // Phương thức chụp toàn bộ nội dung của ScrollView
    public static Bitmap captureFullScrollViewScreenshot(ScrollView scrollView) {
        int totalHeight = 0;

        // Tính tổng chiều cao của tất cả các phần tử con trong ScrollView
        for (int i = 0; i < scrollView.getChildCount(); i++) {
            totalHeight += scrollView.getChildAt(i).getHeight();
        }

        Bitmap fullBitmap = Bitmap.createBitmap(scrollView.getWidth(), totalHeight, Bitmap.Config.ARGB_8888);
        Canvas fullCanvas = new Canvas(fullBitmap);

        // Vẽ toàn bộ nội dung vào Canvas
        scrollView.draw(fullCanvas);

        return fullBitmap;
    }

    // Phương thức lưu Bitmap vào PDF (ví dụ sử dụng iText)
    public static void createPdfWithScreenshot(Bitmap bitmap, String outputPath) throws FileNotFoundException {
        // Tạo FileOutputStream và PdfWriter
        File file = new File(outputPath);
        file.getParentFile().mkdirs();
        PdfWriter writer = new PdfWriter(new FileOutputStream(file));
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document doc = new Document(pdfDoc);

        // Chuyển Bitmap thành Image
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        // Tạo Image từ byte array
        Image image = new Image(ImageDataFactory.create(byteArray));

        // Đặt kích thước trang PDF sao cho vừa với kích thước của ảnh
        pdfDoc.setDefaultPageSize(new PageSize(image.getImageWidth(), image.getImageHeight()));

        // Thêm ảnh vào PDF
        doc.add(image);

        // Đóng tài liệu PDF
        doc.close();
    }
}
