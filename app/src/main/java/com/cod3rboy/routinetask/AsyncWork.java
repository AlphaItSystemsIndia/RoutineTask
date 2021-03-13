package com.cod3rboy.routinetask;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncWork<AT,RT>{
    private static final String LOG_TAG = AsyncWork.class.getSimpleName();
    private ExecutorService mExecutionService;
    public interface OnLoaderCompleted<RT>{
        void complete(RT data);
    }
    public interface Work<AT,RT>{
        RT execute(AT... args);
    }
    private OnLoaderCompleted<RT> mOnLoaderCompleted;
    private Work<AT,RT> mWork;

    public AsyncWork(Work<AT,RT> work) {
        mOnLoaderCompleted = null;
        mExecutionService = Executors.newCachedThreadPool();
        mWork = work;
    }

    public void registerOnLoadComplete(OnLoaderCompleted<RT> mCallback){
        mOnLoaderCompleted = mCallback;
    }

    private void doInBackground(AT ...args) {
        mExecutionService.execute(new Runnable() {
            @Override
            public void run() {
                RT result = mWork.execute(args);
                postResults(result);
                mExecutionService.shutdown();
                mExecutionService = null;
            }
        });
    }

    public void start(AT ...args){
        doInBackground(args);
    }

    private void postResults(RT data){
        new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if(mOnLoaderCompleted != null) mOnLoaderCompleted.complete(data);
                return true;
            }
        }).sendMessage(Message.obtain());
    }
}
