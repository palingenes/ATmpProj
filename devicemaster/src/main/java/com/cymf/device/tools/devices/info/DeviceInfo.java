package com.cymf.device.tools.devices.info;

import android.content.Context;

import androidx.core.util.Pair;


import com.cymf.device.tools.devices.utils.DeviceUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chensongsong on 2020/8/3.
 */
public class DeviceInfo {

    public static List<Pair<String, String>> getDeviceInfo(Context context) {
        List<Pair<String, String>> list = new ArrayList<>();
        list.add(new Pair<>("AndroidId", DeviceUtils.getAndroidId(context)));
        list.add(new Pair<>("IMEI", DeviceUtils.getIMEI(context)));
        DeviceUtils.getDeviceInfo(context,list);
        list.add(new Pair<>("ICCID", DeviceUtils.getIccId(context)));
        DeviceUtils.getSimInfo(context,list);
        DeviceUtils.getOtherInfo(context,list);
        return list;
    }

}
