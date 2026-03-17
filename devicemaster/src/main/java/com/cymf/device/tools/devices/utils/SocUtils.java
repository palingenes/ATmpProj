package com.cymf.device.tools.devices.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.text.TextUtils;

import androidx.core.util.Pair;

import com.blankj.utilcode.util.LogUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by chensongsong on 2020/6/1.
 */
public class SocUtils {

    /**
     * 读取 SOC 型号信息
     */
    public static String getSocInfo() {
        String socStr;
        socStr = CommandUtils.execute("getprop ro.board.platform");
        if (TextUtils.isEmpty(socStr)) {
            socStr = CommandUtils.execute("getprop ro.hardware");
            if (TextUtils.isEmpty(socStr)) {
                socStr = CommandUtils.execute("getprop ro.boot.hardware");
            }
        }
        return socStr;
    }

    public static void setCpuInfo(List<Pair<String, String>> list) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/cpuinfo"));
            String line;
//            CpuBean bean = new CpuBean();
//            HashSet<String> parts = new HashSet<>();
//            HashSet<String> implementer = new HashSet<>();
            while ((line = bufferedReader.readLine()) != null) {
                String result = line.toLowerCase();
                LogUtils.d("CPU: " + result);
                String[] split = result.split(":\\s+", 2);
                if (split.length == 0) continue;
                if (split.length == 2) {
                    list.add(new Pair<>(split[0], split[1]));
                } else {
                    list.add(new Pair<>(split[0], "Undefined"));
                }
//                if (split[0].startsWith("cpu part")) {
//                    parts.add(split[1]);
//                } else if (split[0].startsWith("hardware")) {
//                    bean.setHardware(split[1]);
//                } else if (split[0].startsWith("features")) {
//                    bean.setFeatures(split[1]);
//                } else if (split[0].startsWith("cpu implementer")) {
//                    implementer.add(split[1]);
//                }
            }
            if ( list.isEmpty()) {
                list.add(new Pair<>("cpuinfo", "CpuInfo 文件没有读到或文件为空"));
            }
//            bean.setParts(parts.toArray(new String[0]));
//            bean.setImplementers(implementer.toArray(new String[0]));
//            list.add(new Pair<>("Parts", parts.toString()));
//            list.add(new Pair<>("Implementer", implementer.toString()));
//            list.add(new Pair<>("Hardware", bean.getHardware()));
//            list.add(new Pair<>("Features", bean.getFeatures()));
        } catch (IOException e) {
            list.add(new Pair<>("Exc", e.getMessage()));
        }
    }

    public static String getCoreInfo() {
        String core = FileUtils.readFile("/sys/devices/system/cpu/present");
        if (TextUtils.isEmpty(core)) {
            core = CommandUtils.execute("cat /sys/devices/system/cpu/present");
        }
        if (TextUtils.isEmpty(core)) {
            core = "UNKNOWN";
        }
        return core;
    }

    public static void getGPUInfo(Context context, List<Pair<String, String>> list) {
        ActivityManager am = (ActivityManager) context.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = am.getDeviceConfigurationInfo();
        list.add(new Pair<>("GlEsVersion", info.getGlEsVersion()));
        list.add(new Pair<>("GlEsVersion", info.reqGlEsVersion + ""));
        list.add(new Pair<>("GlEsVersion", info.reqInputFeatures + ""));
        list.add(new Pair<>("GlEsVersion", info.reqKeyboardType + ""));
        list.add(new Pair<>("GlEsVersion", info.reqNavigation + ""));
        list.add(new Pair<>("GlEsVersion", info.reqTouchScreen + ""));
        list.add(new Pair<>("GlEsVersion", info.describeContents() + ""));
    }

}
