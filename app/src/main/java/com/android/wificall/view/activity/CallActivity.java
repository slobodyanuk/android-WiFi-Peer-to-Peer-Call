package com.android.wificall.view.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.data.event.GroupOwnerEvent;
import com.android.wificall.router.audio.AudioSender;
import com.android.wificall.router.audio.OnSendAudioListener;
import com.android.wificall.router.reactive.ReceiveTask;
import com.android.wificall.router.reactive.RecordTask;
import com.android.wificall.util.Globals;
import com.android.wificall.util.PermissionsUtil;
import com.android.wificall.util.TimeConstants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.subscribers.DefaultSubscriber;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

public class CallActivity extends BaseActivity {

    private static final String TAG = CallActivity.class.getCanonicalName();

    private static final int MIN_BUFFER_SIZE = 2048;
    private static int RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_IN, RECORDER_AUDIO_ENCODING);
    private static int RECEIVE_BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);

    private boolean isGroupOwner;

    @BindView(R.id.call_msg)
    TextView mCallMessage;
    @BindView(R.id.update)
    Button mUpdateButton;

    private RecordTask mRecordTask;
    private ReceiveTask mReceiveTask;
    private AudioTrack mAudioTrack = null;

    private boolean stopThread = false;
    private boolean isUpdating;

    private final Handler mHandler = new Handler();
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdating && !isGroupOwner) {
                onUpdateClick();
                mHandler.postDelayed(mUpdateRunnable, TimeConstants.THRITY_SECONDS);
            }
        }
    };
    private OnSendAudioListener mSendCallback;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isGroupOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && PermissionsUtil.needRecordAudioPermissions(this)) {
            PermissionsUtil.requestRecordAudioPermission(this);
            startRecording();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("group_owner", WifiDirectActivity.isGroupOwner);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isGroupOwner = savedInstanceState.getBoolean("group_owner");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isGroupOwner = savedInstanceState.getBoolean("group_owner");
        } else {
            isGroupOwner = WifiDirectActivity.isGroupOwner;
        }

        isUpdating = true;
        mHandler.postDelayed(mUpdateRunnable, TimeConstants.SECOND);

        if (RECEIVE_BUFFER_SIZE < MIN_BUFFER_SIZE) {
            RECEIVE_BUFFER_SIZE = MIN_BUFFER_SIZE;
        }

        if (!isGroupOwner) {
            mCallMessage.setText(getString(R.string.receive_msg));
            mUpdateButton.setVisibility(View.VISIBLE);
        } else {
            mCallMessage.setText(getString(R.string.record_msg));
            startRecording();
            mUpdateButton.setVisibility(View.GONE);
        }

        initReceivingThread();
    }

    @Subscribe
    public void onEvent(GroupOwnerEvent event) {
        isGroupOwner = event.isGroupOwner();
        Log.e("TAG", "onEvent: isGroupOwner : " + isGroupOwner);

    }

    private void initReceivingThread() {
        if (!isGroupOwner) {
            mReceiveTask = new ReceiveTask(RECEIVE_BUFFER_SIZE, this);
            mReceiveTask.execute(new DefaultSubscriber<byte[]>() {
                @Override
                public void onNext(byte[] bytes) {
                    Log.e(TAG, "onNext: " + bytes);
                }

                @Override
                public void onError(Throwable t) {
                    Log.d(TAG, "onError: " + t);
                }

                @Override
                public void onComplete() {
                    Log.d(TAG, "onComplete: ");
                }
            });
        }
    }

    @OnClick(R.id.update)
    public void onUpdateClick() {
        if (mReceiveTask != null) {
//            stopReceiving();
//            mReceiveTask.updateReceiver();
//            initReceivingThread();
        }
    }

    private void startRecording() {
        mRecordTask = new RecordTask(this, RECORD_BUFFER_SIZE);
        mSendCallback = new AudioSender();
        mRecordTask.execute(new DefaultSubscriber<byte[]>() {
            @Override
            public void onNext(byte[] bytes) {
                Log.e(TAG, "onNext: " + bytes);
                mSendCallback.onSendAudioData(bytes);
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, "onError: " + t);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
                mSendCallback.onCompleted();
            }
        });

        stopThread = false;
    }

    public boolean isThreadStopped() {
        return stopThread;
    }

    public void setThreadStopped(boolean stop) {
        stopThread = stop;
    }

    private void stopRecording() {
        if (mRecordTask != null) {
            mRecordTask.stop();
            stopThread = true;
        }
    }

    private void stopReceiving() {
        if (mReceiveTask != null) {
            Log.e(TAG, "stopReceiving");
            mReceiveTask.stop();
            stopThread = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Globals.REQUEST_RECORD_AUDIO) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startRecording();
            } else {
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        boolean screenOn;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            screenOn = pm.isInteractive();
        } else {
            screenOn = pm.isScreenOn();
        }

        if (screenOn) {
            stopUpdating();
            stopReceiving();
            stopRecording();
        }
    }

    private void stopUpdating() {
        isUpdating = false;
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.call;
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}