function randomHex(len) {
    const chars = '0123456789abcdef';
    let r = '';
    for (let i = 0; i < len; i++) {
        r += chars.charAt(Math.floor(Math.random() * 16));
    }
    return r;
}

// -------------------------------
// 1. 生成本次进程唯一标识
// -------------------------------
let FAKE_GAID = null;
let FAKE_ANDROID_ID = null;
let FAKE_FID = null;

Java.perform(() => {
    FAKE_GAID = `${randomHex(8)}-${randomHex(4)}-4${randomHex(3)}-${['8', '9', 'a', 'b'][Math.floor(Math.random() * 4)]}${randomHex(3)}-${randomHex(12)}`;
    FAKE_ANDROID_ID = randomHex(16);
    FAKE_FID = `cr${randomHex(11)}`;

    console.log(`[🆔] 伪造 GAID: ${FAKE_GAID}`);
    console.log(`[🔧] 伪造 ANDROID_ID: ${FAKE_ANDROID_ID}`);
    console.log(`[🌐] 伪造 FID: ${FAKE_FID}`);

    // -------------------------------
    // 2. Hook ANDROID_ID
    // -------------------------------
    const SettingsSecure = Java.use('android.provider.Settings$Secure');
    SettingsSecure.getString.overloads.forEach(overload => {
        overload.implementation = function (context, name) {
            if (name === 'android_id') {
                console.log(`[+] 拦截 ANDROID_ID -> ${FAKE_ANDROID_ID}`);
                return FAKE_ANDROID_ID;
            }
            return this.getString(context, name);
        };
    });

    // -------------------------------
    // 3. Hook AdvertisingIdClient.getAdvertisingIdInfo()
    // -------------------------------
    try {
        const AdvertisingIdClient = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient');
        const Info = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient$Info');

        AdvertisingIdClient.getAdvertisingIdInfo.implementation = function (context) {
            console.log("[🔧] 拦截 getAdvertisingIdInfo -> 返回伪造 Info 实例");
            return Info.$new(FAKE_GAID, false); // 使用真实构造函数
        };

        // 异步方法（部分 SDK 使用）
        if (AdvertisingIdClient.getAdvertisingIdInBackground) {
            AdvertisingIdClient.getAdvertisingIdInBackground.implementation = function (context) {
                console.log("[🔧] 拦截 getAdvertisingIdInBackground");
                return Info.$new(FAKE_GAID, false);
            };
        }

        console.log("[✅] Hooked AdvertisingIdClient");

    } catch (e) {
        console.log("[⚠️] 未找到 AdvertisingIdClient: " + e.message);
    }

    // -------------------------------
    // 4. Hook FirebaseInstallations.getId()
    // -------------------------------
    try {
        const FirebaseInstallations = Java.use('com.google.firebase.installations.FirebaseInstallations');
        const Tasks = Java.use('com.google.android.gms.tasks.Tasks');

        FirebaseInstallations.getId.overloads.forEach(overload => {
            overload.implementation = function () {
                console.log(`[🔥] 拦截 FirebaseInstallations.getId() -> ${FAKE_FID}`);
                return Tasks.forResult(FAKE_FID);
            };
        });

        console.log("[✅] Hooked FirebaseInstallations");

    } catch (e) {
        console.log("[⚠️] 未找到 FirebaseInstallations: " + e.message);
    }

    // -------------------------------
    // 5. 欺骗 SharedPreferences（首次启动标记）
    // -------------------------------
    const SP_RESET_KEYS = [
        // AppsFlyer
        { file: 'AF_SHARED_PREFS', key: 'firstLaunchTime', type: 'long' },
        { file: 'AF_SHARED_PREFS', key: 'firstLaunchTimeInMillis', type: 'long' },
        { file: 'AF_SHARED_PREFS', key: 'started', type: 'boolean' },
        { file: 'advertiserId', key: 'firstLaunchTime2', type: 'string' },

        // Adjust
        { file: 'adjust_default_preferences', key: 'session_count', type: 'int' },
        { file: 'adjust_default_preferences', key: 'first_launch', type: 'long' },
        { file: 'adjust_default_preferences', key: 'device_known', type: 'boolean' },
        { file: 'adjust_default_preferences', key: 'needs_to_deduplicate', type: 'boolean' },

        // Branch
        { file: 'DeviceIdentifiers', key: 'device_fingerprint_id', type: 'string' },
        { file: 'ServerRequestQueue', key: 'session_id', type: 'string' },
        { file: 'Branch', key: 'session_id', type: 'string' },

        // Facebook
        { file: 'com.facebook.sdk.appEventPreferences', key: 'anonymousAppDeviceGUID', type: 'string' },

        // Firebase Analytics
        { file: 'firebase_analytics', key: 'first_open_time', type: 'long' },
        { file: 'firebase_analytics', key: 'first_open_time_sec', type: 'long' },
        { file: 'app_instance_id', type: 'string' },
    ];

    const SharedPreferences = Java.use('android.content.SharedPreferences');

    SharedPreferences.getLong.implementation = function (key, defValue) {
        for (const h of SP_RESET_KEYS) {
            if (this.$className.includes(h.file) && key === h.key && h.type === 'long') {
                console.log(`[🔄] 伪造 ${this.$className}.${key} -> -1`);
                return -1;
            }
        }
        return this.getLong(key, defValue);
    };

    SharedPreferences.getInt.implementation = function (key, defValue) {
        for (const h of SP_RESET_KEYS) {
            if (this.$className.includes(h.file) && key === h.key && h.type === 'int') {
                console.log(`[🔄] 伪造 ${this.$className}.${key} -> 1`);
                return 1;
            }
        }
        return this.getInt(key, defValue);
    };

    SharedPreferences.getBoolean.implementation = function (key, defValue) {
        for (const h of SP_RESET_KEYS) {
            if (this.$className.includes(h.file) && key === h.key && h.type === 'boolean') {
                if (key === 'device_known' || key === 'started') {
                    console.log(`[🔄] 伪造 ${this.$className}.${key} -> false`);
                    return false;
                }
            }
        }
        return this.getBoolean(key, defValue);
    };

    SharedPreferences.getString.implementation = function (key, defValue) {
        for (const h of SP_RESET_KEYS) {
            if (this.$className.includes(h.file) && key === h.key && h.type === 'string') {
                const fakeValue = `fake_${randomHex(8)}`;
                console.log(`[🔄] 伪造 ${this.$className}.${key} -> ${fakeValue}`);
                return fakeValue;
            }
        }
        return this.getString(key, defValue);
    }

    // -------------------------------
    // 6. 清除 WebView Cookie & Storage
    // -------------------------------
    try {
        const WebView = Java.use('android.webkit.WebView');

        // Hook 常见构造函数
        ['overload("android.content.Context")', 'overload("android.content.Context", "android.util.AttributeSet")']
            .forEach(methodName => {
                try {
                    WebView['$init.' + methodName].implementation = function (context, attrs) {
                        const ret = this['$init.' + methodName].apply(this, arguments);
                        console.log("[🧹] 初始化 WebView -> 清除存储");

                        const cm = Java.use('android.webkit.CookieManager').getInstance();
                        cm.removeAllCookies(null);
                        cm.flush();

                        Java.use('android.webkit.WebStorage').getInstance().deleteAllData();

                        return ret;
                    };
                } catch (e) {
                    // 忽略不支持的重载
                }
            });

        console.log("[✅] Hooked WebView 初始化");

    } catch (e) {
        console.log("[⚠️] 无法 Hook WebView: " + e.message);
    }

    // -------------------------------
    // 7. 阻止读取 Firebase 安装缓存文件（关键！）
    // -------------------------------
    try {
        const FileInputStream = Java.use('java.io.FileInputStream');

        // Hook 构造函数：FileInputStream(File file)
        FileInputStream.$init.overload('java.io.File').implementation = function (file) {
            const path = file.toString();
            if (path.indexOf("firebase_installations") !== -1) {
                console.log("[🔥] 拦截到 FID 缓存文件读取: " + path);
                throw Java.use('java.io.FileNotFoundException').$new("Blocked firebase installations cache");
            }
            // 正常调用原构造函数
            return this.$init(file);
        };

        // 可选：Hook String 路径版本
        FileInputStream.$init.overload('java.lang.String').implementation = function (path) {
            if (path.indexOf("firebase_installations") !== -1) {
                console.log("[🔥] 拦截到 FID 缓存文件读取 (String): " + path);
                throw Java.use('java.io.FileNotFoundException').$new("Blocked firebase installations cache");
            }
            return this.$init(path);
        };

    } catch (e) {
        console.log("[⚠️] 无法 Hook FileInputStream: " + e.message);
    }

    // -------------------------------
    // 8. 清除 Telephony 唯一标识（备用）
    // -------------------------------
    try {
        const TelephonyManager = Java.use('android.telephony.TelephonyManager');
        ['getDeviceId', 'getImei', 'getMeid', 'getSimSerialNumber'].forEach(method => {
            if (TelephonyManager[method]) {
                TelephonyManager[method].overloads.forEach(over => {
                    over.implementation = function () {
                        console.log(`[📵] 拦截 ${method} -> 返回 null`);
                        return null;
                    };
                });
            }
        });
    } catch (e) {
        console.log("[⚠️] 无法 Hook TelephonyManager");
    }

    // -------------------------------
    // 9. 最终提示
    // -------------------------------
    console.log("[🎉] 🚀 设备伪装完成：已模拟“广告 ID 重置”行为！");
    console.log("[💡] 建议搭配 'adb shell pm clear <pkg>' 使用以确保干净环境");
});