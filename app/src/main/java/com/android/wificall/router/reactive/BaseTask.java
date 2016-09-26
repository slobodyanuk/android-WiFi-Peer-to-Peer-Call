package com.android.wificall.router.reactive;

import android.util.Log;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;

/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public abstract class BaseTask<T> {

    @SuppressWarnings("unchecked")
    public void execute(final DefaultSubscriber<T> subscriber) {

        Flowable mObservable = createObservable();
        mObservable
                .subscribeOn(Schedulers.io())
                .subscribe(subscriber);

    }

    private Flowable createObservable() {
        Log.e("create", "return");
        return Flowable.create(e -> executeTask((FlowableEmitter<T>) e), FlowableEmitter.BackpressureMode.BUFFER);
    }

    protected abstract void executeTask(FlowableEmitter<T> subscribe);

}
