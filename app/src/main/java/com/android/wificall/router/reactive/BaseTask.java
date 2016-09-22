package com.android.wificall.router.reactive;

import org.reactivestreams.Subscription;

import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DefaultSubscriber;

/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public abstract class BaseTask<T> {

    private Subscription mSubscription;


    @SuppressWarnings("unchecked")
    public void execute(final DefaultSubscriber<byte[]> subscriber) {
       // unsubscribe();
        Flowable mObservable = createObservable();
        mObservable
                .subscribeOn(Schedulers.io())
                .subscribe(subscriber);
    }

    private Flowable createObservable() {
        return Flowable.create(e -> executeTask((FlowableEmitter<T>) e), FlowableEmitter.BackpressureMode.BUFFER);
    }

    protected abstract void executeTask(FlowableEmitter<T> subscribe);

    @SuppressWarnings("SpellCheckingInspection")
    public void unsubscribe() {
//        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
//            mSubscription.unsubscribe();
//            mSubscription = null;
//        }
    }

}
