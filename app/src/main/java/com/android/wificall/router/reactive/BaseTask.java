package com.android.wificall.router.reactive;

import java.util.concurrent.Executor;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.ReplaySubject;

/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public abstract class BaseTask<T> {

    private Subscription mSubscription;

    @SuppressWarnings("unchecked")
    public void execute(final Subscriber<T> subscriber) {
        unsubscribe();

        Observable mObservable = createObservable();
//        Observable mObservable = ReplaySubject.create(this::executeTask);
        mSubscription = mObservable
                .subscribeOn(Schedulers.io())
                .subscribe(subscriber);
    }

    private Observable createObservable() {
        return Observable.create((Observable.OnSubscribe<Object>) this::executeTask);
    }

    protected abstract void executeTask(Subscriber<? super T> subscriber);

    @SuppressWarnings("SpellCheckingInspection")
    public void unsubscribe() {
        if (mSubscription != null && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
    }

}
