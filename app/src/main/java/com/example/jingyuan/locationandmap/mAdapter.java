package com.example.jingyuan.locationandmap;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jingyuan on 11/4/17.
 */

public class mAdapter extends BaseAdapter {

    private Activity activity;
    private LayoutInflater inflater;
    private List<CheckPoint> list;

    public mAdapter(Activity activity, List<CheckPoint> Items) {
        this.activity = activity;
        this.list = Items;
    }

    @Override
    public int getCount() {

        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (inflater == null)
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (view == null)
            view = inflater.inflate(R.layout.listitem, null);

        TextView coordination = (TextView) view.findViewById(R.id.lv_location);
        TextView time = (TextView) view.findViewById(R.id.lv_time);
        TextView addr = (TextView) view.findViewById(R.id.lv_address);
        final CheckPoint listitem = list.get(i);

        coordination.setText(listitem.getLat() + ", " + listitem.getLng());
        time.setText(listitem.getTime());
        addr.setText(listitem.getAddress());

        return view;
    }
}
