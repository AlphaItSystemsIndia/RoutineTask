package com.cod3rboy.routinetask.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.cod3rboy.routinetask.activities.MainActivity;
import com.cod3rboy.routinetask.R;

public class AboutFragment extends Fragment {
    private WebView mWebView;

    public static AboutFragment getInstance(int fragmentType){
        Bundle args = new Bundle();
        args.putInt(MainActivity.KEY_FRAGMENT_TYPE, fragmentType);
        AboutFragment fragment = new AboutFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container,false);
        mWebView = root.findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true); // Enable Javascript
        mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setSupportZoom(false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mWebView.loadUrl("file:///android_asset/index.html");
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        int  fragmentType = (getArguments() == null) ? 0 : getArguments().getInt(MainActivity.KEY_FRAGMENT_TYPE);
        ((MainActivity) getActivity()).onSectionAttached(fragmentType);
        ((MainActivity) getActivity()).refreshActionBar();
    }
}
