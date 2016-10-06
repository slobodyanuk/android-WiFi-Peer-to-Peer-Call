package com.android.wificall.view.activity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.data.event.GroupOwnerEvent;
import com.android.wificall.data.event.UpdateConnection;
import com.android.wificall.data.event.VolumeEvent;
import com.android.wificall.router.audio.AudioSender;
import com.android.wificall.router.audio.OnReceiveAudioListener;
import com.android.wificall.router.audio.OnSendAudioListener;
import com.android.wificall.router.reactive.ReceiveTask;
import com.android.wificall.router.reactive.RecordTask;
import com.android.wificall.util.Globals;
import com.android.wificall.util.MuteUtil;
import com.android.wificall.util.PermissionsUtil;
import com.android.wificall.util.PrefsKeys;
import com.android.wificall.util.RetryExecution;
import com.android.wificall.util.SettingsContentObserver;
import com.android.wificall.util.TimeConstants;
import com.pixplicity.easyprefs.library.Prefs;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.net.DatagramPacket;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.subscribers.DefaultSubscriber;

import static com.android.wificall.router.Configuration.RECORDER_AUDIO_ENCODING;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_IN;
import static com.android.wificall.router.Configuration.RECORDER_CHANNEL_OUT;
import static com.android.wificall.router.Configuration.RECORDER_RATE;

public class CallActivity extends BaseActivity implements RetryExecution {

    private static final String TAG = CallActivity.class.getCanonicalName();

    private static final int MIN_BUFFER_SIZE = 2048;
    private static int RECORD_BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_IN, RECORDER_AUDIO_ENCODING);
    private static int RECEIVE_BUFFER_SIZE = AudioTrack.getMinBufferSize(RECORDER_RATE, RECORDER_CHANNEL_OUT, RECORDER_AUDIO_ENCODING);

    private boolean isGroupOwner = Prefs.getBoolean(PrefsKeys.IS_SPEAKER, false);

    @BindView(R.id.call_msg)
    TextView mCallMessage;
    @BindView(R.id.update)
    ImageButton mUpdateButton;
    @BindView(R.id.btn_volume)
    ImageButton mVolumeButton;
    @BindView(R.id.btn_mute)
    ImageButton mMuteButton;
    @BindView(R.id.seekBar)
    SeekBar mSeekBar;
    @BindView(R.id.call_container)
    LinearLayout mCallButtonLayout;
    @BindView(R.id.micro_container)
    LinearLayout mMicroButtonLayout;
    @BindView(R.id.conference)
    ImageView mConversationLogo;

    private RecordTask mRecordTask;
    private ReceiveTask mReceiveTask;

    private boolean stopThread = false;
    private boolean isUpdating;
    private boolean isMute = false;
    private boolean isVolumeClicked = false;

    private MuteUtil mMuteUtil;
    private OnSendAudioListener mSendCallback;

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener;
    private final Handler mHandler = new Handler();
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isUpdating) {
                onUpdateClick();
                mHandler.postDelayed(mUpdateRunnable, TimeConstants.THRITY_SECONDS);
            }
        }
    };
    private AudioManager mAudioManager;
    private SettingsContentObserver mSettingsContentObserver;
    private boolean isRecording = false;

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        if (isGroupOwner && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && PermissionsUtil.needRecordAudioPermissions(this)) {
            PermissionsUtil.requestRecordAudioPermission(this);
        }
        if (isGroupOwner) {
            if (!isRecording) startRecording();
        } else {
            mSettingsContentObserver = new SettingsContentObserver(this, new Handler());
            getApplicationContext()
                    .getContentResolver()
                    .registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, mSettingsContentObserver);
            initReceivingThread();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isUpdating = true;
        mHandler.postDelayed(mUpdateRunnable, TimeConstants.SECOND);

        if (RECEIVE_BUFFER_SIZE < MIN_BUFFER_SIZE) {
            RECEIVE_BUFFER_SIZE = MIN_BUFFER_SIZE;
        }
        mMuteUtil = new MuteUtil(this, isGroupOwner);
        mUpdateButton.setVisibility(View.VISIBLE);

        if (!isGroupOwner) {
            mVolumeButton.setVisibility(View.VISIBLE);
            mCallMessage.setText(getString(R.string.receive_msg));
            mMuteButton.setImageResource(R.drawable.ic_allow_voice);
            mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };
        } else {
            mCallMessage.setText(getString(R.string.record_msg));
            mVolumeButton.setVisibility(View.GONE);
            mMuteButton.setImageResource(R.drawable.ic_microphone);
        }

    }

    @Subscribe
    public void onEvent(GroupOwnerEvent event) {
        isGroupOwner = event.isGroupOwner();
        Log.e("TAG", "onEvent: isGroupOwner : " + isGroupOwner);
    }

    @Subscribe
    public void onEvent(VolumeEvent event) {
        if (!isGroupOwner && mSeekBar != null && mSeekBar.isEnabled()) {
            mSeekBar.setProgress(event.getVolume());
        }
    }

    @Subscribe
    public void onEvent(UpdateConnection event) {
        onUpdateClick();
    }

    private void startRecording() {
        isRecording = true;
        mRecordTask = new RecordTask(this, RECORD_BUFFER_SIZE);
        mSendCallback = new AudioSender();
        mRecordTask.execute(new DefaultSubscriber<byte[]>() {
            @Override
            public void onNext(byte[] bytes) {
                if (!isMute) {
                    mSendCallback.onSendAudioData(bytes);
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.d(TAG, "onUpdateSubscriber: " + t);
            }

            @Override
            public void onComplete() {
                Log.e(TAG, "onComplete: ");
                mSendCallback.onCompleted();
            }
        });

        stopThread = false;
    }

    private void initReceivingThread() {
        if (!isGroupOwner) {
            mReceiveTask = new ReceiveTask(RECEIVE_BUFFER_SIZE, this);
            OnReceiveAudioListener mReceiveAudioListener = mReceiveTask.initAudioReader();
            mReceiveTask.execute(new DefaultSubscriber<DatagramPacket>() {
                @Override
                public void onNext(DatagramPacket packet) {
                    if (!isMute) {
                        mReceiveAudioListener.onReceiveData(packet.getData(), packet.getLength());
                    }
                }

                @Override
                public void onError(Throwable t) {
                    Log.e(TAG, "onUpdateSubscriber: " + t);
                }

                @Override
                public void onComplete() {
                    Log.d(TAG, "onComplete: receive");
                }
            });
        }
    }

    @OnClick(R.id.update)
    public void onUpdateClick() {
        if (!isGroupOwner) {
            if (mReceiveTask != null) {
                mReceiveTask.updateReceiver();
            }
        } else {
            if (mRecordTask != null) {
                mRecordTask.updateRecorder();
                mSendCallback.onUpdateConnection();
            }
        }
    }

    @OnClick(R.id.btn_mute)
    public void onMuteClick() {
        isMute = !isMute;
        mMuteButton.setImageResource(mMuteUtil.initMute(isMute));
        mConversationLogo.setImageResource(mMuteUtil.initConversationLogo(mCallMessage, mUpdateButton));
    }

    @OnClick(R.id.btn_volume)
    public void onVolumeClick() {
        isVolumeClicked = !isVolumeClicked;
        if (isVolumeClicked) {
            mCallButtonLayout.setVisibility(View.GONE);
            mMicroButtonLayout.setVisibility(View.GONE);
            mSeekBar.setVisibility(View.VISIBLE);
            int volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mSeekBar.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            mSeekBar.setProgress(volume);
            mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        } else {
            mCallButtonLayout.setVisibility(View.VISIBLE);
            mMicroButtonLayout.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.GONE);
        }
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
            isRecording = false;
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
                if (!isRecording) startRecording();
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
        Log.e(TAG, "onPause");
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!isGroupOwner) {
            getApplicationContext()
                    .getContentResolver()
                    .unregisterContentObserver(mSettingsContentObserver);
        }
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

    @OnClick(R.id.btn_hang_up)
    public void hangUp() {
        finish();
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

    @Override
    public void onRetryExecution() {
        if (isGroupOwner) {
            startRecording();
        } else {
            initReceivingThread();
        }
    }

    @Override
    public void onBackPressed() {
        if (!isGroupOwner) {
            if (isVolumeClicked) {
                onVolumeClick();
                return;
            }
        }
        super.onBackPressed();
    }
}