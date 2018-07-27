package com.github.wontakkim.rxscreencaputre;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
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
import io.reactivex.ObservableSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

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
        if (windowManager == null) {
            return Observable.error(new NullPointerException());
        }

        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        return capture(new Rect(0, 0, metrics.widthPixels, metrics.heightPixels));
    }

    public Observable<Bitmap>capture(Rect rect) {
        return Observable.just(rect)
                .flatMap(new Function<Rect, ObservableSource<Bitmap>>() {
                    @Override
                    public ObservableSource<Bitmap> apply(@NonNull Rect rect) throws Exception {
                        final DisplayMetrics metrics = new DisplayMetrics();
                        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
                        windowManager.getDefaultDisplay().getRealMetrics(metrics);

                        final MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(MEDIA_PROJECTION_SERVICE);
                        final MediaProjection mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
                        final ImageReader imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2);
                        final VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                                TAG, metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                                imageReader.getSurface(), null, null);

                        final PublishSubject<Bitmap> subject = PublishSubject.create();
                        imageReader.setOnImageAvailableListener(reader -> {
                            Image image = imageReader.acquireLatestImage();
                            if (image != null) {
                                Image.Plane[] planes = image.getPlanes();
                                ByteBuffer buffer = planes[0].getBuffer();
                                int pixelStride = planes[0].getPixelStride();
                                int rowStride = planes[0].getRowStride();
                                int rowPadding = rowStride - pixelStride * metrics.widthPixels;

                                // create bitmap
                                Bitmap bitmap = Bitmap.createBitmap(metrics.widthPixels + rowPadding / pixelStride, metrics.heightPixels, Bitmap.Config.ARGB_8888);
                                bitmap.copyPixelsFromBuffer(buffer);

                                Bitmap cropped = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height());
                                subject.onNext(cropped);

                                bitmap.recycle();
                                image.close();

                                subject.onComplete();
                            }

                            virtualDisplay.release();
                            imageReader.close();
                            mediaProjection.stop();
                        }, null);

                        return subject;
                    }
                });
    }
}
