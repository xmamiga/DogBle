package com.cchip.maddogbt.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.cchip.maddogbt.R;
import com.cchip.maddogbt.bean.DeviceScanBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 2018/10/17-11:41
 * Email: xmamiga@qq.com
 * Author: xmamiga
 * Description: TODO
 */
public class BleDeviceAdapter extends BaseAdapter {
    private final static String TAG = "BleDeviceAdapter";
    private List<DeviceScanBean> lists = new ArrayList<DeviceScanBean>();
    private Context mContext;
    private View view;

    public BleDeviceAdapter(Context context) {
        mContext = context;
    }

    public void setDataChange(ArrayList<DeviceScanBean> list) {
        this.lists.clear();
        this.lists.addAll(list);
        notifyDataSetChanged();
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mHolder;
        view = convertView;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.item_device,
                    null);
            mHolder = new ViewHolder();
            mHolder.tvName = findView(R.id.tv_name);
            mHolder.tvMac = findView(R.id.tv_mac);
            mHolder.tvRssi = findView(R.id.tv_rssi);
            mHolder.imgLevel = findView(R.id.img_level);
            view.setTag(mHolder);
        } else {
            mHolder = (ViewHolder) view.getTag();
        }
        final DeviceScanBean scanResult = lists.get(position);
        mHolder.tvName.setText(scanResult.getDeviceName());
        mHolder.tvMac.setText(scanResult.getMacAddress());
        int rssi = scanResult.getRssi();
        mHolder.tvRssi.setText(rssi + "db");
        if (rssi <= 0 && rssi > -25) {
            mHolder.imgLevel.setImageResource(R.drawable.ic_level_five);
        } else if (rssi <= -25 && rssi > -50) {
            mHolder.imgLevel.setImageResource(R.drawable.ic_level_four);
        } else if (rssi <= -50 && rssi > -75) {
            mHolder.imgLevel.setImageResource(R.drawable.ic_level_three);
        } else if (rssi <= -75 && rssi > -100) {
            mHolder.imgLevel.setImageResource(R.drawable.ic_level_two);
        } else {
            mHolder.imgLevel.setImageResource(R.drawable.ic_level_one);
        }
        return view;
    }

    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int position) {
        return lists.get(position).getMacAddress();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class ViewHolder {
        ImageView imgLevel;
        TextView tvName;
        TextView tvMac;
        TextView tvRssi;
    }

    private <T extends View> T findView(int id) {
        return (T) view.findViewById(id);
    }
}
