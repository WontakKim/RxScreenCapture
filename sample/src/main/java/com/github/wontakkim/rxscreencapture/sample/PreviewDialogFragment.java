package com.github.wontakkim.rxscreencapture.sample;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class PreviewDialogFragment extends DialogFragment {

    private static String KEY_PREVIEW = "preview";

    @BindView(R.id.iv_preview) ImageView imageView;

    private Unbinder unbinder;

    public static PreviewDialogFragment newInstance(Bitmap bitmap) {
        PreviewDialogFragment fragment = new PreviewDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_PREVIEW, bitmap);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_preview, container, false);
        unbinder = ButterKnife.bind(this, view);

        Bitmap bitmap = getArguments().getParcelable(KEY_PREVIEW);
        imageView.setImageBitmap(bitmap);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
