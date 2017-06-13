package com.github.wontakkim.rxscreencaputre;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

import io.reactivex.Observable;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static android.content.Context.WINDOW_SERVICE;

public final class RxScreenCapture {

    private static final String TAG = "RxScreenCapture";

    private Context context;
    private int resultCode;
    private Intent resultData;

    public RxScreenCapture(Context context, int resultCode, Intent resultData) {
        this.context = context;
        this.resultCode = resultCode;
        this.resultData = resultData;
    }

    public Observable<Bitmap> capture() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return capture(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
    }

    public Observable<Bitmap> capture(int width, int height) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return capture(width, height, metrics.densityDpi);
    }

    public Observable<Bitmap> capture(int width, int height, int density) {
        return Observable.create(emitter -> {
            final MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
            final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
            final ImageReader imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                    TAG, width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    imageReader.getSurface(), null, null);

            imageReader.setOnImageAvailableListener(reader -> {
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;

                    // create bitmap
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    emitter.onNext(bitmap);

                    bitmap.recycle();
                    image.close();
                }

                virtualDisplay.release();
                imageReader.close();
                mediaProjection.stop();
            }, null);
        });
    }
}
