package com.cymf.device.tools.devices.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by chensongsong on 2020/7/15.
 */
public class RootUtils {

    /**
     * 判断是否root
     *
     * @param context
     * @return
     */

    public  static String isRoot(Context context) {
        return (existingRWPaths().size() > 0 || existingDangerousProperties().size() > 0 || existingRootFiles().size() > 0 || existingRootPackages(context).size() > 0) ? "1" : "0";
    }


    private static final String[] SU_PATHS = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"};
    private static final String[] KNOWN_ROOT_APPS_PACKAGES = {
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.yellowes.su"};
    private static final String[] KNOWN_DANGEROUS_APPS_PACKAGES = {
            "com.koushikdutta.rommanager",
            "com.dimonvideo.luckypatcher",
            "com.chelpus.lackypatch",
            "com.ramdroid.appquarantine"};
    private static final String[] KNOWN_ROOT_CLOAKING_PACKAGES = {
            "com.devadvance.rootcloak",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.devadvance.rootcloakplus",
            "com.zachspong.temprootremovejb",
            "com.amphoras.hidemyroot",
            "com.formyhm.hideroot"};
    private static final String[] PATHS_THAT_SHOULD_NOT_BE_WRITABLE = {
            "/system",
            "/system/bin",
            "/system/sbin",
            "/system/xbin",
            "/vendor/bin",
            "/sbin",
            "/etc"};
    private static final Map<String, String> DANGEROUS_PROPERTIES = new HashMap<>();

    /**
     * 检查已知指示 root 权限的文件。
     *
     * @return - 找到的此类文件列表
     */
    public static List<String> existingRootFiles() {
        List<String> filesFound = new ArrayList<>();
        for (String path : SU_PATHS) {
            if (new File(path).exists()) {
                filesFound.add(path);
            }
        }
        return filesFound;
    }

    /**
     * 检查已知指示 root 权限的软件包。
     *
     * @return - 找到的此类软件包列表
     */
    public static List<String> existingRootPackages(Context context) {
        ArrayList<String> packages = new ArrayList<>();
        packages.addAll(Arrays.asList(KNOWN_ROOT_APPS_PACKAGES));
        packages.addAll(Arrays.asList(KNOWN_DANGEROUS_APPS_PACKAGES));
        packages.addAll(Arrays.asList(KNOWN_ROOT_CLOAKING_PACKAGES));

        PackageManager pm = context.getPackageManager();
        List<String> packagesFound = new ArrayList<>();

        for (String packageName : packages) {
            try {
                // Root app detected
                pm.getPackageInfo(packageName, 0);
                packagesFound.add(packageName);
            } catch (PackageManager.NameNotFoundException e) {
                // Exception thrown, package is not installed into the system
            }
        }
        return packagesFound;
    }

    /**
     * 检查系统属性中是否存在任何指示 root 身份的危险属性。
     *
     * @return - 指示 root 身份的危险属性列表
     */
    public static List<String> existingDangerousProperties() {
        DANGEROUS_PROPERTIES.put("[ro.debuggable]", "[1]");
        DANGEROUS_PROPERTIES.put("[ro.secure]", "[0]");
        String[] lines = propertiesReader();
        List<String> propertiesFound = new ArrayList<>();
        assert lines != null;
        for (String line : lines) {
            for (String key : DANGEROUS_PROPERTIES.keySet()) {
                if (line.contains(key) && line.contains(DANGEROUS_PROPERTIES.get(key))) {
                    propertiesFound.add(line);
                }
            }
        }
        return propertiesFound;
    }

    /**
     * 当您以 root 身份登录时，您可以更改常见系统目录的写入权限。
     * 此方法检查 PATHS_THAT_SHOULD_NOT_BE_WRITABLE 中的任何路径是否可写。
     *
     * @return 所有可写的路径
     */
    /**
     * 检查系统中指定路径是否以可读写（"rw"）方式挂载。
     * 返回一个包含所有被发现以 "rw" 方式挂载的敏感路径列表。
     *
     * @return List<String> 包含所有本应只读却被挂载为可读写的路径
     */
    public static List<String> existingRWPaths() {
        // 1. 调用 mountReader() 方法获取系统的挂载信息（通常是读取 /proc/mounts 或 mount 命令输出）
        String[] lines = mountReader();

        // 2. 创建一个列表用于保存发现的、错误地设置为可写（rw）的路径
        List<String> pathsFound = new ArrayList<>();

        // 3. 非空断言，防止后续空指针异常
        assert lines != null;

        // 4. 遍历每一行挂载信息
        for (String line : lines) {
            // 5. 将每一行按空格分割成多个字段（格式通常为：设备 挂载点 文件系统类型 选项 dump pass）
            String[] args = line.split(" ");

            // 6. 如果字段数量不足 4 个（即缺少挂载点或挂载选项），跳过该行
            if (args.length < 4) {
                continue;
            }

            // 7. 获取挂载点路径和挂载选项
            String mountPoint = args[1];     // 第二个字段是挂载点路径
            String mountOptions = args[3];   // 第四个字段是挂载选项（如 rw, ro）

            // 8. 遍历预定义的“不应该被写入”的路径数组（例如 PATHS_THAT_SHOULD_NOT_BE_WRITABLE）
            for (String pathToCheck : PATHS_THAT_SHOULD_NOT_BE_WRITABLE) {
                // 9. 如果当前挂载点与目标路径匹配（忽略大小写）
                if (mountPoint.equalsIgnoreCase(pathToCheck)) {

                    // 10. 将挂载选项按逗号分割（如 "rw,noatime" 分割为 ["rw", "noatime"]）
                    for (String option : mountOptions.split(",")) {

                        // 11. 如果存在 "rw"（可读写）选项，则认为该路径被错误配置
                        if ("rw".equalsIgnoreCase(option)) {
                            // 12. 添加到结果列表中，并跳出循环
                            pathsFound.add(pathToCheck);
                            break;
                        }
                    }
                }
            }
        }

        // 13. 返回最终发现的、被错误挂载为可写的敏感路径列表
        return pathsFound;
    }

    /**
     * Used for existingDangerousProperties().
     *
     * @return - list of system properties
     */
    private static String[] propertiesReader() {
        InputStream inputstream = null;
        try {
            inputstream = Runtime.getRuntime().exec("getprop").getInputStream();
        } catch (IOException e) {
            //忽略异常
        }
        if (inputstream == null) {
            return null;
        }

        String allProperties = "";
        try {
            allProperties = new Scanner(inputstream).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
            //忽略异常
        }
        return allProperties.split("\n");
    }

    /**
     * 用于 existingRWPaths()。
     *
     * @return - 目录及其属性列表
     */
    private static String[] mountReader() {
        InputStream inputstream = null;
        try {
            inputstream = Runtime.getRuntime().exec("mount").getInputStream();
        } catch (IOException e) {
            //忽略异常
        }
        if (inputstream == null) {
            return null;
        }

        String allPaths = "";
        try {
            allPaths = new Scanner(inputstream).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
            //忽略异常
        }
        return allPaths.split("\n");
    }

}
