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




// -----------------------------------------------------------------------------






Java.perform(function () {
    try { hookGAIDAndAppSetId(); } catch (e) { logExceptionToConsole(e, " hookGAIDAndAppSetId:"); }

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
    } catch (e) {
        notifyError(e); //   WebSettings
    }
});

Java.perform(function () {

    var ContentResolver = Java.use("android.content.ContentResolver");
    var Cursor = Java.use("android.database.Cursor");

    function makeFakeCursor(value) {
        // 避免重复注册类名冲突
        var className = "Cursor2_" + Math.random().toString(36).substr(2, 9);

        var FakeCursor = Java.registerClass({
            name: className,
            implements: [Cursor],
            methods: {
                // --- Cursor 方法实现 ---
                getColumnNames: function () { return ["value"]; },
                getColumnIndex: function (columnName) { return columnName === "value" ? 0 : -1; },
                getString: function (columnIndex) {
                    if (columnIndex === 0) return value;
                    return null;
                },
                getInt: function (columnIndex) { return columnIndex === 0 ? 0 : -1; },
                getLong: function (columnIndex) { return columnIndex === 0 ? 0 : -1; },
                getFloat: function (columnIndex) { return columnIndex === 0 ? 0.0 : 0.0; },
                getDouble: function (columnIndex) { return columnIndex === 0 ? 0.0 : 0.0; },
                getBlob: function (columnIndex) { return null; },
                isNull: function (columnIndex) { return false; },

                getCount: function () { return 1; },
                getPosition: function () { return 0; },
                move: function (offset) { return offset === 0; },
                moveToPosition: function (position) { return position === 0; },
                moveToFirst: function () { return true; },
                moveToLast: function () { return this.getCount() > 0; },
                moveToNext: function () { return false; },
                moveToPrevious: function () { return false; },
                isFirst: function () { return true; },
                isLast: function () { return true; },
                isBeforeFirst: function () { return false; },
                isAfterLast: function () { return !this.isLast(); },

                getColumnCount: function () { return 1; },
                getColumnName: function (index) { return index === 0 ? "value" : null; },

                close: function () { },
                isClosed: function () { return false; },

                registerContentObserver: function () { },
                unregisterContentObserver: function () { },
                registerDataSetObserver: function () { },
                unregisterDataSetObserver: function () { },

                setNotificationUri: function () { },
                getNotificationUri: function () { return null; },
                getExtras: function () { return null; },
                respond: function () { return null; },
                getWantsAllOnMoveCalls: function () { return false; }
            }
        });
        return FakeCursor.$new();
    }

    ContentResolver.query.overloads.forEach(function (overload) {
        overload.implementation = function (uri, projection, selection, selectionArgs, sortOrder, cancellationSignal) {
            try {
                var uriString = uri.toString();

                if (
                    uriString.includes("gsf.gservices") &&
                    selection &&
                    typeof selection === 'string' &&
                    selection.includes("android_id")
                ) {
                    console.log("[*] 拦截到 GSF 查询 Android ID: " + uriString);
                    console.log("[+] 返回伪造 Android ID: " + getFakeAndroidId());

                    return makeFakeCursor(getFakeAndroidId());
                }

                // 如果是 Bundle 版本（Android 11+），也检查 bundle 中是否有 android_id
                if (
                    uriString.includes("gsf.gservices") &&
                    arguments.length >= 3 &&
                    Java.classFactory.use("android.os.Bundle").isInstance(selection)
                ) {
                    var bundle = selection;
                    var keys = bundle.keySet().toArray();
                    for (var i = 0; i < keys.length; i++) {
                        if (keys[i].includes("android_id")) {
                            console.log("[*] 拦截到 Bundle 查询 Android ID: " + uriString);
                            console.log("[+] 返回伪造 Android ID: " + getFakeAndroidId());
                            return makeFakeCursor(getFakeAndroidId());
                        }
                    }
                }

            } catch (e) {
                console.warn("[-] query Hook 出错: ", e.message);
            }
            return this.query.apply(this, arguments);
        };
    });
});

/**
 * 获取三方 id 
 */
function hookGAIDAndAppSetId() {
    try {
        // 尝试获取 AdvertisingIdClient$Info 类 adid GAID
        var AdvertisingIdClientInfo = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient$Info');
        AdvertisingIdClientInfo.$init.overload('java.lang.String', 'boolean').implementation = function (id, flag) {  //TODO by ly SDK+Hook
            logToFile('AdvertisingIdClient.Info constructed with ID: ' + id + ' and limitAdTracking: ' + flag);
            id = getFakeGaid();
            flag = false;
            return this.$init(id, flag);
        };

        // Hook getId 方法来获取广告 ID。
        AdvertisingIdClientInfo.getId.implementation = function () {          //TODO by ly SDK+Hook
            var id = this.getId();
            id = getFakeGaid();
            logToFile('AdvertisingIdClient.Info.getId() returned: ' + id);
            return id;
        };
    } catch (e) {
        notifyError(e);
    }

    try {
        // TODO 不SDK, 需要Hook 
        var AppSetIdInfo = Java.use("com.google.android.gms.appset.AppSetIdInfo");
        if (AppSetIdInfo.getId) {
            AppSetIdInfo.getId.overload().implementation = function () {
                var result = this.getId();
                result = getFakeAppSetId();
                logToFile(`[*] 应用读取Google应用集ID AppSetIdInfo.getId` + " result=" + result);
                return result;
            }
        }
    } catch (e) {
        notifyError(e);
    }
}

Java.perform(function () {
    console.log("[*] 开始监控应用安装路径获取行为...");

    var Context = Java.use("android.content.Context");
    Context.getPackageCodePath.implementation = function () {
        var result = this.getPackageCodePath();
        console.log("\n[+] 触发：Context.getPackageCodePath()");
        console.log("    返回路径: " + result);

        // 打印调用栈（注意：不要再嵌套 Java.perform）
        var Exception = Java.use("java.lang.Exception");
        var stack = Exception.$new().getStackTrace();
        console.log("    Java Stack:");
        for (var i = 0; i < stack.length; i++) {
            console.log("        " + stack[i].toString());
        }

        return result;
    };

    // 2. 正确 Hook ApplicationInfo.sourceDir 字段
    var ApplicationInfo = Java.use("android.content.pm.ApplicationInfo");

    Object.defineProperty(ApplicationInfo, 'sourceDir', {
        get: function () {
            var result = this.sourceDir;  // ✅ 正确：直接读取字段值
            console.log("\n[+] 触发：ApplicationInfo.sourceDir 被读取");
            console.log("    sourceDir: " + result);

            var Exception = Java.use("java.lang.Exception");
            var stack = Exception.$new().getStackTrace();
            console.log("    Java Stack:");
            for (var i = 0; i < stack.length; i++) {
                console.log("        " + stack[i].toString());
            }

            return result;
        },
        set: function (value) {
            console.log("\n[!] 注意：ApplicationInfo.sourceDir 被修改");
            console.log("    原值: " + this.sourceDir);
            console.log("    新值: " + value);
            this.sourceDir = value;  // ✅ 正确赋值
        }
    });

    var PackageInfo = Java.use("android.content.pm.PackageInfo");

    Object.defineProperty(PackageInfo, 'applicationInfo', {
        get: function () {
            var appInfo = this.applicationInfo;  // ✅ 正确获取
            console.log("\n[+] 触发：PackageInfo.applicationInfo 被访问");
            console.log("    sourceDir: " + appInfo.sourceDir);
            console.log("    dataDir: " + appInfo.dataDir);

            var Exception = Java.use("java.lang.Exception");
            var stack = Exception.$new().getStackTrace();
            console.log("    Java Stack:");
            for (var i = 0; i < stack.length; i++) {
                console.log("        " + stack[i].toString());
            }

            return appInfo;
        },
        set: function (value) {
            console.log("\n[!] 注意：PackageInfo.applicationInfo 被修改");
            this.applicationInfo = value;
        }
    });
});



// ==================== 全局变量：确保每个 ID 只生成一次 ====================
let FAKE_GAID = null;
let FAKE_ANDROID_ID = null;
let FAKE_APPSET_ID = null;

// ==================== 工具函数：生成随机 ID ====================

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

// 获取或生成（单例）
function getFakeGaid() {
    if (FAKE_GAID === null) {
        FAKE_GAID = generateRandomGAID();
        console.log("[+] 已生成随机 GAID: " + FAKE_GAID);
    }
    return FAKE_GAID;
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