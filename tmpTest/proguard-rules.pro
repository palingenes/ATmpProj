# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile



# 防止混淆和裁剪网络相关类
-keep class java.net.NetworkInterface { *; }
-keep class java.net.InetAddress { *; }

# 保留 IpScanner 相关类（假设你有自定义的 IpScanner）
-keep class com.cymf.tmp.utils.IpScanner { *; }

# 不要裁剪枚举类型
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 防止 R8 删除未显式引用的代码
-keep class * {
    public private protected *;
}