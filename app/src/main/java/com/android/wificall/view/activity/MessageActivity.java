package com.android.wificall.view.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.wificall.R;
import com.android.wificall.data.Client;
import com.android.wificall.data.Packet;
import com.android.wificall.data.event.MessageEvent;
import com.android.wificall.router.NetworkManager;
import com.android.wificall.router.Sender;
import com.android.wificall.router.broadcast.WifiDirectBroadcastReceiver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;


public class MessageActivity extends BaseActivity {

	private static TextView messageView;

	@BindView(R.id.edit_message)
	EditText mEditMessage;

	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		messageView = (TextView) findViewById(R.id.message_view);

		final Button button = (Button) findViewById(R.id.btn_send);
		final EditText message = (EditText) findViewById(R.id.edit_message);

		this.setTitle("Group Chat");

	}

	@OnClick(R.id.btn_send)
	public void onSendClick(){
		String msgStr = mEditMessage.getText().toString();
		addMessage("This phone", msgStr);
		mEditMessage.setText("");

		// Send to other clients as a group chat message
		for (Client c : NetworkManager.routingTable.values()) {
			if (c.getMac().equals(NetworkManager.getSelf().getMac()))
				continue;
			Sender.queuePacket(new Packet(Packet.TYPE.MESSAGE, msgStr.getBytes(), c.getMac(),
					WifiDirectBroadcastReceiver.MAC));
		}
	}

	@Subscribe(threadMode = ThreadMode.MAIN)
	public void onEventMainThread(MessageEvent event){
		addMessage(event.getName(), event.getMsg());
	}

	@Override
	protected int getLayoutResource() {
		return R.layout.message;
	}

	public static void addMessage(String from, String text) {
		if (messageView != null && text != null) {
			messageView.append(from + " says " + text + "\n");
			final int scrollAmount = messageView.getLayout().getLineTop(messageView.getLineCount())
					- messageView.getHeight();
			// if there is no need to scroll, scrollAmount will be <=0
			if (scrollAmount > 0)
				messageView.scrollTo(0, scrollAmount);
			else
				messageView.scrollTo(0, 0);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		EventBus.getDefault().unregister(this);
	}
}
