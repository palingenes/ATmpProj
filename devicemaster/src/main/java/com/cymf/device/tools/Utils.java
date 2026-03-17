package com.cymf.device.tools;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

public class Utils {
    /**
     * 检测设备Root状态
     */
    public static boolean isRootAvailable() {
        String[] paths = {"/system/bin/su", "/system/xbin/su", "/sbin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    /**
     * 验证SU可执行性*****************************
     */
    public static boolean canExecuteRootCommands() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            OutputStream out = process.getOutputStream();
            InputStream in = process.getInputStream();
            out.write("exit\n".getBytes());
            out.flush();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 标准权限请求模式
     */
    public static void runAsRoot(String[] commands) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            for (String cmd : commands) {
                os.writeBytes(cmd + "\n");
            }

            os.writeBytes("exit\n");
            os.flush();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                // 处理错误流
                BufferedReader err = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = err.readLine()) != null) {
                    Log.e("ROOT_ERR", line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 带超时控制的执行方案
     */
    private static class RootTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... commands) {
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());

                for (String cmd : commands) {
                    os.writeBytes(cmd + "\n");
                }

                os.writeBytes("exit\n");
                os.flush();

                // 设置30秒超时
                boolean finished = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    finished = process.waitFor(30, TimeUnit.SECONDS);
                }
                return finished ? process.exitValue() : -999;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    // 典型高危操作示例（需严格审查）
    public void removeSystemApp(String packageName) {
        runAsRoot(new String[]{
                "mount -o remount,rw /system",
                "rm -rf /system/app/" + packageName,
                "mount -o remount,ro /system"
        });
    }

    /**
     * 校验命令执行结果
     */
//    public boolean disableApp(String packageName) {
//        String[] commands = {
//                "pm disable " + packageName,
//                "echo $?"
//        };
//
//        String result = executeRootCommandForResult(Arrays.toString(commands),1000).exitCode;
//        return "0".equals(result.trim());
//    }

    /**
     * Magisk SU路径检测
     */
    public static String detectSuPath() {
        String[] magiskPaths = {
                "/data/adb/magisk",
                "/sbin/.magisk"
        };

        for (String path : magiskPaths) {
            File suFile = new File(path, "su");
            if (suFile.exists()) {
                return suFile.getAbsolutePath();
            }
        }
        return "su"; // 回退到默认路径
    }

    // 检查Magisk版本
    public static String getMagiskVersion() {
        return executeRootCommandForResult("magisk -v", 1000).toString();
    }

    // 安装Magisk模块
    public static void installModule(File moduleZip) {
        runAsRoot(new String[]{
                "magisk --install-module " + moduleZip.getAbsolutePath()
        });
    }

    // 核心执行方法
    public static CommandResult executeRootCommandForResult(String command, long timeoutMs) {
        CommandResult result = new CommandResult();
        Process process = null;
        DataOutputStream os = null;
        BufferedReader stdoutReader = null;
        BufferedReader stderrReader = null;

        try {
            // 启动su进程
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // 启动流读取线程
            final StringBuilder stdout = new StringBuilder();
            final StringBuilder stderr = new StringBuilder();

            // 标准输出读取线程
            stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader finalStdoutReader = stdoutReader;
            Thread stdoutThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = finalStdoutReader.readLine()) != null) {
                        stdout.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // 错误流读取线程
            stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader finalStderrReader = stderrReader;
            Thread stderrThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = finalStderrReader.readLine()) != null) {
                        stderr.append(line).append("\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            stdoutThread.start();
            stderrThread.start();

            // 执行命令
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            // 等待进程结束（带超时）
            boolean finished;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            } else {
                // 兼容旧版本的手动实现
                long startTime = System.currentTimeMillis();
                while (true) {
                    try {
                        result.exitCode = process.exitValue();
                        finished = true;
                        break;
                    } catch (IllegalThreadStateException e) {
                        if (System.currentTimeMillis() - startTime > timeoutMs) {
                            finished = false;
                            break;
                        }
                        Thread.sleep(50);
                    }
                }
            }

            // 处理结果
            if (!finished) {
                process.destroy();
                result.exitCode = -1;
                result.stderr = "Command timed out";
                return result;
            }

            // 等待流读取完成
            stdoutThread.join(500);
            stderrThread.join(500);

            result.stdout = stdout.toString().trim();
            result.stderr = stderr.toString().trim();
            result.exitCode = process.exitValue();

        } catch (IOException | InterruptedException e) {
            result.exitCode = -1;
            result.stderr = "Execution failed: " + e.getMessage();
        } finally {
            // 清理资源
            try {
                if (os != null) os.close();
                if (stdoutReader != null) stdoutReader.close();
                if (stderrReader != null) stderrReader.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    // 结果封装类
    public static class CommandResult {
        public int exitCode = -1;
        public String stdout = "";
        public String stderr = "";

        public boolean isSuccess() {
            return exitCode == 0;
        }

        @Override
        public String toString() {
            return "Exit code: " + exitCode + "\n" +
                    "Stdout: " + stdout + "\n" +
                    "Stderr: " + stderr;
        }
    }

}
