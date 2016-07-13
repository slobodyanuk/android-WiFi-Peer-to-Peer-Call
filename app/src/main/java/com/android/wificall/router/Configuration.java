package com.android.wificall.router;

import android.media.AudioFormat;

public class Configuration {

	public static final int RECEIVE_PORT = 8888;
	public static final int RECEIVE_VOICE_PORT = 8080;

	public static final String GO_IP = "192.168.49.1";

	public static final int RECORDER_RATE = 8000;
	public static final int RECORDER_CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
	public static final int RECORDER_CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
	public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	public static boolean isDeviceBridgingEnabled = true;
}
