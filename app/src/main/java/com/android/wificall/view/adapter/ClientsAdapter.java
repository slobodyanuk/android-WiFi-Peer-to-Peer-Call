package com.android.wificall.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.wificall.R;

import butterknife.ButterKnife;

/**
 * Created by Serhii Slobodyanuk on 05.10.2016.
 */
public class ClientsAdapter extends BaseAdapter {

    private Context context;
    private String[] values;

    public ClientsAdapter(Context context, String[] values) {
        this.context = context;
        this.values = values;
    }

    @Override
    public int getCount() {
        return values.length;
    }

    @Override
    public String getItem(int i) {
        return values[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder holder;
        View rowView = convertView;
        if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.row_clients, parent, false);
            holder = new ViewHolder();
            holder.name = ButterKnife.findById(rowView, R.id.client);
            rowView.setTag(holder);
        }else{
            holder = (ViewHolder) rowView.getTag();
        }

        holder.name.setText(values[position]);

        return rowView;
    }

    public void updateValues(String[] values){
        this.values = values;
        notifyDataSetChanged();
    }

    static class ViewHolder {
        public TextView name;
    }
}
