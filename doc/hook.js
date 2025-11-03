//输出到控制台
function logToFile(message) {
    if (true) {
        console.log(message);
    }
}
function logToConsole(message) {
    console.log(message);
}
function logExceptionToConsole(error, tag) {
    const errorMessage = (error && (error.message || error.toString())) || "未知错误";
    console.log('[*]hook 代码执行异常:' + tag + errorMessage);
}
//异常输出
function notifyError(error) {
    function isJavaException(err) {
        if (!err) return false;
        if (typeof err.getClass === 'function') {
            return true;
        }
        const str = (err.stack || err.toString() || '').toLowerCase();
        if (/java\.lang\.(exception|throwable)/.test(str)) {
            return true;
        }
        try {
            const Throwable = Java.use("java.lang.Throwable");
            const casted = Java.cast(err, Throwable);
            return casted !== null && typeof casted.getMessage === 'function';
        } catch (e) {
            return false;
        }
    }

    if (isJavaException(error)) {
        console.warn("🔁 抛出App异常:" + error.toString());
        throw error;
    }

    const errorMessage = (error && (error.message || error.toString())) || "未知错误";
    // 下面也可以直接用一行日志输出 console.error(xxx)
    // 目前使用console.warn只是为了方便区分正常的log和未被捕获的error异常
    if (/class.*not found/i.test(errorMessage)) {
        console.warn("📎 检测到类未找到错误（可能是 API 不支持）:" + errorMessage);
    } else if (/implementation was called/i.test(errorMessage)) {
        console.warn("📎 hook 方法被调用时出错，可能 hook 逻辑有误:" + errorMessage);
    } else if (/attempt to.*invoke virtual method/i.test(errorMessage)) {
        console.warn("📎 调用空对象的方法导致 NullPointerException:" + errorMessage);
    } else {
        console.warn("📎 其他 JS/Frida 错误:" + errorMessage);
    }
    // 开发阶段可以选择重新抛出，方便定位问题
    // throw error;
}

//方法里抛出异常
function throwError(error) {
    console.error(`ERROR::::::` + error); // 控制台输出错误日志
    throw error;
}




// // -----------------------------------------------------------------------------

let FAKE_GAID = undefined;

function getFakeGaid() {
    if (typeof FAKE_GAID === 'undefined' || FAKE_GAID === null) {
        FAKE_GAID = generateRandomGAID();
        console.log("[+] 已生成随机 GAID: " + FAKE_GAID);
    }
    return FAKE_GAID;
}

// --- 3. 辅助函数：生成 GAID ---
function generateRandomGAID() {
    const hex = "0123456789abcdef";
    const segments = [8, 4, 4, 4, 12];
    let id = "";
    for (let i = 0; i < segments.length; i++) {
        for (let j = 0; j < segments[i]; j++) {
            id += hex.charAt(Math.floor(Math.random() * 16));
        }
        if (i < 4) id += "-";
    }
    return id;
}



Java.perform(function () {

    try { hookGAIDAndAppSetId(); } catch (e) { logExceptionToConsole(e, " hookGAIDAndAppSetId:"); }
    try { hookContentResolver(); } catch (e) { logExceptionToConsole(e, " hookContentResolver:"); }

    const Secure = Java.use('android.provider.Settings$Secure');
    if (Secure.getString) {
        Secure.getString.overload('android.content.ContentResolver', 'java.lang.String').implementation = function (contentResolver, name) {
            let result = this.getString(contentResolver, name);
            try {
                if (name == "android_id") {                                 //  TODO by ly 不需要SDK，需要Hook。清除数据后android_id不变
                    result = getFakeAndroidId();
                    logToFile("[+] 获取到的 Android ID: " + result);
                } else if (name == "enabled_accessibility_services") {//启动的无障碍服务列表 值就是空串
                    result = "";
                } else if (name == "advertising_id") {//google gaid
                    result = getFakeGaid();
                    logToFile(`[+] 伪造 GAID via Secure: ${result} → ${fake}`);
                } else if (name == "accessibility_captioning_locale") {//SDK 无障碍字幕的语言配置
                    result = null;
                } else {
                    logToFile("[+] Secure.getString,用户输入的name: " + name);  //  TODO by wzy ly SDK+Hook。需要处理advertising_id、enabled_accessibility_services、accessibility_captioning_locale
                }
            } catch (e) {
                notifyError(e); //  Secure.getString.overload
            }
            return result;
        };
    }

    try {
        var AppSetIdClient = Java.use("androidx.core.app.appset.AppSetIdClient");

        AppSetIdClient.getAppSetId.implementation = function (context) {
            var result = this.getAppSetId(context);
            result = getFakeAppSetId();
            console.log("[+] App Set ID: " + result);
            return result;
        };
    } catch (e) {
        console.log("[-] AppSetIdClient not found (可能系统 < Android 13 或未引入 androidx.core)");
    }

    try {
        var WebView = Java.use('android.webkit.WebView');
        WebView.$init.overloads.forEach(function (overload) {
            overload.implementation = function () {
                logToFile('[+] WebView构造函数被调用');
                var result = this.$init.apply(this, arguments);
                return result;
            };
        });
        WebView.addJavascriptInterface.implementation = function (obj, interfaceName) {
            logToFile(`[+] 检测到 WebView JS 接口注入: ${interfaceName}`);
            // 使用动态代理包装 obj，拦截其方法
            const Proxy = Java.use('java.lang.reflect.Proxy');
            const InvocationHandler = Java.use('java.lang.reflect.InvocationHandler');

            const handler = InvocationHandler.$new({
                invoke: function (proxy, method, args) {
                    const methodName = method.getName();
                    if (methodName.toLowerCase().includes('adid') ||
                        methodName.includes('gaid') ||
                        methodName.includes('advertising')) {
                        logToFile(`[+] 拦截 JS Bridge 方法: ${methodName} → 返回伪造 GAID`);
                        return getFakeGaid();
                    }
                    return method.invoke(obj, args);
                }
            });
            const clazz = obj.getClass();
            const interfaces = clazz.getInterfaces();
            const newProxy = Proxy.newProxyInstance(clazz.getClassLoader(), interfaces, handler);
            this.addJavascriptInterface(newProxy, interfaceName);
        };
    } catch (e) {
        notifyError(e); //   WebSettings
    }
});

function hookContentResolver() {
    var ContentResolver = Java.use("android.content.ContentResolver");
    var Cursor = Java.use("android.database.Cursor");

    /**
     * 创建一个伪造的 Cursor，返回单行单列的指定值
     * @param {string} value - 要返回的伪造值
     * @returns {Cursor} 伪造的 Cursor 对象
     */
    function makeFakeCursor(value) {
        var className = "FakeCursor_" + Math.random().toString(36).substr(2, 9);

        var FakeCursor = Java.registerClass({
            name: className,
            implements: [Cursor],
            methods: {
                // --- 数据访问 ---
                getColumnNames: function () { return ["value"]; },
                getColumnIndex: function (name) { return name === "value" ? 0 : -1; },
                getString: function (index) { return index === 0 ? value : null; },
                getInt: function (index) { return index === 0 ? 0 : -1; },
                getLong: function (index) { return index === 0 ? 0 : -1; },
                getFloat: function (index) { return index === 0 ? 0.0 : 0.0; },
                getDouble: function (index) { return index === 0 ? 0.0 : 0.0; },
                getBlob: function (index) { return null; },
                isNull: function (index) { return false; },

                // --- 游标位置 ---
                getCount: function () { return 1; },
                getPosition: function () { return 0; },
                move: function (offset) { return offset === 0; },
                moveToPosition: function (pos) { return pos === 0; },
                moveToFirst: function () { return true; },
                moveToLast: function () { return true; },
                moveToNext: function () { return false; },
                moveToPrevious: function () { return false; },
                isFirst: function () { return true; },
                isLast: function () { return true; },
                isBeforeFirst: function () { return false; },
                isAfterLast: function () { return !this.isLast(); },

                // --- 元数据 ---
                getColumnCount: function () { return 1; },
                getColumnName: function (index) { return index === 0 ? "value" : null; },

                // --- 生命周期 ---
                close: function () { },
                isClosed: function () { return false; },

                // --- Observer ---
                registerContentObserver: function () { },
                unregisterContentObserver: function () { },
                registerDataSetObserver: function () { },
                unregisterDataSetObserver: function () { },

                // --- 其他 ---
                setNotificationUri: function () { },
                getNotificationUri: function () { return null; },
                getExtras: function () { return null; },
                respond: function () { return null; },
                getWantsAllOnMoveCalls: function () { return false; }
            }
        });

        return FakeCursor.$new();
    }

    /**
     * 判断列名是否与广告 ID 相关
     * @param {string} colName
     * @returns {boolean}
     */
    function isAdIdColumn(colName) {
        if (!colName) return false;
        const lower = colName.toLowerCase();
        return lower.includes('adid') || lower.includes('advertising') || lower.includes('id');
    }

    // 遍历所有 overload
    ContentResolver.query.overloads.forEach(function (overload) {
        overload.implementation = function (uri, projection, selection, selectionArgs, sortOrder, cancellationSignal) {
            try {
                var uriString = uri ? uri.toString() : "";

                // ========== 情况1：GSF 查询 android_id ==========
                if (uriString.includes("gsf.gservices")) {
                    // 字符串 selection 情况
                    if (typeof selection === 'string' && selection.includes("android_id")) {
                        logToFile("[*] 拦截 GSF 查询 Android ID (字符串): " + uriString);
                        logToFile("[+] 返回伪造 Android ID: " + getFakeAndroidId());
                        return makeFakeCursor(getFakeAndroidId());
                    }

                    // Bundle selection 情况 (Android 11+)
                    if (Java.classFactory.use("android.os.Bundle").isInstance(selection)) {
                        var bundle = selection;
                        var keys = bundle.keySet().toArray();
                        for (var i = 0; i < keys.length; i++) {
                            if (keys[i].includes("android_id")) {
                                logToFile("[*] 拦截 GSF 查询 Android ID (Bundle): " + uriString);
                                logToFile("[+] 返回伪造 Android ID: " + getFakeAndroidId());
                                return makeFakeCursor(getFakeAndroidId());
                            }
                        }
                    }
                }

                // ========== 情况2：查询广告 ID 相关表，动态篡改 Cursor 返回值 ==========
                if (uriString.includes('adid') || uriString.includes('advertising') || uriString.includes('gsf')) {
                    // 先调用原始 query 获取 cursor
                    var cursor = this.query.call(this, uri, projection, selection, selectionArgs, sortOrder, cancellationSignal);
                    if (!cursor || cursor.isClosed()) return cursor;

                    // 仅当 cursor 有效时，Hook 其 getString 方法
                    var originalGetString = cursor.getString.overloads[0];
                    if (!originalGetString.$replaced) {  // 防止重复 Hook
                        originalGetString.implementation = function (colIndex) {
                            try {
                                var colName = this.getColumnName(colIndex);
                                if (isAdIdColumn(colName)) {
                                    var realValue = originalGetString.call(this, colIndex);
                                    var fakeValue = getFakeGaid();
                                    logToFile(`[+] 伪造 Cursor 列 "${colName}": ${realValue} → ${fakeValue}`);
                                    return fakeValue;
                                }
                            } catch (e) {
                                logToFile("[-] Cursor getString 拦截失败: " + e.message);
                            }
                            return originalGetString.call(this, colIndex);
                        };
                        // 标记已 Hook，防止重复
                        originalGetString.$replaced = true;
                    }

                    return cursor;
                }

            } catch (e) {
                logToFile("[-] ContentResolver.query Hook 异常: " + e.message);
                logToFile("[-] Stack: " + e.stack);
            }

            // 默认放行
            return this.query.apply(this, arguments);
        };
    });
}

/**
 * 获取三方 id
 */
function hookGAIDAndAppSetId() {
    try {
        const Info = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient$Info');

        // Hook 构造函数：new Info(id, isLimitAdTrackingEnabled)
        Info.$init.overload('java.lang.String', 'boolean').implementation = function (id, isLimit) {
            const fakeId = getFakeGaid();
            const fakeLimit = false;
            logToFile(`[+] 构造 AdvertisingIdClient$Info: "${id}" → "${fakeId}", limit=${isLimit} → ${fakeLimit}`);
            return this.$init(fakeId, fakeLimit);
        };

        // Hook getId()
        Info.getId.implementation = function () {
            const realId = this.getId.call(this); // 调用原始方法（即使不使用）
            const fakeId = getFakeGaid();
            logToFile(`[+] 伪造 AdvertisingIdClient$Info.getId(): "${realId}" → "${fakeId}"`);
            return fakeId;
        };
    } catch (e) {
        handleError(e, "Hook AdvertisingIdClient$Info");
    }

    // ==================== 2. Hook AdvertisingIdClient.getAdvertisingIdInfo ====================
    try {
        const Client = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient');
        Client.getAdvertisingIdInfo.overload('android.content.Context').implementation = function (context) {
            logToFile('[+] 拦截 AdvertisingIdClient.getAdvertisingIdInfo()');
            const task = this.getAdvertisingIdInfo(context);

            // Hook onSuccess 回调
            const onSuccess = task.addOnSuccessListener;
            if (onSuccess) {
                onSuccess.implementation = function (listener) {
                    return this.addOnSuccessListener({
                        onSuccess: function (info) {
                            logToFile(`[+] 在 onSuccess 中伪造 GAID`);

                            // 动态篡改 Info 对象行为
                            info.getId = function () {
                                return getFakeGaid();
                            };
                            info.isLimitAdTrackingEnabled = function () {
                                return false;
                            };

                            return listener.onSuccess(info);
                        }
                    });
                };
            }

            return task;
        };
    } catch (e) {
        handleError(e, "Hook getAdvertisingIdInfo");
    }

    try {
        const ServiceManager = Java.use('android.os.ServiceManager');
        ServiceManager.getService.implementation = function (name) {
            const binder = this.getService(name);
            if (binder && name && typeof name.toString === 'function') {
                const nameStr = name.toString();
                if (nameStr.toLowerCase().includes('adsid')) {
                    logToFile(`[+] 检测到 AIDL 请求: ${nameStr}`);
                    // 可扩展：包装 binder，拦截 transact
                }
            }
            return binder;
        };
    } catch (e) {
        handleError(e, "Hook ServiceManager.getService");
    }

    // ==================== 5. Hook AppSetIdInfo.getId ====================
    try {
        const AppSetIdInfo = Java.use("com.google.android.gms.appset.AppSetIdInfo");
        if (AppSetIdInfo.getId && typeof AppSetIdInfo.getId === 'function') {
            AppSetIdInfo.getId.overload().implementation = function () {
                const result = this.getId.call(this);
                const fake = getFakeAppSetId();
                logToFile(`[*] 伪造 AppSetIdInfo.getId(): "${result}" → "${fake}"`);
                return fake;
            };
        }
    } catch (e) {
        handleError(e, "Hook AppSetIdInfo");
    }

    // ==================== 6. 清理 SharedPreferences 缓存 ====================
    setTimeout(function cleanupSharedPreferences() {
        try {
            const ActivityThread = Java.use('android.app.ActivityThread');
            const app = ActivityThread.currentApplication();
            if (!app) {
                logToFile("[-] 无法获取 Application 实例");
                return;
            }
            const ctx = app.getApplicationContext();

            const prefsList = [
                "com.facebook.sdk.appEvents",
                "appsflyer.sdk",
                "adjust_preferences",
                "com.google.android.gms.appid",
                "io.branch.sdk.views",
                "com.mixpanel.android.mpmetrics"
            ];

            const keysToClear = ["advertiser_id", "gaid", "gps_adid", "hardware_id", "device_id"];

            prefsList.forEach(function (prefName) {
                try {
                    const sp = ctx.getSharedPreferences(prefName, 0);
                    const editor = sp.edit();
                    let hasChanges = false;

                    keysToClear.forEach(function (key) {
                        if (sp.contains(key)) {
                            editor.remove(key);
                            logToFile(`[✓] 清除缓存: ${prefName}/${key}`);
                            hasChanges = true;
                        }
                    });

                    if (hasChanges) {
                        // 使用 apply() 异步提交
                        editor.apply();
                    }
                } catch (e) {
                    // 单个 sp 失败不影响其他
                }
            });
        } catch (e) {
            logToFile("[-] 清理 SharedPreferences 失败: " + e.message);
        }
    }, 3000);
}

// Java.perform(function () {
//     console.log("[*] 开始监控应用安装路径获取行为...");

//     var Context = Java.use("android.content.Context");
//     Context.getPackageCodePath.implementation = function () {
//         var result = this.getPackageCodePath();
//         console.log("\n[+] 触发：Context.getPackageCodePath()");
//         console.log("    返回路径: " + result);

//         // 打印调用栈（注意：不要再嵌套 Java.perform）
//         var Exception = Java.use("java.lang.Exception");
//         var stack = Exception.$new().getStackTrace();
//         console.log("    Java Stack:");
//         for (var i = 0; i < stack.length; i++) {
//             console.log("        " + stack[i].toString());
//         }

//         return result;
//     };

//     // 2. 正确 Hook ApplicationInfo.sourceDir 字段
//     var ApplicationInfo = Java.use("android.content.pm.ApplicationInfo");

//     Object.defineProperty(ApplicationInfo, 'sourceDir', {
//         get: function () {
//             var result = this.sourceDir;  // ✅ 正确：直接读取字段值
//             console.log("\n[+] 触发：ApplicationInfo.sourceDir 被读取");
//             console.log("    sourceDir: " + result);

//             var Exception = Java.use("java.lang.Exception");
//             var stack = Exception.$new().getStackTrace();
//             console.log("    Java Stack:");
//             for (var i = 0; i < stack.length; i++) {
//                 console.log("        " + stack[i].toString());
//             }

//             return result;
//         },
//         set: function (value) {
//             console.log("\n[!] 注意：ApplicationInfo.sourceDir 被修改");
//             console.log("    原值: " + this.sourceDir);
//             console.log("    新值: " + value);
//             this.sourceDir = value;  // ✅ 正确赋值
//         }
//     });

//     var PackageInfo = Java.use("android.content.pm.PackageInfo");

//     Object.defineProperty(PackageInfo, 'applicationInfo', {
//         get: function () {
//             var appInfo = this.applicationInfo;  // ✅ 正确获取
//             console.log("\n[+] 触发：PackageInfo.applicationInfo 被访问");
//             console.log("    sourceDir: " + appInfo.sourceDir);
//             console.log("    dataDir: " + appInfo.dataDir);

//             var Exception = Java.use("java.lang.Exception");
//             var stack = Exception.$new().getStackTrace();
//             console.log("    Java Stack:");
//             for (var i = 0; i < stack.length; i++) {
//                 console.log("        " + stack[i].toString());
//             }

//             return appInfo;
//         },
//         set: function (value) {
//             console.log("\n[!] 注意：PackageInfo.applicationInfo 被修改");
//             this.applicationInfo = value;
//         }
//     });
// });



// ==================== 全局变量：确保每个 ID 只生成一次 ====================
let FAKE_ANDROID_ID = null;
let FAKE_APPSET_ID = null;

// ==================== 工具函数：生成随机 ID ====================



function generateRandomAndroidId() {
    const hex = "0123456789abcdef";
    let id = "";
    for (let i = 0; i < 16; i++) {
        id += hex.charAt(Math.floor(Math.random() * 16));
    }
    return id;
}

// 生成 22 位 Base64 URL-Safe 的 AppSetId
function generateRandomAppSetId() {
    const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"; // URL-Safe Base64
    let id = "";
    for (let i = 0; i < 22; i++) {
        id += chars.charAt(Math.floor(Math.random() * 64));
    }
    return id;
}


function getFakeAndroidId() {
    if (FAKE_ANDROID_ID === null) {
        FAKE_ANDROID_ID = generateRandomAndroidId();
        console.log("[+] 已生成随机 Android ID: " + FAKE_ANDROID_ID);
    }
    return FAKE_ANDROID_ID;
}

function getFakeAppSetId() {
    if (FAKE_APPSET_ID === null) {
        FAKE_APPSET_ID = generateRandomAppSetId();
        console.log("[+] 已生成随机 AppSetId: " + FAKE_APPSET_ID);
    }
    return FAKE_APPSET_ID;
}



//---------------------------------------------------------------------------------


// Java.perform(function () {
//     console.log("[✅] 开始 Hook com.microfun.onesdk.Platform");

//     const Platform = Java.use("com.microfun.onesdk.Platform");

//     // 1. Hook getGoogleAdid() —— 直接读取广告 ID
//     Platform.getGoogleAdid.implementation = function () {
//         var result = this.getGoogleAdid.call(this);
//         console.log("[🔍] getGoogleAdid() 被调用，返回值: " + result);
//         return result;
//     };

//     // 2. Hook StartGoogleAdidRetrive() —— 异步获取广告 ID 的入口
//     Platform.StartGoogleAdidRetrive.implementation = function (activity) {
//         console.log("[🚀] StartGoogleAdidRetrive() 被调用，准备获取广告 ID...");
//         console.log("[📱] Context: " + activity);

//         // 调用原方法
//         var result = this.StartGoogleAdidRetrive.call(this, activity);

//         // 尝试获取 AdvertisingIdClient.Info 的 getId()
//         var AdvertisingIdClient = Java.use("com.google.android.gms.ads.identifier.AdvertisingIdClient");
//         var Info = Java.use("com.google.android.gms.ads.identifier.AdvertisingIdClient$Info");

//         Info.getId.implementation = function () {
//             var adid = this.getId.call(this);
//             console.log("[🎯] ✅ 成功获取 Google 广告 ID (GAID): " + adid);
//             return adid;
//         };

//         Info.isLimitAdTrackingEnabled.implementation = function () {
//             var limit = this.isLimitAdTrackingEnabled.call(this);
//             console.log("[⚠️] 用户是否启用‘限制广告追踪’(LAT): " + limit);
//             return limit;
//         };

//         return result;
//     };

//     // 3. Hook getMA() —— 获取 MAC 地址
//     Platform.getMA.implementation = function () {
//         var result = this.getMA.call(this);
//         console.log("[📡] getMA() 获取 MAC 地址: " + result);
//         return result;
//     };

//     // 4. Hook getLocale / Country / Language
//     Platform.getLocale.implementation = function () {
//         var result = this.getLocale.call(this);
//         console.log("[🌐] getLocale() -> " + result);
//         return result;
//     };

//     Platform.getCountry.implementation = function () {
//         var result = this.getCountry.call(this);
//         console.log("[🌍] getCountry() -> " + result);
//         return result;
//     };

//     Platform.getLanguage.implementation = function () {
//         var result = this.getLanguage.call(this);
//         console.log("[🗣] getLanguage() -> " + result);
//         return result;
//     };

//     // 5. Hook getMCC / getMNC
//     Platform.getSimOperator.implementation = function () {
//         var result = this.getSimOperator.call(this);
//         console.log("[📞] getSimOperator() -> MCC+MNC: " + result);
//         return result;
//     };

//     // 6. Hook getOSVersion
//     Platform.getOSVersion.implementation = function () {
//         var result = this.getOSVersion.call(this);
//         console.log("[⚙️] getOSVersion() -> " + result);
//         return result;
//     };

//     // 7. Hook getAppDataSize
//     Platform.getAppDataSize.implementation = function () {
//         var result = this.getAppDataSize.call(this);
//         console.log("[💾] getAppDataSize() -> " + result + " bytes");
//         return result;
//     };

//     // 8. Hook isGmsAvailable
//     Platform.isGmsAvailable.implementation = function () {
//         var result = this.isGmsAvailable.call(this);
//         console.log("[🧩] Google Play Services 可用性: " + result);
//         return result;
//     };

//     // 9. Hook getMetaValue
//     Platform.getMetaValue.implementation = function (context, key) {
//         var result = this.getMetaValue.call(this, context, key);
//         console.log("[🔑] getMetaValue(key=" + key + ") -> " + result);
//         return result;
//     };

//     // 10. Hook isAppInstalled
//     Platform.isAppInstalled.implementation = function (packageName) {
//         var result = this.isAppInstalled.call(this, packageName);
//         console.log("[📦] isAppInstalled(" + packageName + ") -> " + result);
//         return result;
//     };

//     // 11. Hook getMemoryUsed
//     Platform.getMemoryUsed.implementation = function () {
//         var result = this.getMemoryUsed.call(this);
//         console.log("[🧠] getMemoryUsed() -> " + result + " bytes");
//         return result;
//     };

//     // 12. Hook getRemainingDiskSpaceInBytes
//     Platform.getRemainingDiskSpaceInBytes.implementation = function () {
//         var result = this.getRemainingDiskSpaceInBytes.call(this);
//         console.log("[💽] 剩余存储空间: " + result + " bytes (" + (result / (1024 * 1024)).toFixed(2) + " MB)");
//         return result;
//     };

//     // 13. Hook getFCM Token（如果 FCM_TOKEN 是通过其他方式设置的）
//     // 注意：FCM Token 通常不在这里设置，但可以监控静态变量
//     setInterval(function () {
//         var fcmToken = Platform.FCM_TOKEN.value;
//         if (fcmToken && fcmToken !== "") {
//             console.log("[🔔] FCM Token 已设置: " + fcmToken);
//             // 防止重复打印
//             Platform.FCM_TOKEN.value = "[已捕获]";
//         }
//     }, 1000);

// });



// ---------------------------------------------------------------------------------



// // 功能：Google 登录、AdMob、Firebase、Play Games、Play Integrity 等
// Java.perform(function () {
//     console.log("[🌍] Universal Google SDK Hook 已注入");

//     // ==================== 工具函数 ====================
//     function logWithPid(tag, msg) {
//         var pid = Java.use('android.os.Process').myPid();
//         console.log(`[mPid:${pid}] ${tag} | ${msg}`);
//     }

//     function printStack() {
//         Java.perform(function () {
//             var Exception = Java.use('java.lang.Exception');
//             var ins = Exception.$new();
//             console.log('\n📘 调用栈:\n' + ins.getStackTrace().map(it => `   at ${it.toString()}`).join('\n'));
//         });
//     }

//     // ==================== 1. Google Sign-In ====================
//     try {
//         var GoogleSignInAccount = Java.use('com.google.android.gms.auth.api.signin.GoogleSignInAccount');

//         GoogleSignInAccount.getDisplayName.implementation = function () {
//             var result = this.getDisplayName.call(this);
//             logWithPid("[🔑 GOOGLE LOGIN]", `用户昵称: ${result}`);
//             return result;
//         };

//         GoogleSignInAccount.getEmail.implementation = function () {
//             var result = this.getEmail.call(this);
//             logWithPid("[🔑 GOOGLE LOGIN]", `邮箱: ${result}`);
//             return result;
//         };

//         GoogleSignInAccount.getId.implementation = function () {
//             var result = this.getId.call(this);
//             logWithPid("[🔑 GOOGLE LOGIN]", `Google ID: ${result}`);
//             return result;
//         };

//         GoogleSignInAccount.getIdToken.implementation = function () {
//             var result = this.getIdToken.call(this);
//             logWithPid("[🔑 GOOGLE LOGIN]", `ID Token: ${result}`);
//             return result;
//         };

//         console.log("[✅] Hooked Google Sign-In");
//     } catch (e) {
//         console.log("[❌] Google Sign-In 未找到");
//     }

//     // ==================== 2. AdMob 广告 ====================
//     try {
//         var MobileAds = Java.use('com.google.android.gms.ads.MobileAds');
//         MobileAds.initialize.implementation = function (context, callback) {
//             logWithPid("[💰 ADMOB]", "AdMob 初始化");
//             return this.initialize.call(this, context, callback);
//         };

//         // Hook 激励视频加载
//         var RewardedAd = Java.use('com.google.android.gms.ads.rewarded.RewardedAd');
//         RewardedAd.loadAd.implementation = function (context, adRequest, listener) {
//             logWithPid("[💰 ADMOB]", "激励视频开始加载");
//             printStack();
//             return this.loadAd.call(this, context, adRequest, listener);
//         };

//         // Hook 广告展示
//         var InterstitialAd = Java.use('com.google.android.gms.ads.InterstitialAd');
//         InterstitialAd.show.implementation = function () {
//             logWithPid("[💰 ADMOB]", "插屏广告正在展示！");
//             printStack();
//             return this.show.call(this);
//         };

//         console.log("[✅] Hooked AdMob");
//     } catch (e) {
//         console.log("[❌] AdMob 未找到");
//     }

//     // ==================== 3. Firebase Analytics ====================
//     try {
//         var FirebaseAnalytics = Java.use('com.google.firebase.analytics.FirebaseAnalytics');

//         FirebaseAnalytics.logEvent.overloads.forEach(function (overload) {
//             overload.implementation = function (name, params) {
//                 var eventName = name ? name.toString() : "unknown";
//                 logWithPid("[📊 FIREBASE]", `事件触发: ${eventName}`);
//                 if (params) {
//                     var entries = params.keySet().toArray();
//                     for (var i = 0; i < entries.length; i++) {
//                         var key = entries[i];
//                         var value = params.get(key);
//                         console.log(`     📌 ${key} = ${value}`);
//                     }
//                 }
//                 printStack();
//                 return this.logEvent.call(this, name, params);
//             };
//         });

//         console.log("[✅] Hooked Firebase Analytics");
//     } catch (e) {
//         console.log("[❌] Firebase Analytics 未找到");
//     }

//     // ==================== 4. Play Games Services ====================
//     try {
//         var GamesClient = Java.use('com.google.android.gms.games.GamesClient');

//         // 解锁成就
//         GamesClient.unlockAchievement.implementation = function (achievementId) {
//             logWithPid("[🎮 PLAY GAMES]", `尝试解锁成就: ${achievementId}`);
//             printStack();
//             return this.unlockAchievement.call(this, achievementId);
//         };

//         GamesClient.unlockAchievementImmediate.implementation = function (callback, achievementId) {
//             logWithPid("[🎮 PLAY GAMES]", `立即解锁成就: ${achievementId}`);
//             return this.unlockAchievementImmediate.call(this, callback, achievementId);
//         };

//         // 提交排行榜
//         GamesClient.submitScore.implementation = function (leaderboardId, score) {
//             logWithPid("[🎮 PLAY GAMES]", `提交排行榜: ${leaderboardId}, 分数: ${score}`);
//             return this.submitScore.call(this, leaderboardId, score);
//         };

//         console.log("[✅] Hooked Play Games Services");
//     } catch (e) {
//         console.log("[❌] Play Games Services 未找到");
//     }

//     // ==================== 5. Play Integrity API ====================
//     try {
//         var PlayIntegrityManager = Java.use('com.google.android.play.integrity.PlayIntegrityManager');

//         PlayIntegrityManager.request.implementation = function (requestConfig) {
//             logWithPid("[🛡️ PLAY INTEGRITY]", "应用请求设备完整性校验");
//             printStack();
//             return this.request.call(this, requestConfig);
//         };

//         console.log("[✅] Hooked Play Integrity API");
//     } catch (e) {
//         console.log("[❌] Play Integrity API 未找到");
//     }

//     // ==================== 6. FCM 推送 ====================
//     try {
//         var FirebaseMessagingService = Java.use('com.google.firebase.messaging.FirebaseMessagingService');

//         FirebaseMessagingService.onNewToken.implementation = function (token) {
//             logWithPid("[🔔 FCM]", `收到新推送 Token: ${token}`);
//             return this.onNewToken.call(this, token);
//         };

//         FirebaseMessagingService.onMessageReceived.implementation = function (remoteMessage) {
//             logWithPid("[🔔 FCM]", "收到推送消息");
//             var data = remoteMessage.getData();
//             if (data.size() > 0) {
//                 console.log("   数据: " + JSON.stringify(data));
//             }
//             return this.onMessageReceived.call(this, remoteMessage);
//         };

//         console.log("[✅] Hooked Firebase Messaging (FCM)");
//     } catch (e) {
//         console.log("[❌] Firebase Messaging 未找到");
//     }

//     // ==================== 7. Google Maps ====================
//     try {
//         var GoogleMap = Java.use('com.google.android.gms.maps.GoogleMap');

//         GoogleMap.setOnMapClickListener.implementation = function (listener) {
//             logWithPid("[🗺️ GOOGLE MAPS]", "地图点击监听器已设置");
//             return this.setOnMapClickListener.call(this, listener);
//         };

//         console.log("[✅] Hooked Google Maps");
//     } catch (e) {
//         console.log("[❌] Google Maps 未找到");
//     }

//     // ==================== 8. 获取进程信息 ====================
//     try {
//         var context = Java.use('android.app.ActivityThread').currentApplication().getApplicationContext();
//         var packageName = context.getPackageName();
//         var processName = Java.use('android.os.Process').getCmdline()[0];
//         logWithPid("[📱 PROCESS]", `包名: ${packageName} | 进程: ${processName}`);
//     } catch (e) {
//         logWithPid("[📱 PROCESS]", "无法获取进程信息");
//     }

// });