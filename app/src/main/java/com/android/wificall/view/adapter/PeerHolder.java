package com.android.wificall.view.adapter;

import android.graphics.Color;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.wificall.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PeerHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.root)
    RelativeLayout root;
    @BindView(R.id.tv_name)
    TextView name;
    @BindView(R.id.tv_mac)
    TextView tvMac;
    @BindView(R.id.tv_device_address)
    TextView tvAddress;
    @BindView(R.id.btn_connect)
    AppCompatButton btnConnect;

    private String defaultColor = "#ffffff";
    private String selectedColor = "#5b9c33";

    public PeerHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }

    public void highlight() {
        root.setBackgroundColor(Color.parseColor(selectedColor));
    }

    public void unhighlight() {
        root.setBackgroundColor(Color.parseColor(defaultColor));
    }

}
