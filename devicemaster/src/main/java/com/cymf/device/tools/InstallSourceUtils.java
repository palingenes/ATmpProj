package com.cymf.device.tools;

import android.content.Context;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * 获取应用安装来源
 */
public class InstallSourceUtils {

    public static String getInstallSource(Context context, String packageName) throws PackageManager.NameNotFoundException {
        PackageManager packageManager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            InstallSourceInfo installSourceInfo = packageManager.getInstallSourceInfo(packageName);
            if (installSourceInfo != null) {
                // 获取发起安装的应用包名
                String installingPackageName = installSourceInfo.getInstallingPackageName();
                // 获取安装器的包名 (例如 Google Play Store, 应用商店等)
                String installerPackageName = packageManager.getInstallerPackageName(packageName);

                return "Installing Package: " + installingPackageName + ", Installer Package: " + installerPackageName;
            }
        } else {
            // 在 Android R (API 30) 之前，只能获取安装器包名
            return "Installer Package: " + packageManager.getInstallerPackageName(packageName);
        }
        return "获取安装来源失败";
    }
}