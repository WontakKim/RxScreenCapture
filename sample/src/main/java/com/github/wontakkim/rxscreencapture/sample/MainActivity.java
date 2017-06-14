package com.github.wontakkim.rxscreencapture.sample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.github.wontakkim.rxscreencaputre.RxScreenCapture;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final short GET_MEDIA_PROJECTION_CODE = 986;

    private RxScreenCapture screenCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_screenshot)
    public void onScreenshotClick() {
        if (screenCapture == null) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), GET_MEDIA_PROJECTION_CODE);
            return;
        }

        screenCapture.capture()
                .subscribe(bitmap -> showPreviewDialog(bitmap));
    }

    private void showPreviewDialog(Bitmap bitmap) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        PreviewDialogFragment dialog = PreviewDialogFragment.newInstance(bitmap);
        dialog.show(fragmentManager, "preview");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GET_MEDIA_PROJECTION_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Log.d(TAG, "User cancelled");
                screenCapture = null;
                return;
            }

            screenCapture = new RxScreenCapture(this, resultCode, data);
        }
    }
}
