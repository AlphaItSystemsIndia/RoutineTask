package com.cod3rboy.routinetask.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import com.cod3rboy.routinetask.logging.Logger;

/**
 * This is abstract fragment class with inbuilt support for RecyclerView.
 */
public abstract class RecyclerViewFragment extends Fragment {

    private static final String LOG_TAG = RecyclerViewFragment.class.getSimpleName();

    // RecyclerView hold by fragment
    private RecyclerView mRecyclerView;
    // EmptyView hold by fragment
    private View mEmptyView;
    // Data set Observer for adapter
    private RecyclerView.AdapterDataObserver observer;

    /**
     * Abstract method to be implemented by subclass to provide Resource ID for fragment layout.
     *
     * @return int resource id for fragment layout
     */
    public abstract int getLayoutResourceId();

    /**
     * Abstract method to be implemented by subclass to provide Resource ID for RecyclerView contained in fragment layout
     *
     * @return int resource id for RecyclerView contained in fragment layout
     */
    public abstract int getRecyclerViewId();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * This method is called to inflate the fragment layout.
     *
     * @param inflater           LayoutInflater
     * @param container          Parent ViewGroup
     * @param savedInstanceState Bundle
     * @return Inflated layout for the fragment
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResourceId(), container, false);
        // Obtain the RecyclerView from fragment layout
        mRecyclerView = view.findViewById(getRecyclerViewId());
        observer = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                // update Fragment layout when data items changed in adapter ( i.e. when adapter called notifyDataSetChanged())
                updateLayout();
            }
        };
        return view;
    }

    /**
     * This method helps to update the fragment layout by toggling visibility of RecyclerView
     * and EmptyList View. This method is useful to toggle between RecyclerView and EmptyView based
     * on the items in the adapter. If adapter is empty then RecyclerView is shown and EmptyView is hidden
     * and vice-versa.
     */
    private void updateLayout() {
        RecyclerView.Adapter<?> adapter = getAdapter();
        if (adapter != null && mEmptyView != null) {
            if (adapter.getItemCount() > 0) {
                Logger.d(LOG_TAG, "updateLayout() - Show RecyclerView Layout");
                mRecyclerView.setVisibility(View.VISIBLE);
                mEmptyView.setVisibility(View.GONE);
            } else {
                mRecyclerView.setVisibility(View.GONE);
                mEmptyView.setVisibility(View.VISIBLE);
                Logger.d(LOG_TAG, "updateLayout() - Show EmptyView Layout");
            }
        }
    }

    protected void fadeInRecyclerView() {
        mRecyclerView.setAlpha(0f);
        // Animate recycler view
        mRecyclerView.animate()
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .alpha(1f)
                .withEndAction(() -> mRecyclerView.setAlpha(1f));
    }

    protected void fadeInEmptyView() {
        mEmptyView.setAlpha(0f);
        // Animate empty view
        mEmptyView.animate()
                .setDuration(100)
                .setInterpolator(new DecelerateInterpolator())
                .alpha(1f)
                .withEndAction(() -> mEmptyView.setAlpha(1f));
    }

    /**
     * This Method sets adapter for RecyclerView and also register DataObserver on adapter.
     *
     * @param adapter instance of RecyclerView.Adapter
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (adapter == null) return;
        // Set adapter
        mRecyclerView.setAdapter(adapter);
        // Register DataObserver
        adapter.registerAdapterDataObserver(observer);
        // Immediately update fragment layout
        updateLayout();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        RecyclerView.Adapter adapter = getAdapter();
        if (adapter != null) adapter.unregisterAdapterDataObserver(observer);
    }

    /**
     * Getter for the Adapter
     *
     * @return adapter
     */
    public RecyclerView.Adapter getAdapter() {
        return mRecyclerView.getAdapter();
    }

    /**
     * Set the layout manager which will arrange list items accordingly.
     *
     * @param manager RecyclerView.LayoutManager instance
     */
    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.getRecycledViewPool().clear();
    }

    /**
     * This method sets the empty view to display when there is no item in the adapter.
     * Note that this empty view must be inside the fragment's view hierarchy.
     *
     * @param v inflated empty view
     */
    public void setEmptyView(View v) {
        this.mEmptyView = v;
    }

    /**
     * Getter for the RecyclerView
     *
     * @return recycler view
     */
    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }
}
