package com.android.wificall.router.reactive;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;
import rx.subjects.Subject;

/**
 * Created by Serhii Slobodyanuk on 01.09.2016.
 */
public abstract class BaseTask<T> {

    private Subscription mSubscription;

    @SuppressWarnings("unchecked")
    public void execute(final Subscriber<T> subscriber) {
        unsubscribe();
        Observable mObservable = createObservable();
        mSubscription = mObservable
                .subscribeOn(Schedulers.newThread())
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
            mSubscription = null;
        }
    }

}
