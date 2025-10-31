//输出到控制台
function logToFile(message) {
    if (IS_DEBUG) {
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
// ____________________________ 全局Debug开关 ____________________________
// 用于开发阶段调试与输入log, 正式运用直接改成false即可 删除json 
const IS_DEBUG = false;
const IS_HOOK_SO = true;
//-----------------hook开始--------------------------------------------------------------------------------
const SU_PATH = '/system/xbin/su';
const USER_JSON_PATH = '/data/local/files/gms/bin/ud'
const DEVICE_JSON_PATH = '/data/local/files/gms/bin/hd'
const MACRO_JSON_PATH = '/data/local/files/gms/bin/pd'
var DEVICE_DATA = null;//设备信息
var USER_DATA = null;//用户信息
var MACRO_DATA = null;//宏信息
var initJSTime = 0;//脚本执行时间
var initJSNMTime = 0;//脚本执行时间 纳秒
function readFileAsString(filePath) {
    try {
        var File = Java.use("java.io.File");
        var FileInputStream = Java.use("java.io.FileInputStream");
        var BufferedReader = Java.use("java.io.BufferedReader");
        var InputStreamReader = Java.use("java.io.InputStreamReader");
        var StringBuilder = Java.use("java.lang.StringBuilder");

        var file = File.$new(filePath);

        if (!file.exists() || !file.isFile()) {
            logToFile("[-] 文件不存在或不是一个有效文件: " + filePath);
            return null;
        }
        var fis = FileInputStream.$new(file);
        var reader = BufferedReader.$new(InputStreamReader.$new(fis));
        var line;
        var sb = StringBuilder.$new();

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        fis.close();
        return sb.toString();
    } catch (e) {
        logToFile("[-] 读取文件异常: " + e.message);
        return null;
    }
}
/**
 * 脚本执行入口 json解析失败则退出进程
 */
Java.perform(function () {
    logToFile("[*] 脚本文件成功附加到进程:" + Process.id);
    initJSTime = new Date().getTime();
    var System = Java.use("java.lang.System");
    initJSNMTime = System.nanoTime();
    var deviceJson = readFileAsString(DEVICE_JSON_PATH);
    var macroJson = readFileAsString(MACRO_JSON_PATH);
    var userJson = readFileAsString(USER_JSON_PATH);
    if (deviceJson != null) {
        DEVICE_DATA = JSON.parse(deviceJson);
    }
    if (macroJson != null) {
        MACRO_DATA = JSON.parse(macroJson);
        setIPData(MACRO_DATA);
    }
    if (userJson != null) {
        USER_DATA = JSON.parse(userJson);
    }
    if (DEVICE_DATA == null || MACRO_DATA == null || USER_DATA == null) {
        var System = Java.use("java.lang.System");
        System.exit(-1);
    } else {
        removeExceptionStack();
        setBuildInfo();
        hookUserAgentForWebView();
        hookMainInfo();
    }

});
function setIPData(macroInfoData) {
    FAKE_IP = macroInfoData.ip.FAKE_IP;
    FAKE_GATEWAY_V4 = macroInfoData.ip.FAKE_GATEWAY_V4;
    FAKE_GATEWAY_V6 = macroInfoData.ip.FAKE_GATEWAY_V6;
    FAKE_IP_BASE = macroInfoData.ip.FAKE_IP_BASE;
    FAKE_IP_START = macroInfoData.ip.FAKE_IP_START;
    fakeIpRouteOutput = macroInfoData.ip.fakeIpRouteOutput;
}
function getRandomByteSize() {
    return (Math.floor(Math.random() * (16)) + (-5)) * 1024 * 1024;
}
//监听目标App行为- 芯片、定位、相机相关
function hookMainInfo() {
    var userInfoData = USER_DATA
    var deviceInfoData = DEVICE_DATA
    var macroInfoData = MACRO_DATA
    var BuildVersion = Java.use('android.os.Build$VERSION');
    var SDK_INT = parseInt(BuildVersion.SDK_INT.value);
    var ArrayList = Java.use('java.util.ArrayList');
    try {
        try { hookIntentJump(); } catch (e) { logExceptionToConsole(e, " hookIntentJump:"); }
        try { hookFileInputStream(); } catch (e) { logExceptionToConsole(e, " hookFileInputStream:"); }
        try { hookDisplay(userInfoData, ArrayList); } catch (e) { logExceptionToConsole(e, " hookDisplay:"); }
        try { hookSettingSecureAndGlobal(userInfoData, deviceInfoData); } catch (e) { logExceptionToConsole(e, " hookSettingSecureAndGlobal:"); }
        try { hookActivityManager(userInfoData, SDK_INT); } catch (e) { logExceptionToConsole(e, " hookActivityManager:"); }
        try { hookTelephonyManager(userInfoData); } catch (e) { logExceptionToConsole(e, " hookTelephonyManager:"); }
        try { hookStatFs(userInfoData); } catch (e) { logExceptionToConsole(e, " hookStatFs:"); }
        try { hookPackageManager(userInfoData, ArrayList); } catch (e) { logExceptionToConsole(e, " hookPackageManager:"); }
        try { hookCameraManager(); } catch (e) { logExceptionToConsole(e, " hookCameraManager:"); }
        try { hookAudioManager(userInfoData, SDK_INT); } catch (e) { logExceptionToConsole(e, " hookAudioManager:"); }
        try { hookBatteryManager(userInfoData); } catch (e) { logExceptionToConsole(e, " hookBatteryManager:"); }
        try { hookFile(userInfoData, deviceInfoData); } catch (e) { logExceptionToConsole(e, " hookFile:"); }
        try { hookAccessibilityManager(ArrayList); } catch (e) { logExceptionToConsole(e, " hookAccessibilityManager:"); }
        // hookTouchUtils();//hook自定义点击
        try { hookSystemClock(userInfoData); } catch (e) { logExceptionToConsole(e, " hookSystemClock:"); }
        try { hookSystemProperties(); } catch (e) { logExceptionToConsole(e, " hookSystemProperties:"); }
        try { hookGAIDAndAppSetId(); } catch (e) { logExceptionToConsole(e, " hookGAIDAndAppSetId:"); }
        try { hookKeybord(); } catch (e) { logExceptionToConsole(e, " hookKeybord:"); }
        try { hookNativeNetWork(); } catch (e) { logExceptionToConsole(e, " hookNativeNetWork:"); }
        try { hookInternalNetIP(); } catch (e) { logExceptionToConsole(e, " hookInternalNetIP:"); }
        try { hookJavaNetWork(); } catch (e) { logExceptionToConsole(e, " hookJavaNetWork:"); }
        try { hookMotionEvent(macroInfoData); } catch (e) { logExceptionToConsole(e, " hookMotionEvent:"); }
        try { hookRuntime(); } catch (e) { logExceptionToConsole(e, " hookRuntime:"); }
        try {
            if (macroInfoData.others.isCollectNetwork != null && macroInfoData.others.isCollectNetwork) {
                HookSetNetResource();
            } else {
                logToConsole("暂不执行HookSetNetResource:" + JSON.stringify(macroInfoData.others));
            }
        } catch (e) { logExceptionToConsole(e, " HookSetNetResource:"); }
        // hookCommand();//命令执行（fopen）
        logToConsole('[*]hook代码注入执行完毕')
    } catch (e) {
        logToConsole(e)
    }

}

// webSettings 
function hookUserAgentForWebView() {
    var userInfoData = USER_DATA;
    try {
        var WebView = Java.use('android.webkit.WebView');
        // Hook所有构造函数重载
        WebView.$init.overloads.forEach(function (overload) {
            overload.implementation = function () {
                logToFile('[+] WebView构造函数被调用');
                var result = this.$init.apply(this, arguments);
                if (userInfoData != null) {
                    this.getSettings().setUserAgentString(userInfoData.getUserAgentString);
                }
                return result;
            };
        });

    } catch (e) {
        notifyError(e); //   WebSettings
    }
}

//hookMode
function hookMode(userInfoData) {
    try {
        //Display$Mode
        var Mode = Java.use("android.view.Display$Mode");
        if (Mode.getPhysicalHeight) {
            Mode.getPhysicalHeight.overload().implementation = function () { // todo by wzy sdk+hook 
                var result = this.getPhysicalHeight();
                if (userInfoData != null && userInfoData.getMode != null) {
                    result = userInfoData.getMode.getPhysicalHeight;
                }
                logToFile(`[*] 应用读取Mode.getPhysicalHeight` + " result=" + result + " ==" + this.getPhysicalHeight());
                return result;
            }
        }
        if (Mode.getPhysicalWidth) {
            Mode.getPhysicalWidth.overload().implementation = function () { // todo by wzy sdk+hook
                var result = this.getPhysicalWidth();
                logToFile(`[*] 应用读取Mode.getPhysicalWidth` + " result=" + result + "  data==" + userInfoData.getMode.getPhysicalWidth);
                if (userInfoData != null && userInfoData.getMode != null) {
                    result = userInfoData.getMode.getPhysicalWidth;
                }
                return result;
            }
        }
        if (Mode.getRefreshRate) {
            Mode.getRefreshRate.overload().implementation = function () {  // todo by wzy sdk+hook
                var result = this.getRefreshRate();
                if (userInfoData != null && userInfoData.getMode != null) {
                    result = userInfoData.getMode.getRefreshRate;
                }
                logToFile(`[*] 应用读取Mode.getRefreshRate` + " result=" + result);
                return result;
            }
        }
        if (Mode.getSupportedHdrTypes) {
            Mode.getSupportedHdrTypes.overload().implementation = function () {  // todo by wzy sdk+hook
                var result = userInfoData.getMode.getSupportedHdrTypes;
                if (userInfoData != null && userInfoData.getMode != null) {
                    result = userInfoData.getMode.getSupportedHdrTypes;
                }
                logToFile(`[*] 应用读取Mode.getSupportedHdrTypes` + " result=" + result + " ==" + this.getSupportedHdrTypes());
                return result;
            }
        }
        if (Mode.getAlternativeRefreshRates) {
            Mode.getAlternativeRefreshRates.overload().implementation = function () {  // todo by wzy sdk+hook
                var result = userInfoData.getMode.getAlternativeRefreshRates;
                if (userInfoData != null && userInfoData.getMode != null) {
                    result = userInfoData.getMode.getAlternativeRefreshRates;
                }
                logToFile(`[*] 应用读取Mode.getAlternativeRefreshRates` + " result=" + result + " ==" + this.getAlternativeRefreshRates());
                return result;
            }
        }
        if (Mode.getModeId) {//modeId不可修改，否则会导致启动奔溃，启动起来修改不会奔溃。
            Mode.getModeId.overload().implementation = function () { // todo by wzy sdk+hook
                var result = this.getModeId();
                logToFile(`[*] 应用读取Mode.getModeId` + " result=" + result);
                return result;
            }
        }

    } catch (e) {
        notifyError(e)
    }
}
//hook DisplayCount
function hookDisplayCutout(userInfoData, Rect, ArrayList) {
    try {
        var DisplayCutout = Java.use('android.view.DisplayCutout')
        // Hook getBoundingRects
        DisplayCutout.getBoundingRects.overload().implementation = function () {        //TODO by ly sdk+hook admob、pangle反编译代码中有
            var rects = this.getBoundingRects();
            var sdkList = userInfoData.getDisplayCutout.getBoundingRects;
            var results = ArrayList.$new();
            if (sdkList != null && sdkList.length > 0) {
                for (var i = 0; i < sdkList.size(); i++) {
                    var rect = Rect.$new(sdkList[i].left, sdkList[i].top, sdkList[i].right, sdkList[i].bottom);
                    results.add(rect);
                }
                logToFile("[*]  DisplayCutout.getBoundingRects() 刘海区域矩形数量=" + results.size() + " 原始数据==" + rects.size);
                return results;
            } else {
                logToFile("[*]  DisplayCutout.getBoundingRects() 返回默认数据 刘海区域矩形数量=" + results.size() + " 原始数据==" + rects.size);
                return rects;
            }
        };
        DisplayCutout.getSafeInsetTop.overload().implementation = function () { //  todo by wzy sdk+hook
            var result = userInfoData.getDisplayCutout.getSafeInsetTop;
            logToFile("[*] 调用了 DisplayCutout.getSafeInsetTop, 安全边距 (" + result + " px) 默认=" + this.getSafeInsetTop());
            return result;
        }
        DisplayCutout.getSafeInsetBottom.overload().implementation = function () { //  todo by wzy sdk+hook
            var result = userInfoData.getDisplayCutout.getSafeInsetBottom;
            logToFile("[*] 调用了 DisplayCutout.getSafeInsetBottom, 安全边距 (" + result + " px) 默认值=" + this.getSafeInsetBottom());
            return result;
        }
        DisplayCutout.getSafeInsetLeft.overload().implementation = function () { //  todo by wzy sdk+hook
            var result = userInfoData.getDisplayCutout.getSafeInsetLeft;
            logToFile("[*] 调用了 DisplayCutout.getSafeInsetLeft, 安全边距 (" + result + " px)默认值=" + this.getSafeInsetLeft());
            return result;
        }
        DisplayCutout.getSafeInsetRight.overload().implementation = function () { //  todo by wzy sdk+hook
            var result = userInfoData.getDisplayCutout.getSafeInsetRight;
            logToFile("[*] 调用了 DisplayCutout.getSafeInsetRight, 安全边距 (" + result + " px) 默认值=" + this.getSafeInsetRight());
            return result;
        }
        if (DisplayCutout.getWaterfallInsets) {
            DisplayCutout.getWaterfallInsets.overload().implementation = function () { //  todo by wzy sdk+hook
                var result = this.getWaterfallInsets();;
                if (userInfoData != null && userInfoData.getDisplayCutout != null && userInfoData.getDisplayCutout.getWaterfallInsets) {
                    var userData = userInfoData.getDisplayCutout.getWaterfallInsets;
                    result.left.value = userData.left;
                    result.top.value = userData.top;
                    result.right.value = userData.right;
                    result.bottom.value = userData.bottom;
                }
                logToFile("[*] 调用了 DisplayCutout.getWaterfallInsets, 刘海区域矩形=" + result + "  默认值=" + userData);
                return result;
            }
        }
        if (DisplayCutout.getBoundingRectLeft) {
            DisplayCutout.getBoundingRectLeft.overload().implementation = function () {
                var result = this.getBoundingRectLeft();
                if (userInfoData != null && userInfoData.getDisplayCutout != null && userInfoData.getDisplayCutout.getBoundingRectLeft != null) {
                    var userData = userInfoData.getDisplayCutout.getBoundingRectLeft;
                    result.left.value = userData.left;
                    result.top.value = userData.top;
                    result.right.value = userData.right;
                    result.bottom.value = userData.bottom;
                }
                logToFile("[*] 调用了 DisplayCutout.getBoundingRectLeft, 刘海区域矩形=" + result + "  默认值=" + userData);
                return result;
            }
        }
        if (DisplayCutout.getBoundingRectTop) {
            DisplayCutout.getBoundingRectTop.overload().implementation = function () {
                var result = this.getBoundingRectTop();
                if (userInfoData != null && userInfoData.getDisplayCutout != null && userInfoData.getDisplayCutout.getBoundingRectTop != null) {
                    var userData = userInfoData.getDisplayCutout.getBoundingRectTop;
                    result.left.value = userData.left;
                    result.top.value = userData.top;
                    result.right.value = userData.right;
                    result.bottom.value = userData.bottom;
                }
                logToFile("[*] 调用了 DisplayCutout.getBoundingRectTop, 刘海区域矩形=" + result + "  默认值=" + userData);
                return result;
            }
        }
        if (DisplayCutout.getBoundingRectRight) {
            DisplayCutout.getBoundingRectRight.overload().implementation = function () {
                var result = this.getBoundingRectRight();
                if (userInfoData != null && userInfoData.getDisplayCutout != null && userInfoData.getDisplayCutout.getBoundingRectRight != null) {
                    var userData = userInfoData.getDisplayCutout.getBoundingRectRight;
                    result.left.value = userData.left;
                    result.top.value = userData.top;
                    result.right.value = userData.right;
                    result.bottom.value = userData.bottom;
                }
                logToFile("[*] 调用了 DisplayCutout.getBoundingRectRight, 刘海区域矩形=" + result + "  默认值=" + userData);
                return result;
            }
        }
        if (DisplayCutout.getBoundingRectBottom) {
            DisplayCutout.getBoundingRectBottom.overload().implementation = function () {
                var result = this.getBoundingRectBottom();
                if (userInfoData != null && userInfoData.getDisplayCutout != null && userInfoData.getDisplayCutout.getBoundingRectBottom != null) {
                    var userData = userInfoData.getDisplayCutout.getBoundingRectBottom;
                    result.left.value = userData.left;
                    result.top.value = userData.top;
                    result.right.value = userData.right;
                    result.bottom.value = userData.bottom;
                }
                logToFile("[*] 调用了 DisplayCutout.getBoundingRectBottom, 刘海区域矩形=" + result + "  默认值=" + userData);
                return result;
            }
        }
        DisplayCutout.toString.overload().implementation = function () {
            var result = userInfoData.getDisplayCutout.toString;
            logToFile("[*] 调用了 DisplayCutout.toString, " + result + "  默认值=" + this.toString());
            return result;
        }

    } catch (e) {
        notifyError(e); //   DisplayCutout
    }
}

//hook hdrcapabilities类
function hookHdrCapabilities(userInfoData) {
    try {
        const HdrCapabilities = Java.use('android.view.Display$HdrCapabilities');
        if (HdrCapabilities.getDesiredMaxLuminance) {
            HdrCapabilities.getDesiredMaxLuminance.overload().implementation = function () {
                var result = userInfoData.getHdrCapabilities.getDesiredMaxLuminance;
                logToFile(`[*] 应用读取HdrCapabilities.getDesiredMaxLuminance result=${result}`);
                return result;
            }
        }
        if (HdrCapabilities.getDesiredMinLuminance) {
            HdrCapabilities.getDesiredMinLuminance.implementation = function () {
                var result = userInfoData.getHdrCapabilities.getDesiredMinLuminance;
                logToFile(`[*] 应用读取HdrCapabilities.getDesiredMinLuminance result=${result}`);
                return result;
            }
        }
        if (HdrCapabilities.getDesiredMaxAverageLuminance) {
            HdrCapabilities.getDesiredMaxAverageLuminance.implementation = function () {
                var result = userInfoData.getHdrCapabilities.getDesiredMaxAverageLuminance;
                logToFile(`[*] 应用读取HdrCapabilities.getDesiredMaxAverageLuminance result=${result}`);
                return result;
            }
        }
        if (HdrCapabilities.describeContents) {
            HdrCapabilities.describeContents.implementation = function () {
                var result = userInfoData.getHdrCapabilities.describeContents;
                logToFile(`[*] 应用读取HdrCapabilities.describeContents result=${result}`);
                return result;
            }
        }
        if (HdrCapabilities.toString) {
            HdrCapabilities.toString.implementation = function () {
                var result = userInfoData.getHdrCapabilities.toString;
                logToFile(`[*] 应用读取HdrCapabilities.toString result=${result}`);
                return result;
            }
        }
        if (HdrCapabilities.getSupportedHdrTypes) {
            HdrCapabilities.getSupportedHdrTypes.implementation = function () {
                var result = userInfoData.getHdrCapabilities.getSupportedHdrTypes;
                logToFile(`[*] 应用读取HdrCapabilities.getSupportedHdrTypes result=${result}`);
                return result;
            };
        }
    } catch (e) {
        notifyError(e)
    }
}

//hook windowinsets.type类
function hookWindowInsetsType(userInfoData) {
    try {
        var WindowInsetsType = Java.use("android.view.WindowInsets$Type");
        if (WindowInsetsType.systemOverlays) {
            WindowInsetsType.systemOverlays.overload().implementation = function () {   //TODO by ly sdk+hook
                var result = this.systemOverlays();
                var userData = userInfoData.WindowInsets$Type_systemOverlays;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.systemOverlays()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.tappableElement) {
            WindowInsetsType.tappableElement.overload().implementation = function () {  //TODO by ly sdk+hook
                var result = this.tappableElement();
                var userData = userInfoData.WindowInsets$Type_tappableElement;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.tappableElement()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.systemGestures) {
            WindowInsetsType.systemGestures.overload().implementation = function () {   //TODO by ly sdk+hook
                var result = this.systemGestures();
                var userData = userInfoData.WindowInsets$Type_systemGestures;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.systemGestures()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.systemBars) {
            WindowInsetsType.systemBars.overload().implementation = function () {       //TODO by ly sdk+hook
                var result = this.systemBars();
                var userData = userInfoData.WindowInsets$Type_systemBars;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.systemBars()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.navigationBars) {
            WindowInsetsType.navigationBars.overload().implementation = function () {   //TODO by ly sdk+hook
                var result = this.navigationBars();
                var userData = userInfoData.WindowInsets$Type_navigationBars;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.navigationBars()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.mandatorySystemGestures) {
            WindowInsetsType.mandatorySystemGestures.overload().implementation = function () {  //TODO by ly sdk+hook
                var result = this.mandatorySystemGestures();
                var userData = userInfoData.WindowInsets$Type_mandatorySystemGestures;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.mandatorySystemGestures()  result=" + result);
                return result;
            }
        }

        if (WindowInsetsType.statusBars) {
            WindowInsetsType.statusBars.overload().implementation = function () {       //TODO by ly sdk+hook
                var result = this.statusBars();
                var userData = userInfoData.WindowInsets$Type_statusBars;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.statusBars()  result=" + result);
                return result;
            }
        }

        if (WindowInsetsType.captionBar) {
            WindowInsetsType.captionBar.overload().implementation = function () {       //TODO by ly SDK+Hook
                var result = this.captionBar();
                var userData = userInfoData.WindowInsets$Type_captionBar;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.captionBar()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.ime) {
            WindowInsetsType.ime.overload().implementation = function () {  //TODO by ly SDK+Hook
                var result = this.ime();
                var userData = userInfoData.WindowInsets$Type_ime;
                if (userData != null) {
                    result = userData;
                }
                logToFile("[*] 应用读取WindowInsetsType.ime()  result=" + result);
                return result;
            }
        }
        if (WindowInsetsType.displayCutout) {
            WindowInsetsType.displayCutout.overload().implementation = function () {    //TODO by ly SDK+Hook
                var result = this.displayCutout();
                if (userInfoData != null) {
                    result = userInfoData.WindowInsets$Type_displayCutout;
                }
                logToFile("[*] 应用读取WindowInsetsType.displayCutout()  result=" + result);
                return result;
            }
        }
    }
    catch (e) {
        notifyError(e)
    }
}
//TODO dalvik.vm.isa.arm64.variant  dalvik.vm.isa.arm.variant
function getHookValueBySystemPropertiesKey(key, userInfoData, deviceInfoData) {
    var result = null;
    if (key === 'gsm.operator.alpha') {//SDK值（运营商名称）如StarHub
        result = userInfoData.getNetworkOperatorName;
    } else if (key == 'gsm.sim.operator.alpha') {//SDK值（运营商名称）如StarHub
        result = userInfoData.getSimOperatorName;
    } else if (key == 'gsm.operator.numeric') {//SDK值（运营商编码）如52505
        result = userInfoData.getNetworkOperator;
    } else if (key == 'gsm.sim.operator.numeric') { //SDK值（运营商编码）如52505
        result = userInfoData.getSimOperator;
    } else if (key == 'gsm.version.baseband') {//Build获取的值，getRadioVersion 如g5300i-241121-241122-B-12698486,g5300i-241121-241122-B-12698486
        result = deviceInfoData.getRadioVersion;
    } else if (key == 'ro.board.platform') {//Build获取的值rk3588
        result = deviceInfoData.BRAND;
    } else if (key == 'ro.build.display.id') {//Build获取的值BP11.241121.010  userData.DISPLAY
        result = userInfoData.DISPLAY;
    } else if (key == 'ro.build.flavor') {//Build获取的值 husky_beta-user
        result = null;
    } else if (key == 'ro.hardware') {//Build获取的值 husky
        result = deviceInfoData.HARDWARE;
    } else if (key == 'ro.product.board') {//Build获取的值 husky
        result = deviceInfoData.BOARD;
    } else if (key == 'ro.product.manufacturer') {//Google userData.MANUFACTURER
        result = deviceInfoData.MANUFACTURER;
    } else if (key == 'ro.product.model') {//Pixel 8 Pro 
        result = deviceInfoData.MODEL;
    } else if (key == 'ro.secure') {//1代表正常设备，0代表可调试设备       不使用sdk值只hook 值为1
        result = 1;
    } else if (key == 'ro.allow.mock.location') {//能否模拟位置信息  不使用sdk值只hook  值为0
        result = 0;
    } else if (key == 'ro.build.user') {//能否模拟位置信息 userData.USER
        result = userInfoData.USER;
    } else if (key == 'gsm.sim.operator.iso-country') {//SDK值（运营商国家二字码）如sg
        result = userInfoData.getSimCountryIso;
    }
    return result;
}
//TODO dalvik.vm.isa.arm64.variant  dalvik.vm.isa.arm.variant
function checkNeedHookBySystemPropertiesKey(key) {
    if (key === 'gsm.operator.alpha'//SDK值（运营商名称）如StarHub
        || key == 'gsm.sim.operator.alpha'//SDK值（运营商名称）如StarHub
        || key == 'gsm.operator.numeric'//SDK值（运营商编码）如52505
        || key == 'gsm.sim.operator.numeric'//SDK值（运营商编码）如52505
        || key == 'gsm.version.baseband'//Build获取的值，getRadioVersion 如g5300i-241121-241122-B-12698486,g5300i-241121-241122-B-12698486
        || key == 'ro.board.platform'//Build获取的值rk3588
        || key == 'ro.build.display.id'//Build获取的值BP11.241121.010  userData.DISPLAY
        || key == 'ro.build.flavor'//Build获取的值 husky_beta-user
        || key == 'ro.hardware'//Build获取的值 husky
        || key == 'ro.product.board'//Build获取的值 husky
        || key == 'ro.product.manufacturer'//Google userData.MANUFACTURER
        || key == 'ro.product.model'//Pixel 8 Pro  
        || key == 'ro.secure'//1代表正常设备，0代表可调试设备       不使用sdk值只hook 值为1
        || key == 'ro.allow.mock.location'//能否模拟位置信息  不使用sdk值只hook  值为0
        || key == 'ro.build.user'//能否模拟位置信息 userData.USER
        || key == 'gsm.sim.operator.iso-country'//SDK值（运营商国家二字码）如sg
    ) {
        return true;
    }
    return false;
}
/**
 * 监听目标App行为- 设备信息相关 
 */
function hookSystemProperties() {
    var userInfoData = USER_DATA
    var deviceInfoData = DEVICE_DATA
    try {
        var SystemProperties = Java.use('android.os.SystemProperties');
        SystemProperties.get.overload('java.lang.String').implementation = function (key) { //  todo by wzy sdk+hook
            // sys.usb.config 获取 USB 配置属性 包含 USB调试 判断设备是否启用了 ADB 调试（USB 或 TCP） 包含：meid，Build相关返回值
            var result = this.get(key);
            if (checkNeedHookBySystemPropertiesKey(key) && userInfoData != null) {
                result = getHookValueBySystemPropertiesKey(key, userInfoData, deviceInfoData);
            }
            if (result != null) {
                result = result.toString();
            }
            logToFile("SystemProperties.get(),获取的key=" + key + "\t返回值为=" + this.get(key));
            return result;
        };
        SystemProperties.get.overload('java.lang.String', 'java.lang.String').implementation = function (key, def) {    //  todo by wzy sdk+hook
            // 包含：meid，Build相关返回值
            var result = this.get(key, def);
            if (checkNeedHookBySystemPropertiesKey(key) && userInfoData != null) {
                result = getHookValueBySystemPropertiesKey(key, userInfoData, deviceInfoData);
                if (result == null) {
                    result = def;
                }
            }
            if (result != null) {
                result = result.toString();
            }
            logToFile("SystemProperties.get(2),获取的key=" + key + "\t返回值为=" + this.get(key, def) + " result===" + result);
            return result;
        };
    } catch (e) {
        notifyError(e);
    }

    try {
        // Hook System.getProperty 方法
        var System = Java.use("java.lang.System");
        System.getProperty.overload('java.lang.String').implementation = function (key) {   //  todo by wzy sdk+hook
            var result = this.getProperty(key);
            if (key === "http.agent") {
                if (userInfoData.http_agent != null) {
                    return userInfoData.http_agent;
                }
            }
            logToFile("System.getProperty,,key=" + key + "\tvalue=" + result + "  原始值为=" + this.getProperty(key));
            return result;
        };
    } catch (e) {
        notifyError(e); //  System.getProperty
    }
    try {
        const VMRuntime = Java.use("dalvik.system.VMRuntime");//待添加。
        if (VMRuntime.getTargetSdkVersion) {
            VMRuntime.getTargetSdkVersion.overload().implementation = function () { //  todo by wzy sdk+hook
                let result = this.getTargetSdkVersion();
                logToFile("[*] VMRuntime.getTargetSdkVersion() 被调用,    Target SDK Version: " + result);
                return result;
            };
        }
    } catch (e) {
        notifyError(e);  // VMRuntime 类
    }

}

function hookSystemClock(userInfoData) {//hook系统时间
    var macroInfoData = MACRO_DATA;
    var System = Java.use("java.lang.System");
    try {
        var SystemClock = Java.use('android.os.SystemClock');
        SystemClock.elapsedRealtime.implementation = function () {  //  todo by wzy 不加sdk,加hook  //TODO by ly 加SDK，加Hook
            if (macroInfoData != null && macroInfoData.SystemClock != null) {
                var value = macroInfoData.SystemClock.elapsedRealtime + System.currentTimeMillis() - initJSTime;
                logToFile("[*] SystemClock.elapsedRealtime: " + value);
                return value;
            }
            var originalElapsedTime = this.elapsedRealtime();
            logToFile("[*] SystemClock.elapsedRealtime返回原始值: " + originalElapsedTime);
            return originalElapsedTime;
        };
        SystemClock.elapsedRealtimeNanos.implementation = function () {  //  todo by wzy 不加sdk,加hook  //TODO by ly 加SDK，加Hook
            if (macroInfoData != null && macroInfoData.SystemClock != null) {
                var value = macroInfoData.SystemClock.elapsedRealtimeNanos + System.nanoTime() - initJSNMTime;
                logToFile("[*] SystemClock.elapsedRealtimeNanos: " + value);
                return value;
            }
            var elapsedRealtimeNanos = this.elapsedRealtimeNanos();
            logToFile("[*] SystemClock.elapsedRealtimeNanos返回原始值: " + elapsedRealtimeNanos);
            return elapsedRealtimeNanos;
        };
        SystemClock.uptimeMillis.overload().implementation = function () { //  todo by wzy 不加sdk,加hook  //TODO by ly 加SDK，加Hook
            if (macroInfoData != null) {
                var value = macroInfoData.SystemClock.uptimeMillis + System.currentTimeMillis() - initJSTime;
                logToFile("[*] SystemClock.uptimeMillis: " + value);
                return value;
            }
            var originalResult = this.uptimeMillis();
            logToFile("[*] SystemClock.uptimeMillis返回原始值: " + originalResult);
            return originalResult;
        };
    } catch (e) {
        notifyError(e); //  SystemClock
    }
    try {
        var Runtime = Java.use('java.lang.Runtime');
        // Hook maxMemory 方法 当前应用可用堆内存上限
        Runtime.maxMemory.overload().implementation = function () { //  todo 需要SDK，有可能需要adb修改
            // 调用原始的 maxMemory 方法
            var maxMemoryValue = this.maxMemory();
            if (userInfoData != null && userInfoData.maxMemory != null) {
                maxMemoryValue = userInfoData.maxMemory;
            }
            // 打印最大堆内存详情信息
            logToFile("[*] 当前应用最大堆内存: " + maxMemoryValue / (1024 * 1024) + " MB");
            return maxMemoryValue;
        };
    } catch (e) {
        notifyError(e)
    }
}
/**
 * 获取三方 id 
 */
function hookGAIDAndAppSetId() {
    var userInfoData = USER_DATA;
    var macroInfoData = MACRO_DATA;
    try {
        // 尝试获取 AdvertisingIdClient$Info 类 adid GAID
        var AdvertisingIdClientInfo = Java.use('com.google.android.gms.ads.identifier.AdvertisingIdClient$Info');
        AdvertisingIdClientInfo.$init.overload('java.lang.String', 'boolean').implementation = function (id, flag) {  //TODO by ly SDK+Hook
            logToFile('AdvertisingIdClient.Info constructed with ID: ' + id + ' and limitAdTracking: ' + flag);
            if (userInfoData != null) {
                id = userInfoData.GAID;
                flag = userInfoData.isLimitAdTrackingEnabled;
            }
            return this.$init(id, flag);
        };

        // Hook getId 方法来获取广告 ID。
        AdvertisingIdClientInfo.getId.implementation = function () {          //TODO by ly SDK+Hook
            var id = this.getId();
            if (userInfoData != null) {
                id = userInfoData.GAID;
            }
            logToFile('AdvertisingIdClient.Info.getId() returned: ' + id);
            return id;
        };
    } catch (e) {
        notifyError(e);
    }

    // Hook Google Play Referrer API
    try {
        // 安装引荐来源信息的保留期限为 90 天，除非用户重新安装应用，否则这些信息不会发生变化
        const InstallReferrerClientImpl = Java.use("com.android.installreferrer.api.InstallReferrerClientImpl");
        const ReferrerDetails = Java.use('com.android.installreferrer.api.ReferrerDetails');

        // 获取引荐来源信息（如 UTM 参数）
        if (InstallReferrerClientImpl && InstallReferrerClientImpl.getInstallReferrer) {
            InstallReferrerClientImpl.getInstallReferrer.implementation = function () {                         //TODO by ly hook
                var referrerDetails = this.getInstallReferrer();
                var referrer = referrerDetails.getInstallReferrer().value;
                logToFile('InstallReferrerClientImpl install referrer: ' + referrer);
                return referrerDetails;
            };
        }

        // 获取广告点击的时间戳
        if (ReferrerDetails && ReferrerDetails.getReferrerClickTimestampSeconds) {
            ReferrerDetails.getReferrerClickTimestampSeconds.overload().implementation = function () {      //TODO by ly hook
                let timestamp = this.getReferrerClickTimestampSeconds();
                logToFile("[*] ReferrerDetails.getReferrerClickTimestampSeconds 广告点击时间戳（秒）: " + timestamp);
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    timestamp = macroInfoData.ReferrerDetails.getReferrerClickTimestampSeconds;
                }
                return timestamp;
            };
        }

        // 获取安装开始的时间戳
        if (ReferrerDetails && ReferrerDetails.getInstallBeginTimestampSeconds) {
            ReferrerDetails.getInstallBeginTimestampSeconds.overload().implementation = function () {      //TODO by ly hook
                let timestamp = this.getInstallBeginTimestampSeconds();
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    timestamp = macroInfoData.ReferrerDetails.getInstallBeginTimestampSeconds;
                }
                logToFile("[*] ReferrerDetails.getInstallBeginTimestampSeconds 安装开始时间戳（秒）: " + timestamp);
                return timestamp;
            };
        }

        // 获取广告点击的时间戳
        if (ReferrerDetails && ReferrerDetails.getReferrerClickTimestampServerSeconds) {
            ReferrerDetails.getReferrerClickTimestampServerSeconds.overload().implementation = function () {      //TODO by ly hook
                let timestamp = this.getReferrerClickTimestampServerSeconds();
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    timestamp = macroInfoData.ReferrerDetails.getReferrerClickTimestampServerSeconds;
                }
                logToFile("[*] ReferrerDetails.getReferrerClickTimestampServerSeconds 广告点击时间戳（秒）: " + timestamp);
                return timestamp;
            };
        }

        // 获取安装开始的时间戳
        if (ReferrerDetails && ReferrerDetails.getInstallBeginTimestampServerSeconds) {
            ReferrerDetails.getInstallBeginTimestampServerSeconds.overload().implementation = function () {      //TODO by ly hook
                let timestamp = this.getInstallBeginTimestampServerSeconds();
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    timestamp = macroInfoData.ReferrerDetails.getInstallBeginTimestampServerSeconds;
                }
                logToFile("[*] ReferrerDetails.getInstallBeginTimestampServerSeconds安装开始时间戳（秒）: " + timestamp);
                return timestamp;
            };
        }

        // 获取安装版本
        if (ReferrerDetails && ReferrerDetails.getInstallVersion) {
            ReferrerDetails.getInstallVersion.overload().implementation = function () {      //TODO by ly hook
                let installVersion = this.getInstallVersion();
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    installVersion = macroInfoData.ReferrerDetails.getInstallVersion;
                }
                logToFile("[*] ReferrerDetails.getInstallVersion获取安装版本: " + installVersion);
                return installVersion;
            };
        }
        if (ReferrerDetails && ReferrerDetails.getInstallReferrer) {
            ReferrerDetails.getInstallReferrer.overload().implementation = function () {      //TODO by ly hook
                let referrer = this.getInstallReferrer();
                if (macroInfoData != null && macroInfoData.ReferrerDetails != null) {
                    referrer = macroInfoData.ReferrerDetails.getInstallReferrer;
                }
                logToFile("[*] ReferrerDetails.getInstallReferrer 获取安装引荐来源: " + referrer);
                return referrer;
            }
        }
    } catch (e) {
        notifyError(e); // Google Play Referrer API BroadcastReceiver(INSTALL_REFERRER)
    }

    try {
        // TODO 不SDK, 需要Hook 
        var AppSetIdInfo = Java.use("com.google.android.gms.appset.AppSetIdInfo");
        if (AppSetIdInfo.getId) {
            AppSetIdInfo.getId.overload().implementation = function () {
                var result = this.getId();
                if (macroInfoData != null && macroInfoData.AppSetIdInfo != null) {
                    result = macroInfoData.AppSetIdInfo.ID;
                }
                logToFile(`[*] 应用读取Google应用集ID AppSetIdInfo.getId` + " result=" + result);
                return result;
            }
        }
    } catch (e) {
        notifyError(e);
    }
}

//hook 自定义按下坐标
function hookTouchUtils() {
    try {
        var TouchUtils = Java.use("com.example.why.TouchUtils");//TODO 需要制定app包名以及路径。
        if (TouchUtils.checkMultiTouchSupport) {
            TouchUtils.checkMultiTouchSupport.overload('android.content.Context').implementation = function (ctx) {
                // var result = this.checkMultiTouchSupport(ctx);
                logToFile("[*] 应用调用TouchUtils.checkMultiTouchSupport()  ctx=" + ctx);
                this.checkMultiTouchSupport(ctx);
            }
        }
        if (TouchUtils.getPointerCount) {
            TouchUtils.getPointerCount.overload('android.view.MotionEvent').implementation = function (motionEvent) {
                var result = this.getPointerCount(motionEvent);
                logToFile("[*] 应用调用TouchUtils.getPointerCount()  result=" + result + "  motionEvent=" + motionEvent);
                return result;
            }
        }
        if (TouchUtils.listenTouchPoint) {
            TouchUtils.listenTouchPoint.implementation = function (view, listener) {
                // 原始调用
                this.listenTouchPoint(view, listener);
                logToFile("[*] 应用调用TouchUtils.listenTouchPoint()" + view);
            }

        }
        if (TouchUtils.getMaxTouchPointsFromInputDevice) {
            TouchUtils.getMaxTouchPointsFromInputDevice.overload().implementation = function () {
                var result = this.getMaxTouchPointsFromInputDevice();
                logToFile("[*] 应用调用TouchUtils.getMaxTouchPointsFromInputDevice()  result=" + result);
                return result;
            }
        }
    } catch (e) {
        notifyError(e)
    }
}


function checkNeedHookBySettings_SecureIntKey(key) {
    if (key === 'accessibility_display_magnification_enabled'//仅hook  值为0
        || key == 'hush_gesture_used'//仅hook  值为0
        || key == 'touch_exploration_enabled'//仅hook  值为0
        || key == 'high_text_contrast_enabled'//仅hook  值为0
        || key == 'accessibility_captioning_enabled'//仅hook  值为0
        || key == 'accessibility_captioning_preset'//仅hook  值为0
        || key == 'accessibility_enabled'//hook 值为 0
        // || key == 'stylus_handwriting_enabled'//SDK值 0
        || key == 'adb_enabled'//hook值为 0
    ) {
        return true;
    }
    return false;
}


function hookFileInputStream() {
    try {
        // Hook FileInputStream 构造函数
        var FileInputStream = Java.use("java.io.FileInputStream");
        FileInputStream.$init.overload('java.lang.String').implementation = function (filePath) {   //  todo by wzy sdk+hook  TODO by ly sdk获取/proc/cpuinfo, /proc/meminfo, /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq，/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq。只抓取8核手机，各核(0-7)的cpuinfo_max_freq、cpuinfo_min_freq。不需要hook，用linux挂载。
            logToFile('FileInputStream 打开：' + filePath);
            if (filePath != null && filePath == SU_PATH) {
                const FileNotFoundException = Java.use('java.io.FileNotFoundException');
                throw FileNotFoundException.$new(
                    filePath + ": open failed: EACCES (Permission denied)"
                );
            }
            return this.$init(filePath);
        };
        // Hook 通过 File 对象初始化的构造函数
        FileInputStream.$init.overload('java.io.File').implementation = function (file) {  // todo by wzy 不加sdk，加hook
            var filePath = file.getPath();
            logToFile('使用 File 对象打开 FileInputStream：' + filePath);
            if (filePath != null && filePath == SU_PATH) {
                const FileNotFoundException = Java.use('java.io.FileNotFoundException');
                throw FileNotFoundException.$new(
                    filePath + ": open failed: EACCES (Permission denied)"
                );
            }
            return this.$init(file);
        };

        // Hook 通过 FileDescriptor 对象初始化的构造函数
        FileInputStream.$init.overload('java.io.FileDescriptor').implementation = function (fd) {   //  todo by wzy 不加sdk，加hook
            logToFile('使用 FileDescriptor 打开的 FileInputStream： ' + fd);
            return this.$init(fd);
        };
    } catch (e) {
        notifyError(e);
    }

}
// 监听目标App行为- 屏幕相关  
function hookDisplay(userInfoData, ArrayList) {
    const Display = Java.use('android.view.Display');
    var Resources = Java.use("android.content.res.Resources");
    var BuildVersion = Java.use('android.os.Build$VERSION');
    var Rect = Java.use('android.graphics.Rect');
    var SDK_INT = parseInt(BuildVersion.SDK_INT.value);
    try {
        Resources.getDisplayMetrics.overload().implementation = function () {//疯狂打印，所以注释。
            var metrics = this.getDisplayMetrics();
            if (metrics != null && userInfoData != null && userInfoData.getDisplayMetrics != null) {
                metrics.widthPixels.value = userInfoData.getDisplayMetrics.widthPixels;
                metrics.heightPixels.value = userInfoData.getDisplayMetrics.heightPixels;
                metrics.densityDpi.value = userInfoData.getDisplayMetrics.densityDpi;
                metrics.xdpi.value = userInfoData.getDisplayMetrics.xdpi;
                metrics.ydpi.value = userInfoData.getDisplayMetrics.ydpi;
                metrics.scaledDensity.value = userInfoData.getDisplayMetrics.scaledDensity;
                metrics.density.value = userInfoData.getDisplayMetrics.density;
            }
            return metrics;
        };

        Resources.getConfiguration.overload().implementation = function () {
            try {
                var config = this.getConfiguration();
                if (IS_DEBUG) {
                    // var rawScreenLayout = config.screenLayout.value;
                    // var parsedRaw = parseScreenLayout(rawScreenLayout);
                    // parsedRaw.sizeCode = rawScreenLayout;
                    // logToFile("\n【原始 Configuration】");
                    // logToFile("    screenLayout: " + describeScreenLayout(parsedRaw));
                    // logToFile("    smallestScreenWidthDp: " + config.smallestScreenWidthDp.value);
                    // logToFile("    densityDpi: " + describeDensity(config.densityDpi.value));
                    // logToFile("    fontScale: " + config.fontScale.value);
                    // logToFile("    locale: " + config.locale.value.toString());
                }
                // 修改 Configuration 字段
                if (userInfoData != null && userInfoData.getConfiguration != null) {
                    var Configuration = userInfoData.getConfiguration;
                    config.mnc.value = Configuration.mnc;
                    config.mcc.value = Configuration.mcc;
                    config.colorMode.value = Configuration.colorMode;
                    config.densityDpi.value = Configuration.densityDpi;
                    config.navigation.value = Configuration.navigation;
                    config.navigationHidden.value = Configuration.navigationHidden;
                    config.screenHeightDp.value = Configuration.screenHeightDp;
                    config.screenWidthDp.value = Configuration.screenWidthDp;
                    config.smallestScreenWidthDp.value = Configuration.smallestScreenWidthDp;
                }

            } catch (e) {
                notifyError(e); //   Resources.getConfiguration
            }
            return config;
        };

        Display.getMetrics.overload('android.util.DisplayMetrics').implementation = function (metrics) {    //  todo by wzy sdk+hook
            logToFile("[*] Display.getMetrics(DisplayMetrics) 被调用（全局 Hook）");
            this.getMetrics(metrics);
            if (userInfoData != null && userInfoData.getMetrics != null) {
                metrics.widthPixels.value = userInfoData.getMetrics.widthPixels;
                metrics.heightPixels.value = userInfoData.getMetrics.heightPixels;
                metrics.densityDpi.value = userInfoData.getMetrics.densityDpi;
                metrics.xdpi.value = userInfoData.getMetrics.xdpi;
                metrics.ydpi.value = userInfoData.getMetrics.ydpi;
                metrics.scaledDensity.value = userInfoData.getMetrics.scaledDensity;
                metrics.density.value = userInfoData.getMetrics.density;
            }
        };

        if (Display.getRefreshRate) {
            Display.getRefreshRate.implementation = function () {   //  todo by wzy sdk+hook
                const originalValue = this.getRefreshRate();
                if (userInfoData != null) {
                    return userInfoData.getRefreshRate;
                }
                logToFile(`原始的getRefreshRate:==${originalValue}`);
                return originalValue;
            };
        }
        if (Display.getWidth) {
            Display.getWidth.implementation = function () { // todo by wzy sdk+hook
                const originalValue = this.getWidth();
                if (userInfoData != null) {
                    logToFile(`原始的 getWidth: ${originalValue} 修改后的=${userInfoData.getWidth}`);
                    return userInfoData.getWidth;
                }
                return originalValue;
            };
        }
        if (Display.getHeight) {
            Display.getHeight.implementation = function () {    // todo by wzy sdk+hook
                const originalValue = this.getHeight();
                if (userInfoData != null) {
                    logToFile(`原始的 getHeight: ${originalValue} 修改后的=${userInfoData.getHeight}`);
                    return userInfoData.getHeight;
                }
                return originalValue;
            };
        }
        if (Display.getRealSize) {
            Display.getRealSize.overload('android.graphics.Point').implementation = function (point) { // todo by wzy sdk+hook，需要测试SDK获取的值对不对
                this.getRealSize(point);
                const originalX = point.x.value;
                const originalY = point.y.value;
                if (userInfoData != null) {
                    point.x.value = userInfoData.getRealSize.x;
                    point.y.value = userInfoData.getRealSize.y;
                }
                logToFile(`原始的 getRealSize: (${originalX}, ${originalY}) 修改以后的(${point.x.value}, ${point.y.value})`);
                // return point;//无需返回值。
            };
        }
        if (Display.getSize) {
            Display.getSize.overload('android.graphics.Point').implementation = function (point) {  //   todo by wzy sdk+hook。需要测试SDK获取的值对不对
                this.getSize(point);
                const originalX = point.x.value;
                const originalY = point.y.value;
                if (userInfoData != null) {
                    point.x.value = userInfoData.getSize.x;
                    point.y.value = userInfoData.getSize.y;
                }
                logToFile(`原始的 getSize: (${originalX}, ${originalY}) 修改以后的(${point.x.value}, ${point.y.value})`);
                // return point;
            };
        }
        if (Display.getHdrCapabilities) {
            Display.getHdrCapabilities.implementation = function () {//  todo by wzy sdk+hook //TODO by ly 这块是不是要封装返回对象？
                var result = this.getHdrCapabilities();
                if (userInfoData != null) {
                    result.mMaxLuminance.value = userInfoData.getHdrCapabilities.getDesiredMaxLuminance;
                    result.mMinLuminance.value = userInfoData.getHdrCapabilities.getDesiredMinLuminance;
                    result.mMaxAverageLuminance.value = userInfoData.getHdrCapabilities.getDesiredMaxAverageLuminance;
                    result.mSupportedHdrTypes.value = userInfoData.getHdrCapabilities.getSupportedHdrTypes;
                }
                logToFile("[*] 调用了 Display.getHdrCapabilities(), result=" + result);
                return result;
            }
        }
        hookHdrCapabilities(userInfoData);
        // if (Display.getMode) {
        //     Display.getMode.implementation = function () { //  todo by wzy sdk+hook  //TODO by ly 这块是不是要封装返回对象？
        //         var result = this.getMode();
        //         try {
        //             if (userInfoData != null && result != null && userInfoData.getMode != null) {
        //                 result.mWidth.value = userInfoData.getMode.getPhysicalWidth;
        //                 result.mHeight.value = userInfoData.getMode.getPhysicalHeight;
        //                 result.mRefreshRate.value = userInfoData.getMode.getRefreshRate;
        //                 if (SDK_INT >= 31) {
        //                     result.mAlternativeRefreshRates.value = userInfoData.getMode.getAlternativeRefreshRates;
        //                 }
        //                 if (SDK_INT >= 34) {
        //                     result.mSupportedHdrTypes.value = userInfoData.getMode.getSupportedHdrTypes;
        //                 }
        //             }
        //             logToFile("[*] 调用了 Display.getMode(), result=" + result + "  data==" + userInfoData.getMode);
        //         } catch (e) {
        //             notifyError(e);
        //         }
        //         return result;
        //     }
        // }
        // hookMode(userInfoData)
    } catch (e) {
        notifyError(e); //  windowManager、 Resources.getDisplayMetrics、Display.getMetrics
    }

    // try {
    //     var WindowInsets = Java.use('android.view.WindowInsets');
    //     if (WindowInsets.getDisplayCutout) {
    //         WindowInsets.getDisplayCutout.implementation = function () {    //   todo by wzy sdk+hook
    //             var cutout = this.getDisplayCutout();
    //             if (cutout != null) {
    //                 logToFile("[*] WindowInsets.getDisplayCutout() WindowInsets 中包含裁剪区域");
    //             } else {
    //                 logToFile("[*] WindowInsets.getDisplayCutout() WindowInsets 中未发现裁剪区域");
    //             }
    //             return cutout;
    //         };
    //     }
    // } catch (e) {
    //     notifyError(e); //  WindowInsets
    // }
    if (Java.available && SDK_INT >= 28) {
        hookDisplayCutout(userInfoData, Rect, ArrayList)
    }
    try {
        //监听WindowMetrics 获取宽高。
        var WindowMetrics = Java.use("android.view.WindowMetrics");
        if (WindowMetrics.getBounds) {
            WindowMetrics.getBounds.overload().implementation = function () {  //  todo by wzy sdk+hook
                var result = this.getBounds();
                if (userInfoData != null && userInfoData.windowMetrics_getBounds != null) {
                    var sdkData = userInfoData.windowMetrics_getBounds;
                    result.left.value = sdkData.left
                    result.right.value = sdkData.right
                    result.bottom.value = sdkData.bottom
                    result.top.value = sdkData.top
                }
                logToFile(`[*] 应用读取WindowMetrics.getBounds` + " result=" + result + "  原始数据=" + this.getBounds());
                return result;
            }
        }
    } catch (e) {
        notifyError(e)
    }
    hookWindowInsetsType(userInfoData);
};

//hookStatFs
function hookStatFs(userInfoData) {
    function getStatFsByPath(path) {
        var statfs = null
        if (userInfoData != null && path != null) {
            if (path.startsWith('/storage/emulated/0/')) {
                statfs = userInfoData.storageEmulatedStatFs;
            } else if (path.startsWith('/data')) {
                statfs = userInfoData.dataStatFs;
            } else if (path.startsWith('/storage')) {
                statfs = userInfoData.storageStatFs;
            }
        }
        return statfs
    }
    try {
        // Hook StatFs 获取文件系统统计信息的类，通常用来查询存储设备的空间信息（如总块数、可用块数、块大小等）
        const StatFs = Java.use("android.os.StatFs");
        const pathMap = new Map();
        StatFs.$init.overload('java.lang.String').implementation = function (path) {    // todo by wzy sdk获取/data和/storage/emulated/0磁盘大小信息 + hook时加pathMap，实际返回值在后面的代码hook
            if (pathMap.size >= 300) {
                pathMap.clear();
            }
            pathMap.set(this.hashCode(), path);
            logToFile("[*] StatFs 构造函数被调用, 路径: " + path + "  pathMap.has==" + pathMap.has(this.hashCode()));
            logToConsole("[*][frida缓存]pathMap的大小==" + pathMap.size);
            return this.$init(path);
        };

        // Hook getBlockCount 方法 文件系统中的总块数
        if (StatFs.getBlockCount) {
            StatFs.getBlockCount.overloads.forEach(function (overload) {            //TODO by ly sdk+Hook
                overload.implementation = function () {
                    const result = this.getBlockCount.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        return sdkData.getBlockCount;
                    }
                    logToFile(`[*] StatFs.getBlockCount() 返回值: ${result}  path=${path}`);
                    return result;
                };
            });
        }

        // Hook getBlockCountLong 方法（适用于 API 18 及以上） 与 getBlockCount() 类似，但返回类型为 long，适用于处理更大的存储空间
        if (StatFs.getBlockCountLong) {
            StatFs.getBlockCountLong.overloads.forEach(function (overload) { // todo by wzy sdk+hook
                overload.implementation = function () {
                    const result = this.getBlockCountLong.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        return sdkData.getBlockCountLong;
                    }
                    logToFile(`[*] StatFs.getBlockCountLong() 返回值: ${result}  path=${path}`);
                    return result;
                };
            });
        }

        // Hook getBlockSize 方法 每个块的大小（以字节为单位）
        if (StatFs.getBlockSize) {
            StatFs.getBlockSize.overloads.forEach(function (overload) { // todo by wzy sdk+hook
                overload.implementation = function () {
                    const result = this.getBlockSize.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        return sdkData.getBlockSize;
                    }
                    logToFile(`[*] StatFs.getBlockSize() 返回值: ${result}  path=${path}`);
                    return result;
                };
            });
        }

        // Hook getBlockSizeLong 方法（适用于 API 18 及以上）与 getBlockSize() 类似，但返回类型为 long，适用于处理更大的存储空间
        if (StatFs.getBlockSizeLong) {
            StatFs.getBlockSizeLong.overloads.forEach(function (overload) { //TODO by ly sdk+hook
                overload.implementation = function () {
                    const result = this.getBlockSizeLong.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        return sdkData.getBlockSizeLong;
                    }
                    logToFile(`[*] StatFs.getBlockSizeLong() 返回值: ${result}  path=${path}`);
                    return result;
                };
            });
        }

        // Hook getAvailableBlocks 方法 文件系统中可用的块数
        if (StatFs.getAvailableBlocks) {
            StatFs.getAvailableBlocks.overloads.forEach(function (overload) { // todo by wzy sdk+hook
                overload.implementation = function () {
                    const result = this.getAvailableBlocks.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        logToFile(`[*] StatFs.getAvailableBlocks() 返回值: ${sdkData.getAvailableBlocks}  path=${path}`);
                        return sdkData.getAvailableBlocks;
                    }
                    return result;
                };
            });
        }

        // Hook getAvailableBlocksLong 方法（适用于 API 18 及以上） 与 getAvailableBlocks() 类似，但返回类型为 long，适用于处理更大的存储空间
        if (StatFs.getAvailableBlocksLong) {
            StatFs.getAvailableBlocksLong.overloads.forEach(function (overload) { // todo by wzy sdk+hook
                overload.implementation = function () {
                    const result = this.getAvailableBlocksLong.apply(this, arguments);
                    const path = pathMap.get(this.hashCode());
                    var sdkData = getStatFsByPath(path);
                    if (sdkData != null) {
                        logToFile(`[*] StatFs.getAvailableBlocksLong() 返回值: ${sdkData.getAvailableBlocksLong}  path=${path}`);
                        return sdkData.getAvailableBlocksLong;
                    }
                    return result;
                };
            });
        }
        // Hook getFreeBlocks 文件系统中空闲块的总数
        if (StatFs.getFreeBlocks) {
            StatFs.getFreeBlocks.overload().implementation = function () {              //TODO by ly hook
                const result = this.getFreeBlocks();
                const path = pathMap.get(this.hashCode());
                var sdkData = getStatFsByPath(path);
                if (sdkData != null) {
                    logToFile(`[*] StatFs.getFreeBlocks() 返回值: ${sdkData.getFreeBlocks}  path=${path}`);
                    return sdkData.getFreeBlocks;
                }
                return result;
            };
        }
        if (StatFs.getFreeBlocksLong) {
            StatFs.getFreeBlocksLong.overload().implementation = function () {          //TODO by ly sdk+hook
                const result = this.getFreeBlocksLong();
                const path = pathMap.get(this.hashCode());
                var sdkData = getStatFsByPath(path);
                if (sdkData != null) {
                    logToFile(`[*] StatFs.getFreeBlocksLong() 返回值: ${sdkData.getFreeBlocksLong}  path=${path}`);
                    return sdkData.getFreeBlocksLong;
                }
                return result;
            }
        }
        if (StatFs.getFreeBytes) {
            StatFs.getFreeBytes.overload().implementation = function () {               //TODO by ly hook
                const result = this.getFreeBytes();
                const path = pathMap.get(this.hashCode());
                var sdkData = getStatFsByPath(path);
                if (sdkData != null) {
                    var resultSDK = sdkData.getFreeBytes + getRandomByteSize();
                    logToFile(`[*] StatFs.getFreeBytes() 返回值: ${resultSDK}  path=${path}`);
                    return resultSDK;
                }
                return result;
            }
        }
        if (StatFs.getTotalBytes) {
            StatFs.getTotalBytes.overload().implementation = function () {              //TODO by ly hook
                const result = this.getTotalBytes();
                const path = pathMap.get(this.hashCode());
                var sdkData = getStatFsByPath(path);
                if (sdkData != null) {
                    logToFile(`[*] StatFs.getTotalBytes() 返回值: ${sdkData.getTotalBytes}  path=${path}`);
                    return sdkData.getTotalBytes;
                }
                return result;
            }
        }

        if (StatFs.getAvailableBytes) {
            StatFs.getAvailableBytes.overload().implementation = function () {      //TODO by ly SDK+Hook
                const result = this.getAvailableBytes();
                const path = pathMap.get(this.hashCode());
                var sdkData = getStatFsByPath(path);
                if (sdkData != null) {
                    var resultSDK = sdkData.getAvailableBytes + getRandomByteSize();
                    logToFile(`[*] StatFs.getAvailableBytes() 返回值: ${resultSDK}  path=${path}`);
                    return resultSDK;
                }
                return result;
            }
        }
    } catch (e) {
        notifyError(e) //  StatFs
    }
}

//hookTelephonyManagerr
function hookTelephonyManager(userInfoData) {
    try {
        var TelephonyManager = Java.use("android.telephony.TelephonyManager");
        // 安全打印工具
        function safeToString(obj) {
            if (obj === null || obj === undefined) {
                return String(obj);
            }
            if (typeof obj.toString === 'function') {
                try {
                    return obj.toString();
                } catch (e) {
                    return "[将对象转换为字符串时出错]";
                }
            } else {
                return "[对象没有toString方法]";
            }
        }

        //获取底层 ITelephony 接口（用于拨打电话等操作）
        if (TelephonyManager.getITelephony) {
            TelephonyManager.getITelephony.implementation = function () {   //TODO by ly 敏感信息不sdk，但内部测试需要确认下返回的是什么？再hook它 0811 反编译无人获取
                try {
                    var result = this.getITelephony();
                    if (result !== null && typeof result != 'undefined') {
                        // 安全访问 getClass 和 getName
                        var className = "未知";
                        try {
                            className = result.getClass().getName();
                        } catch (e) {
                            className = "无法获取类名 (" + e.message + ")";
                        }

                        logToFile("[*] TelephonyManager.getITelephony()返回值类型: " + className);
                    } else {
                        logToFile("[!] TelephonyManager.getITelephony() 返回值为空 (null 或 undefined)");
                    }

                    return result;
                } catch (e) {
                    notifyError(e);; // TelephonyManager.getITelephony()
                    return this.getITelephony(); // 出错时仍返回原始值以避免崩溃
                }
            };
        }

        //获取 SIM 卡所在国家的 ISO 编码（如 CN、US）
        if (TelephonyManager.getSimCountryIso.overload()) {  // todo by wzy sdk+hook
            TelephonyManager.getSimCountryIso.overload().implementation = function () {
                var result = this.getSimCountryIso();
                if (userInfoData != null) {
                    result = userInfoData.getSimCountryIso;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimCountryIso()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getSimCountryIso.overload('int')) { // todo by wzy sdk+hook
            TelephonyManager.getSimCountryIso.overload('int').implementation = function (subId) {
                var result = this.getSimCountryIso(subId);
                if (userInfoData != null) {
                    result = userInfoData.getSimCountryIso;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimCountryIso(int)，返回值: " + safeToString(result));
                return result;
            };
        }

        //       获取 SIM 卡运营商名称（如中国移动、中国电信）运营商类型
        if (TelephonyManager.getSimOperator.overload()) {   //  todo by wzy sdk+hook
            TelephonyManager.getSimOperator.overload().implementation = function () {
                var result = this.getSimOperator();
                if (userInfoData != null) {
                    result = userInfoData.getSimOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperator()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getSimOperator.overload('int')) {  //  todo by wzy sdk+hook
            TelephonyManager.getSimOperator.overload('int').implementation = function (subId) {
                var result = this.getSimOperator(subId);
                if (userInfoData != null) {
                    result = userInfoData.getSimOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperator(int)，返回值: " + safeToString(result));
                return result;
            };
        }

        //       获取 SIM 卡运营商编号(MCC+MNC, 例如 46000)
        if (TelephonyManager.getSimOperatorNumeric.overload()) {    //  todo by wzy sdk+hook
            TelephonyManager.getSimOperatorNumeric.overload().implementation = function () {
                var result = this.getSimOperatorNumeric();
                if (userInfoData != null) {
                    result = userInfoData.getSimOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorNumeric()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getSimOperatorNumeric.overload('int')) {   // todo by wzy sdk+hook
            TelephonyManager.getSimOperatorNumeric.overload('int').implementation = function (subId) {
                var result = this.getSimOperatorNumeric(subId);
                if (userInfoData != null) {
                    result = userInfoData.getSimOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorNumeric(int)，返回值: " + safeToString(result));
                return result;
            };
        }
        // 获取 SIM 卡运营商显示名称（运营商品牌名）
        if (TelephonyManager.getSimOperatorName.overload()) {   //  todo by wzy sdk+hook
            TelephonyManager.getSimOperatorName.overload().implementation = function () {
                var result = this.getSimOperatorName();
                if (userInfoData != null) {
                    result = userInfoData.getSimOperatorName;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorName()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getSimOperatorName.overload('int')) {//  todo by wzy sdk+hook
            TelephonyManager.getSimOperatorName.overload('int').implementation = function (subId) {
                var result = this.getSimOperatorName(subId);
                if (userInfoData != null) {
                    result = userInfoData.getSimOperatorName;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorName(int)，返回值: " + safeToString(result));
                return result;
            };
        }
        //获取 SIM 卡所属运营商的 Carrier 名称
        if (TelephonyManager.getSimCarrierIdName) { //  todo by wzy sdk+hook
            TelephonyManager.getSimCarrierIdName.implementation = function () {
                var result = this.getSimCarrierIdName();
                if (userInfoData != null) {
                    result = userInfoData.getSimCarrierIdName;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimCarrierIdName()，返回值: " + safeToString(result));
                return result;
            };
        }

        // 获取SIM卡的状态
        // 返回的值代表了SIM卡的不同状态：SIM_STATE_UNKNOWN = 0, SIM_STATE_ABSENT = 1, SIM_STATE_PIN_REQUIRED = 2,
        // SIM_STATE_PUK_REQUIRED = 3, SIM_STATE_NETWORK_LOCKED = 4, SIM_STATE_READY = 5 此处模拟返回 SIM_STATE_READY，意味着SIM卡已经准备好使用
        if (TelephonyManager.getSimState) {  //  todo by wzy sdk+hook
            TelephonyManager.getSimState.overload().implementation = function () {
                var result = this.getSimState();
                if (userInfoData != null) {
                    result = userInfoData.getSimState;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimState()，返回值: " + safeToString(result));
                return result;
            };
        }
        // 获取当前网络所属国家的ISO代码（如：CN、US）
        if (TelephonyManager.getNetworkCountryIso && TelephonyManager.getNetworkCountryIso.overloads.length > 0) { // todo by wzy sdk+hook
            if (TelephonyManager.getNetworkCountryIso.overload()) {
                TelephonyManager.getNetworkCountryIso.overload().implementation = function () {
                    var result = this.getNetworkCountryIso();
                    if (userInfoData != null) {
                        result = userInfoData.getNetworkCountryIso;
                    }
                    logToFile("[*] 调用了 TelephonyManager.getNetworkCountryIso()，返回值: " + safeToString(result));
                    return result;
                };
            }
            if (TelephonyManager.getNetworkCountryIso.overload('int')) { // todo by wzy sdk+hook
                TelephonyManager.getNetworkCountryIso.overload('int').implementation = function (slotIndex) {
                    var result = this.getNetworkCountryIso(slotIndex);
                    if (userInfoData != null) {
                        result = userInfoData.getNetworkCountryIso;
                    }
                    logToFile("[*] 调用了 TelephonyManager.getNetworkCountryIso(slotIndex=" + slotIndex + ")，返回值: " + safeToString(result));
                    return result;
                };
            }
        }
        // 获取当前注册网络的运营商编号（MCC+MNC）
        if (TelephonyManager.getNetworkOperator.overload()) {   // todo by wzy sdk+hook
            TelephonyManager.getNetworkOperator.overload().implementation = function () {
                var result = this.getNetworkOperator();
                if (userInfoData != null) {
                    result = userInfoData.getNetworkOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getNetworkOperator()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getNetworkOperator.overload('int')) { // todo by wzy sdk+hook
            TelephonyManager.getNetworkOperator.overload('int').implementation = function (subId) {
                var result = this.getNetworkOperator(subId);
                if (userInfoData != null) {
                    result = userInfoData.getNetworkOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getNetworkOperator(int)，返回值: " + safeToString(result));
                return result;
            };
        }
        // 获取当前注册网络的运营商名称 运营商类型 此方法返回用户当前连接的移动网络运营商的名称（如 "中国移动"、"Bermuda, CellOne" 等） 若未注册到网络或信息不可用，则可能返回空值
        if (TelephonyManager.getNetworkOperatorName) {  // todo by wzy sdk+hook
            TelephonyManager.getNetworkOperatorName.overload().implementation = function () {
                var result = this.getNetworkOperatorName();
                if (userInfoData != null) {
                    result = userInfoData.getNetworkOperatorName;
                }
                logToFile("[*] 调用了 TelephonyManager.getNetworkOperatorName()，返回值: " + safeToString(result));
                return result;
            };
        }
        if (TelephonyManager.getNetworkOperatorName.overload('int')) {      //TODO by ly sdk+hook
            TelephonyManager.getNetworkOperatorName.overload('int').implementation = function (subId) {
                var result = this.getNetworkOperatorName(subId);
                if (userInfoData != null) {
                    result = userInfoData.getNetworkOperatorName;
                }
                logToFile("[*] 调用了 TelephonyManager.getNetworkOperatorName(int)，返回值: " + safeToString(result));
                return result;
            };
        }

        if (TelephonyManager.getSimStateIncludingLoaded) {     //TODO by ly sdk+hook
            TelephonyManager.getSimStateIncludingLoaded.overload().implementation = function () {
                var result = this.getSimStateIncludingLoaded();
                if (userInfoData != null) {
                    result = userInfoData.getSimState;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimStateIncludingLoaded()，返回值: " + result);
                return result;
            }

        }
        if (TelephonyManager.getSimOperatorNumericForPhone) {        //TODO by ly sdk+hook
            TelephonyManager.getSimOperatorNumericForPhone.overload('int').implementation = function (phoneId) {
                var result = this.getSimOperatorNumericForPhone(phoneId);
                if (userInfoData != null) {
                    result = userInfoData.getSimOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorNumericForPhone()，返回值: " + result);
                return result;
            }
        }
        if (TelephonyManager.getSimCountryIsoForPhone) {      //TODO by ly sdk+hook
            TelephonyManager.getSimCountryIsoForPhone.overload('int').implementation = function (phoneId) {
                var result = this.getSimCountryIsoForPhone(phoneId);
                if (userInfoData != null) {
                    result = userInfoData.getSimCountryIso;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimCountryIsoForPhone()，返回值: " + result);
                return result;
            }
        }
        if (TelephonyManager.getSimOperatorNameForPhone) {      //TODO by ly sdk+hook
            TelephonyManager.getSimOperatorNameForPhone.overload('int').implementation = function (phoneId) {
                var result = this.getSimOperatorNameForPhone(phoneId);
                if (userInfoData != null) {
                    result = userInfoData.getSimOperatorName;
                }
                logToFile("[*] 调用了 TelephonyManager.getSimOperatorNameForPhone()，返回值: " + result);
                return result;
            }
        }
        // 根据电话卡ID获取运营商编号（MCC+MNC）
        if (TelephonyManager.getNetworkOperatorForPhone && TelephonyManager.getNetworkOperatorForPhone.overload('int')) { // todo by wzy sdk+hook //TODO by ly 注意每个phoneId都需要获取
            TelephonyManager.getNetworkOperatorForPhone.overload('int').implementation = function (phoneId) {
                var result = this.getNetworkOperatorForPhone(phoneId);
                if (userInfoData != null) {
                    result = userInfoData.getNetworkOperator;
                }
                logToFile("[*] 调用了 TelephonyManager.getNetworkOperatorForPhone(phoneId=" + phoneId + ")，返回值: " + safeToString(result));
                return result;
            };
        }
    } catch (e) {
        notifyError(e)  //  TelephonyManager
    }
}

function hookSettingSecureAndGlobal(userInfoData, deviceInfoData) {
    try {
        // 获取 Settings.Secure 类
        const System = Java.use('android.provider.Settings$System');
        if (System) {
            if (System.getInt.overload('android.content.ContentResolver', 'java.lang.String')) {
                System.getInt.overload('android.content.ContentResolver', 'java.lang.String').implementation = function (cr, name) {
                    var result = this.getInt(cr, name);
                    if (name == 'show_touches') {
                        result = 0;
                    }
                    logToFile(' System.getInt(2) 名称: ' + name + '\t返回结果: ' + result);
                    return result;
                };
            }
            if (System.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int')) {
                System.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int').implementation = function (cr, name, def) {
                    var result = this.getInt(cr, name, def);
                    if (name == 'show_touches') {
                        result = 0;
                    }
                    logToFile(' System.getInt(3) 名称: ' + name + '\t默认值: ' + def + '\t返回结果: ' + result);
                    return result;
                };
            }
        }
    } catch (e) {
        notifyError(e); // Secure
    }
    try {
        // 获取 Settings.Secure 类
        const Secure = Java.use('android.provider.Settings$Secure');
        if (Secure) {
            // Hook getInt(ContentResolver cr, String name)
            if (Secure.getInt.overload('android.content.ContentResolver', 'java.lang.String')) {   // todo by wzy sdk+hook
                Secure.getInt.overload('android.content.ContentResolver', 'java.lang.String').implementation = function (cr, name) {
                    var result = this.getInt(cr, name);
                    if (name == 'stylus_handwriting_enabled' && userInfoData != null) {
                        if (userInfoData.settingSecureHashMapInt
                            && userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled']) {
                            result = userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled'];
                        }
                    } else if (checkNeedHookBySettings_SecureIntKey(name)) {
                        result = 0;
                    }
                    logToFile(' Secure.getInt(2) 名称: ' + name + '\t返回结果: ' + result);
                    return result;
                };
            }
            // Hook getInt(ContentResolver cr, String name, int def)
            if (Secure.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int')) {  // todo by wzy sdk+hook
                Secure.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int').implementation = function (cr, name, def) {
                    var result = this.getInt(cr, name, def);
                    if (name == 'stylus_handwriting_enabled' && userInfoData != null) {
                        if (userInfoData.settingSecureHashMapInt
                            && userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled']) {
                            result = userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled'];
                        }
                    } else if (checkNeedHookBySettings_SecureIntKey(name)) {
                        result = 0;
                    }
                    logToFile(' Secure.getInt(3) 名称: ' + name + '\t默认值: ' + def + '\t返回结果: ' + result);
                    return result;
                };
            }
            // Hook getIntForUser(ContentResolver cr, String name, int def, int userId)
            if (Secure.getIntForUser) {   // todo by wzy sdk+hook
                Secure.getIntForUser.overload('android.content.ContentResolver', 'java.lang.String', 'int', 'int').implementation = function (cr, name, def, userId) {
                    // 调用原始方法并获取返回值
                    var result = this.getIntForUser(cr, name, def, userId);
                    if (name == 'stylus_handwriting_enabled' && userInfoData != null) {
                        if (userInfoData.settingSecureHashMapInt
                            && userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled']) {
                            result = userInfoData.settingSecureHashMapInt['stylus_handwriting_enabled'];
                        }
                    } else if (checkNeedHookBySettings_SecureIntKey(name)) {
                        result = 0;
                    }
                    logToFile(' Secure.getInt(4) 名称: ' + name + '\t默认值: ' + def + '\tuser ID: ' + userId + '\t返回结果: ' + result);
                    return result;
                };
            }
            if (Secure.getString) { //  todo by wzy sdk+hook
                Secure.getString.overload('android.content.ContentResolver', 'java.lang.String').implementation = function (contentResolver, name) {
                    let result = this.getString(contentResolver, name);
                    try {
                        if (name == "android_id") {                                 //  TODO by ly 不需要SDK，需要Hook。清除数据后android_id不变
                            result = MACRO_DATA.ANDROID_ID
                            logToFile("[+] 获取到的 Android ID: " + result);
                        } else if (name == "enabled_accessibility_services") {//启动的无障碍服务列表 值就是空串
                            result = "";
                        } else if (name == "advertising_id") {//google gaid
                            if (userInfoData != null) {
                                result = userInfoData.GAID
                            }
                        } else if (name == "accessibility_captioning_locale") {//SDK 无障碍字幕的语言配置
                            if (userInfoData != null && userInfoData.settingSecureHashMapString['accessibility_captioning_locale'] != null) {
                                result = userInfoData.settingSecureHashMapString['accessibility_captioning_locale'];
                            } else {
                                result = null;
                            }
                        } else {
                            logToFile("[+] Secure.getString,用户输入的name: " + name);  //  TODO by wzy ly SDK+Hook。需要处理advertising_id、enabled_accessibility_services、accessibility_captioning_locale
                        }
                    } catch (e) {
                        notifyError(e); //  Secure.getString.overload
                    }
                    return result;
                };
            }
        }
    } catch (e) {
        notifyError(e); // Secure
    }
    try {
        const SettingsGlobal = Java.use('android.provider.Settings$Global');
        // Hook getInt 方法以监听对 adb_enabled 的访问
        SettingsGlobal.getInt.overload('android.content.ContentResolver', 'java.lang.String', 'int').implementation = function (cr, name, def) {  //  todo by wzy sdk+hook
            var result = this.getInt(cr, name, def);   //TODO by ly 这些都需要，SDK+hook
            if (name === 'adb_enabled'//仅hook 值为 0
                || name === 'development_settings_enabled'//仅hook 值为 null 开发者模式
                || name === 'force_resizable_activities'//仅hook  值为0 开发者选项Activity是否可调整大小
                || name === 'show_touches'//仅hook  值为0 点击、滑动是否有小圆点
            ) {
                result = 0;
            } else if (name === 'force_fsg_nav_bar') {//SDK值 0或1，1导航栏强制显示；0不强制
                if (userInfoData.settingGlobalHashMapInt != null
                    && userInfoData.settingGlobalHashMapInt['force_fsg_nav_bar'] != null) {
                    result = userInfoData.settingGlobalHashMapInt['force_fsg_nav_bar']
                }
            } else if (name === 'boot_count') {//SDK值 手机总共开了多少次机
                if (userInfoData.settingGlobalHashMapInt != null
                    && userInfoData.settingGlobalHashMapInt['boot_count'] != null) {
                    result = userInfoData.settingGlobalHashMapInt['boot_count']
                }
            }
            logToFile(' Settings.Global(2) 名称: ' + name + '\t返回结果: ' + result);   //  todo by wzy sdk+hook
            return result;
        };
        if (SettingsGlobal.getString) {  //  todo by wzy sdk+hook
            SettingsGlobal.getString.overload('android.content.ContentResolver', 'java.lang.String').implementation = function (contentResolver, name) {
                var result = this.getString(contentResolver, name);
                if (name === 'adb_enabled'//仅hook 值为 0
                    || name === 'force_resizable_activities'//仅hook  值为0 开发者选项Activity是否可调整大小
                    || name === 'show_touches'//仅hook  值为0 点击、滑动是否有小圆点
                ) {
                    result = "0";
                } else if (name === 'force_fsg_nav_bar') {//SDK值 0或1，1导航栏强制显示；0不强制
                    if (userInfoData.settingGlobalHashMapString != null
                        && userInfoData.settingGlobalHashMapString['force_fsg_nav_bar'] != null) {
                        result = userInfoData.settingGlobalHashMapString['force_fsg_nav_bar']
                    }
                } else if (name === 'boot_count') {//SDK值 手机总共开了多少次机
                    if (userInfoData.settingGlobalHashMapString != null
                    ) {
                        result = userInfoData.settingGlobalHashMapString['boot_count']
                    }
                } else if (name === "device_name") {//SDK Build获取的值  Pixel 8 Pro
                    if (deviceInfoData != null) {
                        result = deviceInfoData.MODEL
                    }
                } else if (name === 'development_settings_enabled') {//仅hook 值为 null 开发者模式
                    result = null
                }

                logToFile(`[*] 应用读取SettingsGlobal.getString name=${name}` + " result=" + result);
                return result;
            }
        }
    } catch (e) {
        notifyError(e); // SettingsGlobal
    }
}

// 电池信息Hook
function hookBatteryManager(userInfoData) {
    try {
        var BatteryManager = Java.use("android.os.BatteryManager");
        BatteryManager.getIntProperty.overload('int').implementation = function (key) {  //  todo by wzy sdk+hook TODO by ly 特别是1、2、3、4参数
            var result = this.getIntProperty(key);
            if (userInfoData != null) {
                if (key == BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER.value) {
                    var temp = this.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY.value);
                    result = Math.floor(userInfoData.BATTERY_PROPERTY_TOTAL_LONG * (temp / 100));
                } else if (key == BatteryManager.BATTERY_PROPERTY_CURRENT_NOW.value) {
                    var temp = this.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS.value);
                    if (temp === BatteryManager.BATTERY_STATUS_CHARGING.value) {
                        result = (Math.floor(Math.random() * (1200 - 900 + 1)) + 900) * -1
                    } else if (temp === BatteryManager.BATTERY_STATUS_FULL.value) {
                        result = 0;
                    } else {
                        result = (Math.floor(Math.random() * (700 - 200 + 1)) + 200)
                    }
                    var now = userInfoData.BATTERY_PROPERTY_CURRENT_NOW;
                    if (result != 0 && now % 1000 == 0 && now > 5000) {
                        result = result * 1000;
                    }
                } else if (key == BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE.value) {
                    result = userInfoData.BATTERY_PROPERTY_CURRENT_AVERAGE;
                } else if (key == BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER.value) {
                    result = userInfoData.BATTERY_PROPERTY_ENERGY_COUNTER;
                }
            }
            logToFile(`BatteryManager.getIntProperty(int) ,key=${key}, result=${result}`);
            return result;
        };
        BatteryManager.getLongProperty.overload('int').implementation = function (key) { //  todo by wzy sdk+hook TODO by ly 特别是1、2、3、4参数
            var result = this.getLongProperty(key);
            if (userInfoData != null) {
                if (key == BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER.value) {
                    var temp = this.getLongProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY.value);
                    result = userInfoData.BATTERY_PROPERTY_TOTAL_LONG * (temp / 100);
                } else if (key == BatteryManager.BATTERY_PROPERTY_CURRENT_NOW.value) {
                    var temp = this.getLongProperty(BatteryManager.BATTERY_PROPERTY_STATUS.value);
                    if (temp === BatteryManager.BATTERY_STATUS_CHARGING.value) {
                        result = (Math.floor(Math.random() * (1200 - 900 + 1)) + 900) * -1
                    } else if (temp === BatteryManager.BATTERY_STATUS_FULL.value) {
                        result = 0;
                    } else {
                        result = (Math.floor(Math.random() * (700 - 200 + 1)) + 200)
                    }
                    var now = userInfoData.BATTERY_PROPERTY_CURRENT_NOW_LONG;
                    if (result != 0 && now % 1000 == 0 && now > 5000) {
                        result = result * 1000;
                    }
                } else if (key == BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE.value) {
                    result = userInfoData.BATTERY_PROPERTY_CURRENT_AVERAGE_LONG;
                } else if (key == BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER.value) {
                    result = userInfoData.BATTERY_PROPERTY_ENERGY_COUNTER_LONG;
                }
            }
            logToFile(`BatteryManager.getLongProperty(int) ,key=${key}, result=${result}`);
            return result;
        };
    } catch (e) {
        notifyError(e); // BatteryManager
    }
}
//音频相关 // 声明全局
const __fia_audioDeviceCache = [];
var audioIndex = 0;
function __fia_addAudioDevice(audioDeviceInfo, jsonObject) {
    if (__fia_audioDeviceCache.length >= 300) {
        __fia_audioDeviceCache[audioIndex % 300] = { device: audioDeviceInfo, data: jsonObject }
        audioIndex++;
    } else {
        __fia_audioDeviceCache.push({
            device: audioDeviceInfo,
            data: jsonObject
        });
    }

}
function __fia_findAudioDevice(audioDeviceInfo) {
    for (let i = 0; i < __fia_audioDeviceCache.length; i++) {
        if (__fia_audioDeviceCache[i] != null && __fia_audioDeviceCache[i].device === audioDeviceInfo) {
            return __fia_audioDeviceCache[i].data;
        }
    }
    return null;
}
function hookAudioManager(userInfoData, SDK_INT) {
    const AudioManager = Java.use('android.media.AudioManager');
    if (AudioManager.getDevices && SDK_INT >= 21) {
        AudioManager.getDevices.overload('int').implementation = function (deviceFlag) {    //  todo 需要SDK+Hook 输入和输出对象都要伪造，AppLovin收集了
            var result = this.getDevicesStatic(deviceFlag);
            logToFile("[*] 调用了 AudioManager.getDevices(), 设备标志: " + deviceFlag + " result==" + result);
            if (((deviceFlag == 2 && userInfoData.GET_DEVICES_OUTPUTS != null && userInfoData.GET_DEVICES_OUTPUTS.length > 0)
                || (deviceFlag == 1 && userInfoData.GET_DEVICES_INPUTS != null && userInfoData.GET_DEVICES_INPUTS.length > 0))
                && result.length > 0) {
                var sdkData = deviceFlag == 1 ? userInfoData.GET_DEVICES_INPUTS : userInfoData.GET_DEVICES_OUTPUTS;
                var needGetCount = (sdkData.length / result.length)
                if (needGetCount > 1) {
                    needGetCount = Math.floor(needGetCount + 1);
                    for (var i = 1; i < needGetCount; i++) {
                        result.push(...this.getDevicesStatic(deviceFlag));
                    }
                    result = result.slice(0, sdkData.length);
                }
                for (var i = 0; i < sdkData.length; i++) {
                    __fia_addAudioDevice(result[i], sdkData[i]);
                }
                logToConsole("[*][frida缓存]__fia_audioDeviceCache==" + __fia_audioDeviceCache.length)
            }
            return result;
        };
        const AudioDeviceInfo = Java.use('android.media.AudioDeviceInfo');
        if (AudioDeviceInfo.getAddress) {
            AudioDeviceInfo.getAddress.implementation = function () {
                var result = this.getAddress();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getAddress
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getAddress(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getType) {
            AudioDeviceInfo.getType.implementation = function () {
                var result = this.getType();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getType
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getType(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getId) {
            AudioDeviceInfo.getId.implementation = function () {
                var result = this.getId();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getId
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getId(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getChannelCounts) {
            AudioDeviceInfo.getChannelCounts.implementation = function () {
                var result = this.getChannelCounts();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getChannelCounts
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getChannelCounts(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getChannelMasks) {
            AudioDeviceInfo.getChannelMasks.implementation = function () {
                var result = this.getChannelMasks();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getChannelMasks
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getChannelMasks(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getChannelIndexMasks) {
            AudioDeviceInfo.getChannelIndexMasks.implementation = function () {
                var result = this.getChannelIndexMasks();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getChannelIndexMasks
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getChannelIndexMasks(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getEncodings) {
            AudioDeviceInfo.getEncodings.implementation = function () {
                var result = this.getEncodings();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getEncodings
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getEncodings(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getSampleRates) {
            AudioDeviceInfo.getSampleRates.implementation = function () {
                var result = this.getSampleRates();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getSampleRates
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getSampleRates(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getEncapsulationMetadataTypes) {
            AudioDeviceInfo.getEncapsulationMetadataTypes.implementation = function () {
                var result = this.getEncapsulationMetadataTypes();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getEncapsulationMetadataTypes
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getEncapsulationMetadataTypes(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getEncapsulationModes) {
            AudioDeviceInfo.getEncapsulationModes.implementation = function () {
                var result = this.getEncapsulationModes();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getEncapsulationModes
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getEncapsulationModes(), result==" + result);
                return result;
            };
        }
        if (AudioDeviceInfo.getProductName) {
            AudioDeviceInfo.getProductName.implementation = function () {
                var result = this.getProductName();
                var sdkData = __fia_findAudioDevice(this);
                if (sdkData != null) {
                    result = sdkData.getProductName
                }
                logToFile("[*] 调用了 AudioDeviceInfo.getProductName(), result==" + result);
                return result;
            };
        }
    }
    if (SDK_INT >= 30) { // API 30+
        AudioManager.getProperty.overload('java.lang.String').implementation = function (key) {   // todo by wzy SDK获取"android.media.property.OUTPUT_SAMPLE_RATE", Hook这个key的value
            var result = this.getProperty(key);
            if (key === 'android.media.property.OUTPUT_SAMPLE_RATE') {
                if (userInfoData != null) {
                    result = userInfoData.PROPERTY_OUTPUT_SAMPLE_RATE
                }
            }
            logToFile("[*] 应用读取AudioManager.getProperty key=" + key + " result=" + result);
            return result;
        }
        AudioManager.getParameters.overload('java.lang.String').implementation = function (keys) {  //TODO by ly SDK获取"offloadVariableRateSupported"，Hook这个key的value
            var result = this.getParameters(keys);
            if (keys === "offloadVariableRateSupported") {
                if (userInfoData != null) {
                    result = userInfoData.offloadVariableRateSupported;
                }
            }
            logToFile("[*] 应用读取AudioManager.getParameters keys=" + keys + " result=" + result);
            return result;
        }
    } else {
        logToFile("[!] 设备 API 级别低于 30，不支持 getActivePlaybackConfigurations / getActiveRecordingConfigurations");
    }
}
// 某行是不是系统调用
function isSystemStack(stack) {
    var result = stack.trim();
    if (result.startsWith("at android.") || result.startsWith("at androidx.")
        || result.startsWith("at com.android.") || result.startsWith("at java.")
        || result.search("SystemWebView.apk") !== -1) {
        return true;
    }
    return false;
}
// 无障碍服务
function hookAccessibilityManager(ArrayList) {
    function printStackAndSearchSystem() {
        // 获取调用栈字符串
        var stackTrace = Java.use("android.util.Log").getStackTraceString(Java.use("java.lang.Throwable").$new()).split("\n");
        logToFile("调用栈信息:\n", '0=' + stackTrace[0] + '    1=' + stackTrace[1] + '    2=' + stackTrace[2] + '    3=' + stackTrace[3]);
        // 提取前三行并拼接输出
        if (isSystemStack(stackTrace[1]) && isSystemStack(stackTrace[2]) && isSystemStack(stackTrace[3])) {
            return true;
        }
        return false

    }
    const AccessibilityManager = Java.use('android.view.accessibility.AccessibilityManager');
    // Hook: isEnabled()
    if (AccessibilityManager.isEnabled) {
        AccessibilityManager.isEnabled.overloads.forEach(function (overload) { //  todo by wzy 不加sdk，加hook
            overload.implementation = function () {
                let result = this.isEnabled.apply(this, arguments);
                // var result = false;
                if (!printStackAndSearchSystem()) {
                    result = false;
                }
                logToFile(`[AccessibilityManager] isEnabled 返回值: ${result}`);
                return result;
            };
        });
    }

    // Hook: isTouchExplorationEnabled()
    if (AccessibilityManager.isTouchExplorationEnabled) {
        AccessibilityManager.isTouchExplorationEnabled.overloads.forEach(function (overload) { //  todo by wzy 不加sdk，加hook
            overload.implementation = function () {
                let result = this.isTouchExplorationEnabled.apply(this, arguments);
                // var result = false;
                if (!printStackAndSearchSystem()) {
                    result = false;
                }
                logToFile(`[AccessibilityManager] isTouchExplorationEnabled 返回值: ${result}`);
                return result;
            };
        });
    }

    // Hook: getEnabledAccessibilityServiceList(int)
    if (AccessibilityManager.getEnabledAccessibilityServiceList) {
        AccessibilityManager.getEnabledAccessibilityServiceList.overloads.forEach(function (overload) {  //  todo by wzy 不加sdk，加hook
            if (overload.argumentTypes.length === 1 &&
                overload.argumentTypes[0].className === 'int') {
                overload.implementation = function (feedbackTypeFlags) {
                    let list = this.getEnabledAccessibilityServiceList(feedbackTypeFlags);
                    // let list = ArrayList.$new();
                    if (!printStackAndSearchSystem()) {
                        list = ArrayList.$new();
                    }
                    logToFile(`[AccessibilityManager] getEnabledAccessibilityServiceList(${feedbackTypeFlags}), 返回服务数量: ${list.size()}`);
                    return list;
                };
            }
        });
    }
}
//摄像头相关信息
function hookCameraManager() {
    const CameraManager = Java.use('android.hardware.camera2.CameraManager');
    if (CameraManager.getCameraIdList) {
        CameraManager.getCameraIdList.implementation = function () {    //  todo by wzy sdk+hook
            try {
                let result = this.getCameraIdList();
                if (DEVICE_DATA != null) {
                    if (IS_DEBUG) {
                        logToFile(`getCameraIdList返回结果: ${result}  sdk result=${DEVICE_DATA.cameraIds}`);
                    }
                    result = DEVICE_DATA.cameraIds;
                }
                return result;
            } catch (e) {
                notifyError(e);
                throwError(e);
            }
        };
    } else {
        logToFile("未找到 getCameraIdList 方法");
    }
}
//进程管理、内存管理
function hookActivityManager(userInfoData, SDK_INT) {
    const ActivityManager = Java.use('android.app.ActivityManager');
    const MemoryInfo = Java.use('android.app.ActivityManager$MemoryInfo');
    ActivityManager.getMemoryInfo.overload('android.app.ActivityManager$MemoryInfo').implementation = function (memoryInfo) {   //  todo by wzy sdk+hook
        try {
            // 调用原始方法获取原始数据
            var memoryInfo = Java.cast(memoryInfo, MemoryInfo)
            this.getMemoryInfo(memoryInfo);
            memoryInfo.totalMem.value = userInfoData.getMemoryInfo.totalMem;
            memoryInfo.lowMemory.value = userInfoData.getMemoryInfo.lowMemory;
            memoryInfo.availMem.value = userInfoData.getMemoryInfo.availMem + getRandomByteSize();
            memoryInfo.threshold.value = userInfoData.getMemoryInfo.threshold;
            if (SDK_INT > 34 && userInfoData.getMemoryInfo.advertisedMem != null) {
                memoryInfo.advertisedMem.value = userInfoData.getMemoryInfo.advertisedMem;
            }
            if (IS_DEBUG) {
                // 打印 MemoryInfo 的详情信息
                logToFile("[*] 详细内存信息:");
                logToFile("[*] 总内存: " + memoryInfo.totalMem.value / (1024 * 1024) + " MB");
                logToFile("[*] 可用内存: " + memoryInfo.availMem.value / (1024 * 1024) + " MB");
                logToFile("[*] 阈值内存: " + memoryInfo.threshold.value / (1024 * 1024) + " MB");
                logToFile("[*] 是否低内存: " + memoryInfo.lowMemory.value);
            }

        } catch (e) {
            notifyError(e); // 记录错误
            throwError(e); // 重新抛出异常以保持原有行为
        }
    };
}
//HookPackageManager 
function hookPackageManager(userInfoData, ArrayList) {
    try {
        var PackageManager = Java.use("android.app.ApplicationPackageManager");
        PackageManager.hasSystemFeature.overload('java.lang.String').implementation = function (feature) {  //  todo by wzy sdk+hook。具体sdk和hook哪些做到时再碰。比如android.hardware.type.foldable、android.hardware.sensor.hinge_angle，android.hardware.touchscreen.multitouch.jazzhand
            var result = this.hasSystemFeature.call(this, feature);
            logToFile("[+] PackageManager.hasSystemFeature(String): " + feature + ",\tresult=" + result);
            if (feature == PackageManager.FEATURE_AUDIO_LOW_LATENCY.value) {//手机是否支持低延迟音频输出 SDK
                if (userInfoData != null) {
                    result = userInfoData.hasSystemFeature[feature];
                }
            } else if (feature == PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND.value) {//是否高级多点触控（5点以上），云手机参数错误 SDK
                if (userInfoData != null) {
                    result = userInfoData.hasSystemFeature[feature];
                }
            } else if (feature == 'cn.google') {//返回false 是否是中国大陆的手机
                result = false;
            } else if (feature == 'com.google.android.feature.services_updater') {//返回true 是否支持google play服务的自动升级
                result = true;
            }
            return result;
        };
        if (PackageManager.hasSystemFeature.overload('java.lang.String', 'int')) {
            PackageManager.hasSystemFeature.overload('java.lang.String', 'int').implementation = function (feature, flags) {  //  todo by wzy sdk+hook。具体sdk和hook哪些做到时再碰。比如android.hardware.type.foldable、android.hardware.sensor.hinge_angle，android.hardware.touchscreen.multitouch.jazzhand
                var result = this.hasSystemFeature.overload('java.lang.String', 'int').call(this, feature, flags);
                if (feature == PackageManager.FEATURE_AUDIO_LOW_LATENCY.value) {//手机是否支持低延迟音频输出 SDK
                    if (userInfoData != null) {
                        result = userInfoData.hasSystemFeature[feature];
                    }
                } else if (feature == PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND.value) {//是否高级多点触控（5点以上），云手机参数错误 SDK
                    if (userInfoData != null) {
                        result = userInfoData.hasSystemFeature[feature];
                    }
                } else if (feature == 'cn.google') {//返回false 是否是中国大陆的手机
                    result = false;
                } else if (feature == 'com.google.android.feature.services_updater') {//返回true 是否支持google play服务的自动升级
                    result = true;
                }
                logToFile("[+] hasSystemFeature(String, int): " + feature + ", flags=" + flags + ",\tresult=" + result);
                return result;
            };
        }
    } catch (e) {
        notifyError(e); // hasSystemFeature(String)
    }
    const ApplicationInfo = Java.use('android.content.pm.ApplicationInfo');
    var macroInfoData = MACRO_DATA;
    try {
        // 获取设备上安装的应用列表  返回 List<ApplicationInfo> 对象，仅包含应用的核心属性（如标签、图标），不包含版本或组件详情‌
        if (PackageManager && PackageManager.getInstalledApplications) {    // todo by wzy sdk+hook
            PackageManager.getInstalledApplications.overload('int').implementation = function (flags) {// why:只返回自己
                try {
                    let result = this.getInstalledApplications(flags);
                    var resultArray = ArrayList.$new();
                    if (macroInfoData != null && macroInfoData.AppPackageInfo != null && macroInfoData.AppPackageInfo.APP_PACKAGE != null) {
                        for (var i = 0; i < result.size(); i++) {
                            const data = result.get(i)
                            if (data != null) {
                                const item = Java.cast(data, ApplicationInfo);
                                if (item.packageName.value == macroInfoData.AppPackageInfo.APP_PACKAGE) {
                                    resultArray.add(data);
                                }
                            }
                        }
                    }
                    logToFile(`[*] PackageManager.getInstalledApplications 被调用，flags:${flags}, 已安装包数量=${result.size()}  =${resultArray.size()}`);
                    if (resultArray.size() == 1) {
                        result = resultArray;
                    }
                    return result;
                } catch (e) {
                    notifyError(e);
                    return this.getInstalledApplications(flags); // 出错后仍尝试执行原方法
                }
            };
        }
    } catch (e) {
        notifyError(e);
    }
    function isNeedIntercept(packageName) {
        return packageName == 'android' || packageName == 'projekt.substratum' || packageName.startsWith('com.android.')
            || packageName.startsWith('com.google.android.') || packageName == macroInfoData.AppPackageInfo.APP_PACKAGE

    }
    try {
        PackageManager.getApplicationInfo.overload('java.lang.String', 'int').implementation = function (packageName, flags) {  //  todo by wzy 不需要SDK，需要加hook。仅允许部分应用有返回（通过宏），
            //其他throw new(android.content.pm.PackageManager$NameNotFoundException: $packagename)
            logToFile("[*] 调用了包的 getApplicationInfo: " + packageName + ", flags=" + flags);    // flags = 1024/81933/0/128
            let applicationInfo = this.getApplicationInfo(packageName, flags);
            // com.android.*
            // com.google.android.*
            // 自己,android,com.google.android.gms,com.android.vending，其他的抛出异常NameNotFoundException
            if (isNeedIntercept(packageName)) {
                return applicationInfo;
            } else {
                // return applicationInfo;
                // 获取NameNotFoundException类
                var NameNotFoundException = Java.use('android.content.pm.PackageManager$NameNotFoundException');
                // 创建并抛出异常实例
                throw NameNotFoundException.$new(packageName);
            }

        };
    } catch (e) {
        notifyError(e);
    }
    try {
        // todo by wzy sdk+hook
        PackageManager.getPackageInfo.overload('java.lang.String', 'int').implementation = function (packageName, flags) {  //  todo by wzy sdk+hook
            try {//只hook自己
                var date = new Date();
                var realPkg = packageName ? packageName.toString() : "";
                logToFile("[*] PackageManager.getPackageInfo 包名 " + realPkg + "  date =" + date + " Flags=" + flags);
                // 判断是否为无效包名
                if (!realPkg || realPkg.length === 0) {
                    logToFile("[-] PackageManager.getPackageInfo 包名为空，跳过 " + "  date =" + date + " Flags=" + flags);
                    return null;
                }
                //  todo 可打印多个参数
                var packageInfo = this.getPackageInfo(packageName, flags);
                if (packageInfo && isNeedIntercept(packageName) && macroInfoData != null && macroInfoData.AppPackageInfo != null) {
                    if (packageName == macroInfoData.AppPackageInfo.APP_PACKAGE) {
                        packageInfo.firstInstallTime.value = macroInfoData.AppPackageInfo.firstInstallTime;
                        packageInfo.lastUpdateTime.value = macroInfoData.AppPackageInfo.lastUpdateTime;
                    } else {
                        packageInfo.lastUpdateTime.value = macroInfoData.AppPackageInfo.systemAppLastUpdateTime;
                        packageInfo.firstInstallTime.value = macroInfoData.AppPackageInfo.systemAppFirstInstallTime;
                    }
                    var firstInstallTime = packageInfo.firstInstallTime.value;
                    var lastUpdateTime = packageInfo.lastUpdateTime.value;
                    var infoMessage = ` 首次安装时间: (${firstInstallTime}) 最后更新时间: (${lastUpdateTime})`          //TODO by ly hook。这个首次安装、最后更新时间一定要hook
                    logToFile("[+] PackageManager.getPackageInfo 包名 " + realPkg + "  date =" + date + "  packageInfo=" + packageInfo + "  infoMessage==" + infoMessage + " Flags=" + flags);
                } else {
                    logToFile("[-] PackageManager.getPackageInfo 包名 " + realPkg + "  date =" + date + "PackageInfo 为 null   PackageManager.getPackageInfo 包名 " + realPkg + " Flags=" + flags);
                }
            } catch (e) {
                if (e.message.includes("NameNotFoundException")) {
                    logToFile(`[-] 找不到包名: ${realPkg}` + "  date =" + date + " Flags=" + flags);
                } else {
                }
                throwError(e);
            }
            //  最近一次打开时间 无法确定, app中可自己去存储对应逻辑
            return packageInfo;
        };
    } catch (e) {
        notifyError(e);
    }
}

function getValueNeedHookBy__system_property_getKey(key, userInfoData, deviceInfoData) {
    var result;
    if (key === "ro.build.version.release") {//安卓版本 应修改outputBufferContent这个值
        result = userInfoData.VERSION$RELEASE;
    } else if (key.search(".qemu") !== -1) {//  所有包含".qemu"//无返回值	qemu是模拟器参数，不返回值
        result = null;
    } else if (key.startsWith("sys.display.gpu.glget.")) {//无返回值	sys.display.gpu.glget*，都无返回值
        result = null;
    } else if (key === "ro.build.version.sdk") { //安卓SDK版本(33、34、35那种)	需要修改为实际安卓SDK版本
        result = userInfoData.VERSION$SDK_INT;
    } else if (key === "ro.build.user") {//返回Build对应信息
        result = userInfoData.USER;
    } else if (key === "ro.build.display.id") {
        result = userInfoData.DISPLAY;
    } else if (key === "ro.build.id") {
        result = userInfoData.ID;
    } else if (key === "ro.build.fingerprint") {
        result = deviceInfoData.FINGERPRINT;
    } else if (key === "ro.product.manufacturer") {
        result = deviceInfoData.MANUFACTURER;
    } else if (key === "ro.product.vendor.manufacturer") {
        result = deviceInfoData.MANUFACTURER;
    } else if (key === "ro.soc.manufacturer") {
        result = deviceInfoData.SOC_MANUFACTURER;
    } else if (key === "ro.product.brand") {
        result = deviceInfoData.BRAND;
    } else if (key === "ro.product.name") {
        result = deviceInfoData.PRODUCT;
    } else if (key === "ro.hardware") {
        result = deviceInfoData.HARDWARE;
    } else if (key === "ro.product.board") {
        result = deviceInfoData.BOARD;
    } else if (key === "ro.product.device") {
        result = deviceInfoData.DEVICE;
    } else if (key === "ro.product.model") {
        result = deviceInfoData.MODEL;
    } else if (key === "ro.build.host") {
        result = userInfoData.HOST;
    } else if (key === "ro.build.version.codename") {
        result = userInfoData.VERSION$CODENAME;
    } else if (key === "ro.bootloader") {
        result = deviceInfoData.BOOTLOADER;
    } else if (key === "ro.soc.model") {
        result = deviceInfoData.SOC_MODEL;
    } else if (key === "ro.runtime.firstboot") {
        result = null;
    } else if (key === "ro.board.platform") {
        // result = deviceInfoData.HARDWARE;
        result = deviceInfoData.BOARD;
    } else if (key === "ro.build.product") {
        // result = deviceInfoData.MODEL;
        result = deviceInfoData.DEVICE;
    } else if (key == 'qemu.hw.mainkeys') {
        result = null;
    }
    return result;
}


function checkNeedHookBy__system_property_getKey(key) {
    if (key === "ro.build.version.release"//安卓版本 应修改outputBufferContent这个值
        || key === "qemu.hw.mainkeys"
        || key.search(".qemu") !== -1//  所有包含".qemu"//无返回值	qemu是模拟器参数，不返回值
        || key.startsWith("sys.display.gpu.glget.")//无返回值	sys.display.gpu.glget*，都无返回值
        || key === "ro.build.version.sdk"//安卓SDK版本(33、34、35那种)	需要修改为实际安卓SDK版本
        || key === "ro.build.user"//返回Build对应信息
        || key === "ro.build.display.id"
        || key === "ro.build.id"
        || key === "ro.build.fingerprint"
        || key === "ro.product.manufacturer"
        || key === "ro.product.vendor.manufacturer"
        || key === "ro.soc.manufacturer"
        || key === "ro.product.brand"
        || key === "ro.product.name"
        || key === "ro.hardware"
        || key === "ro.product.board"
        || key === "ro.product.device"
        || key === "ro.product.model"
        || key === "ro.build.host"
        || key === "ro.build.version.codename"
        || key === "ro.bootloader"
        || key === "ro.soc.model"
        || key === "ro.runtime.firstboot"//无返回值	真机无此参数
        || key === "ro.board.platform"
        || key === "ro.build.product"
    ) {
        return true;
    }
    return false;
}
/**
 *  // 绕过长度限制
    var propRead = Module.findExportByName("libc.so", "__system_property_read");
    Interceptor.attach(propRead, {
        onLeave: function (retval) {
            retval.replace(92); // 强制最大长度
        }
    });
 * 监听目标App行为- 模拟器检测 // TODO by wzy sdk+hook
 */
function hookFile(userInfoData, deviceInfoData) {
    function getStatFsByPath(path) {
        var statfs = null
        if (userInfoData != null && path != null) {
            if (path.startsWith('/storage/emulated/0/')) {
                statfs = userInfoData.storageEmulatedStatFs;
            } else if (path.startsWith('/data')) {
                statfs = userInfoData.dataStatFs;
            } else if (path.startsWith('/storage')) {
                statfs = userInfoData.storageStatFs;
            }
        }
        return statfs
    }
    try {
        var FileClass = Java.use('java.io.File');
        function logMethodCall(methodName, result) {
            logToFile(`${methodName} 返回结果: ${result}`);
            return result;
        }
        function checkNeedFilter(path) {
            if (path != null && path.value == SU_PATH) {
                return true;
            }
            return false;
        }
        // FileClass.getAbsolutePath.overload().implementation = function () { // todo by wzy 不加sdk，加hook  TODO by ly 用分身会读到实际目录，需要hook 。分身需要处理/data/data/、/storage/emulated/0/两个目录 
        //     var originalPath = this.getAbsolutePath();
        //     return logMethodCall('File.getAbsolutePath', originalPath);
        // };
        // FileClass.getCanonicalPath.overload().implementation = function () {    // todo by wzy 不加sdk，加hook   TODO by ly 用分身会读到实际目录，需要hook。分身需要处理/data/data/、/storage/emulated/0/两个目录 
        //     try {
        //         var canonicalPath = this.getCanonicalPath();
        //         return logMethodCall('File.getCanonicalPath', canonicalPath);
        //     } catch (error) {
        //         notifyError(error); // getCanonicalPath 后面主动抛出异常
        //     }
        // };
        // FileClass.getPath.overload().implementation = function () {  //  todo by wzy 不加sdk，加hook      TODO by ly 用分身会读到实际目录，需要hook。分身需要处理/data/data/、/storage/emulated/0/两个目录 
        //     var path = this.getPath();
        //     return logMethodCall('File.getPath', path);
        // };

        FileClass.exists.overload().implementation = function () {  //  todo by wzy 不加sdk，加hook
            try {
                if (!this || typeof this.$className === 'undefined') {
                    logToFile("[*] File.exists() 在空对象或无效对象上调用");
                    return false;
                }
                if (checkNeedFilter(this.path)) {
                    return false;
                }
                var result = this.exists();
                logToFile(`[*] File.exists() -> ${result}, path:  ${this.path}`);
                return result;
            } catch (e) {
                notifyError(e); // 自定义异常通知逻辑
            }
        };
        FileClass.canWrite.overload().implementation = function () {
            var result = this.canWrite();
            if (checkNeedFilter(this.path)) {
                result = false;
            }
            logToFile(`[*] File.canWrite() -> ${result}, path:  ${this.path}`);
            return result;
        };
        FileClass.canRead.overload().implementation = function () {
            var result = this.canRead();
            if (checkNeedFilter(this.path)) {
                result = false;
            }
            logToFile(`[*] File.canRead() -> ${result}, path:  ${this.path}`);
            return result;
        };
        // FileClass.isFile.overload().implementation = function () {//不能和exists()共同Hook
        //     var result = this.isFile();
        //     if (checkNeedFilter(this.path)) {
        //         return false;
        //     }
        //     logToFile(`[*] File.isFile() -> ${result}, path:  ${this.path}`);
        //     return result;
        // };
        FileClass.canExecute.overload().implementation = function () {
            var result = this.canExecute();
            if (checkNeedFilter(this.path)) {
                result = false;
            }
            logToFile(`[*] File.canExecute() -> ${result}, path: ${this.path} `);
            return result;
        };
        // FileClass.getName.implementation = function () {    //  todo by wzy 不加sdk，加hook
        //     var result = this.getName();
        //     logToFile(`[*] 应用读取File.getName result=${result}` + "  getAbsolutePath===" + this.getAbsolutePath());
        //     return result;
        // };
        // 总空间
        FileClass.getTotalSpace.overload().implementation = function () { // todo by wzy sdk+hook
            var totalSpace = this.getTotalSpace();
            if (userInfoData != null) {
                var statfs = getStatFsByPath(this.path.value)
                if (statfs != null) {
                    totalSpace = statfs.getTotalBytes;
                }
            }
            return logMethodCall('File.getTotalSpace', totalSpace);
        };

        // 空闲空间
        FileClass.getFreeSpace.overload().implementation = function () {    //  todo by wzy 不加sdk，加hook  //TODO by ly 可以加SDK，之后看下怎么处理
            var freeSpace = this.getFreeSpace();
            if (userInfoData != null) {
                var statfs = getStatFsByPath(this.path.value)
                if (statfs != null) {
                    freeSpace = statfs.getFreeBytes + getRandomByteSize();
                }
            }
            return logMethodCall('File.getFreeSpace', freeSpace);
        };
        //  可用空间
        FileClass.getUsableSpace.overload().implementation = function () { // todo by wzy sdk+hook  //TODO by ly 可以加SDK，之后看下怎么处理
            var freeSpace = this.getUsableSpace();
            if (userInfoData != null) {
                var statfs = getStatFsByPath(this.path.value)
                if (statfs != null) {
                    freeSpace = statfs.getAvailableBytes + getRandomByteSize();
                }
            }
            return logMethodCall('File.getUsableSpace', freeSpace);
        };
    } catch (e) {
        notifyError(e);
    }
    try {
        const Files = Java.use('java.nio.file.Files');
        Files.getFileStore.overload('java.nio.file.Path').implementation = function (path) {
            const pathStr = path.toString();
            if (IS_DEBUG) {
                logToFile(`[+] Hooked Files.getFileStore("${pathStr}")`);
            }
            const result = this.getFileStore(path);
            if (IS_DEBUG) {
                logToFile(`[+] Files.getFileStore() 返回的 FileStore 类型: ${result.getClass().getName()}`);
            }
            return createFakeFileStore(result, pathStr);
        };
    } catch (e) {
        notifyError(e);
    }
    // 创建代理类替换 FileStore
    function createFakeFileStore(originalFileStore, pathStr) {
        var statfs = getStatFsByPath(this.pathStr)
        const FakeFileStore = Java.registerClass({
            name: 'sun.nio.fs.AndroidFileStores',
            implements: [Java.use('java.nio.file.FileStore')],
            methods: {
                getTotalSpace: function () {
                    if (IS_DEBUG) {
                        logToFile(`[+] Hooked FileStore.getTotalSpace("${pathStr}")`);
                    }
                    if (statfs != null) {
                        return statfs.getTotalBytes;
                    }
                    return fakeSize;
                },
                getUsableSpace: function () {
                    if (IS_DEBUG) {
                        logToFile(`[+] Hooked FileStore.getUsableSpace("${pathStr}") → ${usable}`);
                    }
                    if (statfs != null) {
                        return statfs.getAvailableBytes + getRandomByteSize();
                    }
                    return usable;
                },
                getFreeSpace: function () {
                    if (statfs != null) {
                        return statfs.getFreeBytes + getRandomByteSize();
                    }
                    return originalFileStore.getFreeSpace();
                },
                name: function () {
                    return originalFileStore.name();
                },
                type: function () {
                    return originalFileStore.type();
                },
                isReadOnly: function () {
                    return originalFileStore.isReadOnly();
                },
                supportsFileAttributeView: function (view) {
                    return originalFileStore.supportsFileAttributeView(view);
                },
                getFileStoreAttributeView: function (view) {
                    return originalFileStore.getFileStoreAttributeView(view);
                }
            }
        });
        // return FakeFileStore.$new();
        return new FakeFileStore();
    }
    try {
        // todo 即 SystemProperties.get   Hook 到 __system_property_get 方法 这块代码可以被 Runtime.exec 给拦截
        Interceptor.attach(Module.findExportByName(null, '__system_property_get'), {    //  todo by wzy sdk+hook
            onEnter: function (args) {
                try {
                    // 读取属性名称
                    var propertyName = Memory.readUtf8String(args[0]);
                    logToFile('请求的系统属性  onEnter:' + propertyName);
                    // 保存输出缓冲区地址以供 onLeave 使用
                    this.outputBuffer = args[1];
                    this.key = propertyName;
                } catch (e) {
                    notifyError(e); //  Interceptor.onEnter__system_property_get
                }
            },
            onLeave: function (retval) {
                try {
                    var length = retval.toInt32();
                    if (length > 0) {
                        // 尝试读取字符串类型的系统属性值
                        var outputBufferContent = Memory.readUtf8String(this.outputBuffer);
                        // 尝试读取其他类型（例如整数）注意：这可能需要根据实际需求和目标平台的内存布局进行调整
                        var outputInt = Memory.readInt(this.outputBuffer);
                        // Memory.writeUtf8String(this.outPtr, "FAKE_SERIAL_123");
                        if (checkNeedHookBy__system_property_getKey(this.key)) {
                            var result = getValueNeedHookBy__system_property_getKey(this.key, userInfoData, deviceInfoData);
                            logToFile('系统属性值(string) onLeave: ' + outputBufferContent + '系统属性值(int): ' + result + "  propertyName===" + this.key);
                            if (result != null) {
                                if (typeof result === 'number' && Number.isInteger(result)) {
                                    Memory.writeU32(this.outputBuffer, parseInt(result))
                                } else {
                                    Memory.writeUtf8String(this.outputBuffer, result);
                                }
                            } else {
                                // Memory.writePointer(this.outputBuffer, ptr[0]);null 写出去
                            }
                        }
                        logToFile('系统属性值(string) onLeave: ' + outputBufferContent + '系统属性值(int): ' + outputInt + "  propertyName===" + this.key);
                    }
                } catch (e) {
                    notifyError(e); //  Interceptor.onLeave__system_property_get
                }
            }
        });
    } catch (e) {
        notifyError(e)
    }
}

// 辅助函数来查找和hook native 方法
function hookFunction(name, onEnterCb, onLeaveCb) {// todo by wzy sdk+hook (封装函数，必须加到sdk和hook，否则无法调用native方法)
    try {
        const address = findFunctionAddress(name);
        if (address === null) return;
        Interceptor.attach(address, {
            onEnter(args) {
                if (typeof onEnterCb === 'function') {
                    try {
                        onEnterCb.apply(this, arguments);
                    } catch (e) {
                        console.error(`[onEnter] ${name} error:`, e);
                    }
                }
            },
            onLeave(retval) {
                if (typeof onLeaveCb === 'function') {
                    try {
                        onLeaveCb.apply(this, arguments);
                    } catch (e) {
                        console.error(`[onLeave] ${name} error:`, e);
                    }
                }
            }
        });

        logToFile(`[hookFunction] 成功 Hook 函数: ${name} @ ${address}`);
    } catch (e) {
        notifyError(`[hookFunction] Hook ${name} 失败:`, e);
    }
}

/**
 * 通过多种方式查找函数在native中的地址
 * @param {*} name 函数名
 * @returns 函数在native中的地址 ，可为空
 */
function findFunctionAddress(name) { // todo by wzy sdk+hook (封装函数，必须加到sdk和hook，否则无法调用native方法)
    let address = null;
    // 尝试 1: 使用 Module.findExportByName(null, name)
    address = Module.findExportByName(null, name);
    if (address !== null) {
        // logToFile(`[findFunctionAddress] 成功找到 (Module.findExportByName): ${name} @ ${address}`);
        return address;
    }

    // 尝试 2: 使用 DebugSymbol.fromModuleNameAndName("libc.so", name)
    const symbols = DebugSymbol.fromModuleNameAndName("libc.so", name);
    if (symbols.length > 0) {
        address = symbols[0].address;
        // logToFile(`[findFunctionAddress] 成功找到 (DebugSymbol.fromModuleNameAndName): ${name} @ ${address}`);
        return address;
    }

    // 尝试 3: 使用 Process.enumerateSymbols()
    Process.enumerateSymbols("libc.so", {
        onMatch: function (sym) {
            if (sym.name === name) {
                address = sym.address;
                // logToFile(`[findFunctionAddress] 成功找到 (Process.enumerateSymbols): ${name} @ ${address}`);
                return "stop";
            }
        },
        onComplete: function () { }
    });
    if (address !== null) {
        return address;
    }
    logToFile(`[findFunctionAddress] 未找到函数: ${name}`);
    return null;
}

// ==================异常配置项 ==================
const TARGET_EXCEPTION_CLASSES = [
    "java.lang.Exception", 'java.lang.Error'
];

const SYSTEM_PACKAGE_PREFIXES = [
    "android.",
    "java.",
    "javax.",
    "kotlin.",
    "com.android.",
    "dalvik.",
    "sun.",
    "org.apache.",
    "org.json.",
    "org.w3c.",
    "androidx.",
    "libcore.",
];

const MAX_STACK_LINES = 20;
let isInsideHook = false; // 防止递归调用

// ================== 工具函数 ==================

function isSystemClass(className) {
    for (let prefix of SYSTEM_PACKAGE_PREFIXES) {
        if (className.startsWith(prefix)) return true;
    }
    return false;
}

function shouldLogStackTrace(stackTrace) {
    for (let i = 0; i < stackTrace.length; i++) {
        let className = stackTrace[i].getClassName();
        if (!isSystemClass(className)) return true;
    }
    return false;
}


// ================== 异常输出 ==================
function logException(exceptionName, message, stackTrace) {
    console.log(`\n\n[+] 异常捕获: ${exceptionName}`);
    if (message && message.value) {
        console.log("Message: " + message.value);
    } else if (message) {
        console.log("Message: " + message);
    }
    console.log("Stack trace:");

    const lines = Math.min(MAX_STACK_LINES, stackTrace.length);
    for (let i = 0; i < lines; i++) {
        try {
            const element = stackTrace[i];
            const className = element.getClassName();
            const methodName = element.getMethodName();
            const fileName = element.getFileName();
            const lineNumber = element.getLineNumber();

            let lineStr = `  at ${className}.${methodName}(${fileName}:${lineNumber})`;
            console.log(lineStr);
        } catch (e) {
            console.warn("[warn] 无法解析堆栈帧:", e);
        }
    }
}

// ================== Hook 异常类 ==================
function hookCustomException(clazzName) {
    try {
        const Clazz = Java.use(clazzName);
        // String 构造函数
        Clazz.$init.overload('java.lang.String').implementation = function (msg) {
            if (isInsideHook) return this.$init(msg);
            isInsideHook = true;
            const realExceptionClass = this.getClass().getName();
            const stackTrace = Java.use("java.lang.Throwable").$new().getStackTrace();
            if (shouldLogStackTrace(stackTrace)) {
                logException(realExceptionClass, msg, stackTrace);
            }
            isInsideHook = false;
            return this.$init(msg);
        };

        // String + Throwable 构造函数
        Clazz.$init.overload('java.lang.String', 'java.lang.Throwable').implementation = function (msg, cause) {
            if (isInsideHook) return this.$init(msg);
            isInsideHook = true;
            let stackTrace = null;
            if (cause !== null && cause.getStackTrace) {
                stackTrace = cause.getStackTrace();
            } else {
                stackTrace = Java.use("java.lang.Throwable").$new().getStackTrace();
            }
            const realExceptionClass = this.getClass().getName();
            if (stackTrace && shouldLogStackTrace(stackTrace)) {
                logException(realExceptionClass, msg, stackTrace);
            }
            isInsideHook = false;
            return this.$init(msg, cause);
        };

        // Throwable 构造函数
        Clazz.$init.overload('java.lang.Throwable').implementation = function (cause) {
            if (isInsideHook) return this.$init(cause);
            isInsideHook = true;
            let stackTrace = null;
            if (cause !== null && cause.getStackTrace) {
                stackTrace = cause.getStackTrace();
            } else {
                stackTrace = Java.use("java.lang.Throwable").$new().getStackTrace();
            }
            const realExceptionClass = this.getClass().getName();
            if (stackTrace && shouldLogStackTrace(stackTrace)) {
                logException(realExceptionClass, null, stackTrace);
            }
            isInsideHook = false;
            return this.$init(cause);
        };

        // 无参构造函数
        Clazz.$init.overload().implementation = function () {
            if (isInsideHook) return this.$init();
            isInsideHook = true;
            const realExceptionClass = this.getClass().getName();
            const stackTrace = Java.use("java.lang.Throwable").$new().getStackTrace();
            if (shouldLogStackTrace(stackTrace)) {
                logException(realExceptionClass, null, stackTrace);
            }
            isInsideHook = false;
            return this.$init();
        };
    } catch (e) {
        console.log(`[-] Failed to hook class ${clazzName}: ${e}`);
    }
}

// ================== 启动入口 ==================
Java.perform(function () {
    TARGET_EXCEPTION_CLASSES.forEach(hookCustomException);
});
//设置系统Build参数。
function setBuildInfo() {
    var Build = Java.use("android.os.Build");
    var BuildVersion = Java.use('android.os.Build$VERSION');
    if (DEVICE_DATA != null) {
        //📱 ‌硬件标识参数-start-
        Build.BRAND.value = DEVICE_DATA.BRAND;
        Build.BOOTLOADER.value = DEVICE_DATA.BOOTLOADER;
        Build.DEVICE.value = DEVICE_DATA.DEVICE;
        Build.HARDWARE.value = DEVICE_DATA.HARDWARE;
        Build.SERIAL.value = DEVICE_DATA.SERIAL;
        Build.SOC_MODEL.value = DEVICE_DATA.SOC_MODEL;
        //💾 ‌CPU 与 ABI 信息-start--
        Build.CPU_ABI.value = DEVICE_DATA.CPU_ABI;
        Build.CPU_ABI2.value = DEVICE_DATA.CPU_ABI2;
        Build.SUPPORTED_ABIS.value = DEVICE_DATA.SUPPORTED_ABIS;
        //品牌与产品信息
        Build.BOARD.value = DEVICE_DATA.BOARD;
        Build.MANUFACTURER.value = DEVICE_DATA.MANUFACTURER;
        Build.PRODUCT.value = DEVICE_DATA.PRODUCT;
        Build.FINGERPRINT.value = DEVICE_DATA.FINGERPRINT;
        Build.MODEL.value = DEVICE_DATA.MODEL;
        Build.SOC_MODEL.value = DEVICE_DATA.SOC_MODEL;
        Build.getRadioVersion.implementation = function () {
            return DEVICE_DATA.getRadioVersion;
        };
    }
    if (USER_DATA != null) {
        //build ==系统构建信息-start-
        Build.DISPLAY.value = USER_DATA.DISPLAY;
        Build.ID.value = USER_DATA.ID;
        Build.TYPE.value = USER_DATA.TYPE;
        Build.TAGS.value = USER_DATA.TAGS;
        Build.TIME.value = USER_DATA.TIME;
        Build.USER.value = USER_DATA.USER;
        Build.HOST.value = USER_DATA.HOST;
        // 🔄 ‌系统版本参数‌-start-
        // logToFile('[*] 系统参数=='+BuildVersion.INCREMENTAL.value+"==="+USER_DATA.VERSION$INCREMENTAL)
        // logToFile('[*] 系统参数RELEASE=='+BuildVersion.RELEASE.value+"==="+USER_DATA.VERSION$RELEASE)
        // logToFile('[*] 系统参数SECURITY_PATCH=='+BuildVersion.SECURITY_PATCH.value+"==="+USER_DATA.VERSION$SECURITY_PATCH)
        // logToFile('[*] 系统参数SDK=='+BuildVersion.SDK.value+"==="+USER_DATA.VERSION$SDK)
        // logToFile('[*] 系统参数SDK_INT=='+BuildVersion.SDK_INT.value+"==="+USER_DATA.VERSION$SDK_INT)
        // logToFile('[*] 系统参数CODENAME=='+BuildVersion.CODENAME.value+"==="+USER_DATA.VERSION$CODENAME)
        BuildVersion.INCREMENTAL.value = USER_DATA.VERSION$INCREMENTAL;
        BuildVersion.RELEASE.value = USER_DATA.VERSION$RELEASE;
        BuildVersion.SECURITY_PATCH.value = USER_DATA.VERSION$SECURITY_PATCH;
        BuildVersion.SDK.value = USER_DATA.VERSION$SDK;
        BuildVersion.SDK_INT.value = USER_DATA.VERSION$SDK_INT;
        BuildVersion.CODENAME.value = USER_DATA.VERSION$CODENAME;
    }
    var Locale = Java.use("java.util.Locale");
    var defaultLocaleCountry = Locale.getDefault().getCountry();
    Locale.getCountry.overload().implementation = function () {
        var result = this.getCountry();
        if (result == defaultLocaleCountry && USER_DATA.country != null) {
            logToFile('[*]Locale.getDefault().getCountry 修改成功==' + USER_DATA.country);
            return USER_DATA.country;
        }
        return result;
    }
}

//TODO by ly SELinux状态，需要SDK，需要HOOK
/**
 * Runtime.exec与ProcessBuilder相关的 hook代码
 * 因为sh相关的参数太多，所以单独罗列出来 Runtime.exec与ProcessBuilder 都可以获取 SELinux状态 cat /proc/meminfo 运行内存大小 | 内存占用 top -n 1 | grep 'cpu  获取 CPU 使用率cat /proc/stat | grep '^cpu ' 
 * 更精确地计算 CPU 使用率 cat /proc/swap SWAP空间大小 SWAP已用大小
 */
function hookCommand() {
    var userInfoData = USER_DATA
    try {
        // 获取 ProcessBuilder 类
        var ProcessBuilder = Java.use('java.lang.ProcessBuilder');
        // Hook ProcessBuilder 构造函数 (List<String> command)
        ProcessBuilder.$init.overload('java.util.List').implementation = function (commandList) {          //TODO by ly Hook
            logToFile("[*] ProcessBuilder(List<String> command) 构造函数调用命令：" + commandList.toString());
            var instance = this.$init(commandList);
            return instance;
        };

        // Hook ProcessBuilder 构造函数 (String... command)
        ProcessBuilder.$init.overload('[Ljava.lang.String;').implementation = function (commands) {      //TODO by ly Hook
            var commandArray = Array.from(commands);
            logToFile("[*] ProcessBuilder(String... command) 构造函数使用命令调用：" + commandArray.join(", "));
            var instance = this.$init(commands);
            return instance;
        };
    } catch (e) {
        notifyError(e); // ProcessBuilder
    }
    // try {
    //     var Runtime = Java.use('java.lang.Runtime');
    //     // 1. exec(String command)
    //     Runtime.exec.overload('java.lang.String').implementation = function (command) { //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         logToFile("[*]  调用了 Runtime.exec(String command)   命令: " + command);
    //         try {
    //             var result = this.exec(command);
    //             logToFile("[*]  调用了 Runtime.exec(String command)   命令: " + command + "  result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); //   Runtime.exec(String command)  调用失败 
    //             throwError(e);
    //         }
    //     };

    //     // 2. exec(String[] cmdArray)
    //     Runtime.exec.overload('[Ljava.lang.String;').implementation = function (cmdArray) { //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         logToFile("[*] 调用了 Runtime.exec(String[] cmdArray)  命令数组: " + JSON.stringify(cmdArray));

    //         try {
    //             var result = this.exec(cmdArray);
    //             logToFile("[*] 调用了 Runtime.exec(String[] cmdArray)   命令数组: " + JSON.stringify(cmdArray) + "  result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); // Runtime.exec(String[] cmdArray)   调用失败 
    //             throwError(e);
    //         }
    //     };

    //     // 3. exec(String command, String[] envp)
    //     Runtime.exec.overload('java.lang.String', '[Ljava.lang.String;').implementation = function (command, envp) {        //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         logToFile("[*] 调用了 Runtime.exec(String command, String[] envp)     命令: " + command + "环境变量: " + JSON.stringify(envp));
    //         try {
    //             var result = this.exec(command, envp);
    //             logToFile("[*] 调用了 Runtime.exec(String command, String[] envp)     命令: " + command + " 环境变量: " + JSON.stringify(envp) + "  result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); // Runtime.exec(String command, String[] envp)    调用失败 
    //             throwError(e);
    //         }
    //     };

    //     // 4. exec(String[] cmdArray, String[] envp)
    //     Runtime.exec.overload('[Ljava.lang.String;', '[Ljava.lang.String;').implementation = function (cmdArray, envp) {        //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         logToFile("[*] 调用了 Runtime.exec(String[] cmdArray, String[] envp)     命令数组: " + JSON.stringify(cmdArray) + " 环境变量: " + JSON.stringify(envp));

    //         try {
    //             var result = this.exec(cmdArray, envp);
    //             logToFile("[*] 调用了 Runtime.exec(String[] cmdArray, String[] envp)     命令数组: " + JSON.stringify(cmdArray) + " 环境变量: " + JSON.stringify(envp) + "  result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); //  Runtime.exec(String[] cmdArray, String[] envp)     调用失败 
    //             throwError(e);
    //         }
    //     };

    //     // 5. exec(String command, String[] envp, File dir)
    //     Runtime.exec.overload('java.lang.String', '[Ljava.lang.String;', 'java.io.File').implementation = function (command, envp, dir) {   //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         let dirPath = dir ? dir.getAbsolutePath() : "null";
    //         logToFile(`[*] 调用了 Runtime.exec(String command, String[] envp, File dir)        命令: ${command}       环境变量: ${JSON.stringify(envp)}        工作目录: ${dirPath}`);
    //         try {
    //             var result = this.exec(command, envp, dir);
    //             logToFile("[*] 调用了 Runtime.exec(String command, String[] envp, File dir)     命令: " + command + " 环境变量: " + JSON.stringify(envp) + +" 工作目录: " + dirPath + " result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); //  Runtime.exec(String command, String[] envp, File dir)     调用失败 
    //             throwError(e);
    //         }
    //     };

    //     // 6. exec(String[] cmdArray, String[] envp, File dir)
    //     Runtime.exec.overload('[Ljava.lang.String;', '[Ljava.lang.String;', 'java.io.File').implementation = function (cmdArray, envp, dir) {   //  todo 不需要SDK，需要hook，hook哪些命令之后提供
    //         let dirPath = dir ? dir.getAbsolutePath() : "null";
    //         logToFile("[*] 调用了 Runtime.exec(String[] cmdArray, String[] envp, File dir)    命令数组: " + JSON.stringify(cmdArray) + "    环境变量: " + JSON.stringify(envp) + "    工作目录: " + dirPath);
    //         try {
    //             var result = this.exec(cmdArray, envp, dir);
    //             logToFile("[*] 调用了 Runtime.exec(String[] cmdArray, String[] envp, File dir)    命令数组: " + JSON.stringify(cmdArray) + "    环境变量: " + JSON.stringify(envp) + "    工作目录: " + dirPath + " result=" + result);
    //             return result;
    //         } catch (e) {
    //             notifyError(e); // Runtime.exec(String[] cmdArray, String[] envp, File dir)    调用失败 
    //             throwError(e);
    //         }
    //     };

    // } catch (e) {
    //     notifyError(e);  // Runtime.exec
    // }
    //hook selinux.so文件读取
    if (IS_HOOK_SO) {
        try {
            // 监控avc日志
            hookFunction('fopen', function (args) {//就一个文件。
                try {
                    const pathPtr = args[0];
                    const path = pathPtr.readCString();

                    this.logPath = path;
                    if (path.includes("avc")) {
                        logToFile("[!] 捕获到对 SELinux AVC 日志的访问: " + path);
                    }
                } catch (e) {
                    notifyError(e); // fopen onEnter 错误
                }
            }, function (retval) {
                const path = this.logPath;
                const filePtr = retval;  // fopen 返回的 FILE* 指针

                logToFile(`[-] fopen 返回值: ${filePtr} (FILE*), 路径: ${path}`);

                if (ptr(filePtr).isNull()) {
                    logToFile(`[!] fopen 打开失败（指针为空）: ${path}`);
                } else {
                    logToFile(`[+] fopen 成功打开文件: ${path}, 返回 FILE* 地址: ${filePtr}`);
                }
            });
        } catch (e) {
            notifyError(e)
        }
        try {
            hookFunction('fopen64', function (args) {
                try {
                    const pathPtr = args[0];
                    const path = pathPtr.readCString();

                    this.logPath = path;
                    if (path.includes("avc")) {
                        logToFile("[!] 捕获到对 SELinux AVC 日志的访问: " + path);
                    }
                } catch (e) {
                    notifyError(e); // fopen onEnter 错误
                }
            }, function (retval) {
                const path = this.logPath;
                const filePtr = retval;  // fopen 返回的 FILE* 指针

                logToFile(`[-] fopen64 返回值: ${filePtr} (FILE*), 路径: ${path}`);

                if (ptr(filePtr).isNull()) {
                    logToFile(`[!] fopen64 打开失败（指针为空）: ${path}`);
                } else {
                    logToFile(`[+] fopen64 成功打开文件: ${path}, 返回 FILE* 地址: ${filePtr}`);
                }
            });
        } catch (e) {
            notifyError(e);
        }
        try {
            hookFunction('open', function (args) {
                try {
                    this.path = args[0].readCString();
                    this.flags = args[1];
                    logToFile(`open 调用: ${this.path}, flags: 0x${this.flags.toString(16)}`);
                } catch (e) {
                    notifyError(e);
                }
            },
                function (retval) {
                    try {
                        const fd = retval.toInt32();
                        if (fd >= 0) {
                            logToFile(`[!] open 成功打开文件: ${this.path}, fd = ${fd}`);
                        } else {
                            logToFile(`[+] open 打开文件失败: ${this.path}`);
                        }
                    } catch (e) {
                        notifyError(e);
                    }
                }
            );
        } catch (e) {
            notifyError(e);
        }

        //拦截获取系统文件读取硬件设备信息
        try {
            /**   
             * "/bin/su", 标准 bin目录||"/sbin/su",  标准sbin目录||"/system/bin/su" 系统 bin 目录||"/system/sbin/su", 系统 sbin 目录|| "/system/xbin/su"备用 xbin 目录||  "/vendor/bin/su"供应商 bin 目录*/
            // 拦截open()调用
            Interceptor.attach(Module.findExportByName("libc.so", "open"), { // todo by wzy 不加sdk，加hook
                onEnter: function (args) {
                    var path = args[0].readUtf8String();
                    //cpu核心线程，cpu信息，温度,cpu调频策略，cpu变频策略路径包含cpufreq/scaling_governor.// gpu
                    // if (path.includes("/sys/devices/system/cpu/") || path.includes("/proc/cpuinfo")
                    //     || path.includes("/sys/class/thermal/thermal_zone") || path.includes("/sys/devices/system/cpu/cpufreq/policy")
                    //     || path.includes("/proc/gpuinfo") || path.includes("/sys/kernel/") || path.includes("/sys/devices") || path.includes("/proc/version")
                    //     || path.includes("/bin/su") || path.includes("/sbin/su") || path.includes("/system/bin/su") || path.includes("/system/sbin/su") || path.includes("/system/xbin/su") || path.includes("/vendor/bin/su")
                    // ) {
                    //     logToFile("[*] 访问CPU/GPU/linux内核/su文件: " + path);
                    // }
                    logToFile("[*] 通过 libc.so 的 open方法 打开文件 : " + path);
                }
            });
        } catch (e) {
            notifyError(e) //  StatFs
        }

        //保留 文件目录是否存在，读写权限。。返回-1；
        hookFunction('access', function (args) {
            try {
                const pathPtr = args[0];
                const mode = args[1];

                this._path = '<无法读取>';
                try {
                    this._path = Memory.readUtf8String(pathPtr);
                } catch (e) {
                    // Ignore
                }

                this._mode = mode;

                logToFile(`调用了 access: 路径=${this._path}, 模式=${this._mode}`);
            } catch (e) {
                notifyError(e); // onEnter for access
            }
        }, function (retval) {
            try {
                if (this.returnOverride !== undefined) {
                    logToFile(`强制 access 返回值: ${this.returnOverride} | 路径=${this._path}, 模式=${this._mode}`);
                    retval.replace(this.returnOverride);
                    delete this.returnOverride;
                } else {
                    logToFile(`access 返回值: ${retval.toInt32()} | 路径=${this._path}, 模式=${this._mode}`);
                }
            } catch (e) {
                notifyError(e); // onLeave for access
            }
        });
        hookFunction('execve', function (args) { // todo by wzy sdk+hook
            try {
                // 保存 filename
                try {
                    this._filename = Memory.readUtf8String(args[0]);
                } catch (e) {
                    this._filename = '<无法读取>';
                }
                // 保存 argv[]
                this._argv = [];
                try {
                    for (let i = 0; ; i++) {
                        let ptr = Memory.readPointer(args[1].add(Process.pointerSize * i));
                        if (ptr.isNull()) break;
                        let arg = Memory.readUtf8String(ptr);
                        this._argv.push(arg);
                    }
                } catch (e) {
                    this._argv = ['<无法读取参数>'];
                }
                // 保存 envp[]
                this._envp = [];
                try {
                    for (let i = 0; ; i++) {
                        let ptr = Memory.readPointer(args[2].add(Process.pointerSize * i));
                        if (ptr.isNull()) break;
                        let env = Memory.readUtf8String(ptr);
                        this._envp.push(env);
                    }
                } catch (e) {
                    this._envp = ['<无法读取环境变量>'];
                }

                logToFile(`execve 被调用了: ${this._filename} 参数: [${this._argv.join(', ')}] 环境变量: [${this._envp.join(', ')}]`);

            } catch (e) {
                notifyError(e);
            }
        }, function (retval) {
            // 使用之前保存的数据
            const filename = this._filename || '<未知>';
            const argv = this._argv ? `[${this._argv.join(', ')}]` : '<无法读取>';
            const envp = this._envp ? `[${this._envp.join(', ')}]` : '<无法读取>';

            logToFile(`execve 返回值: ${retval} | 命令: ${filename} 参数: ${argv} 环境变量: ${envp}`);
        });
    }
}

//******************************  网络相关 {宏替换常量}  *******************************
var FAKE_IP = "192.168.0.101";         // 伪造内网 IP
var FAKE_GATEWAY_V4 = "192.168.0.1"; // IPv4 伪造网关
var FAKE_GATEWAY_V6 = "2001:db8::1"; // IPv6 伪造网关

var FAKE_IP_BASE = "192.168.0.";
var FAKE_IP_START = 101;
// 自定义伪造的 DNS 列表（长度可以大于系统 DNS 数量）
var FAKE_DNS_LIST = ["8.8.4.4", "1.1.1.1", "8.8.8.8", "9.9.9.9", "1.0.0.1", "149.112.112.112", "208.67.222.222", "208.67.220.220"];

// const fakeArpOutput =
//     "IP address       HW type     Flags       HW address           Mask     Device\n" +
//     "192.168.0.1     0x1         0x2         00:11:22:33:44:55    *        eno0\n" +
//     "192.168.0.254   0x1         0x2         AA:BB:CC:DD:EE:FF    *        eno0\n";

var fakeIpRouteOutput =
    "192.168.0.0/24 dev wlan0 proto kernel scope link src 192.168.0.101 \n" + //  这个ip后面的空格一定要保留！！！！！！！！！！！！！！！！
    "default via 192.168.1.1 dev eno0";

// const fakeIfconfigOutput =
//     "eno0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n" +
//     "      inet 192.168.0.101  netmask 255.255.255.0  broadcast 192.168.0.255\n" +
//     "      inet6 fe80::dead:beef:cafe:face  prefixlen 64  scopeid 0x20<link>\n" +
//     "      ether 00:11:22:33:44:55  txqueuelen 1000  (Ethernet)\n" +
//     "      RX packets 123456  bytes 123456789 (117.7 MiB)\n" +
//     "      TX packets 98765   bytes 987654321 (942.0 MiB)\n";

// 判断传入的 IP 字符串是否是以 10. 或 172. 开头
function isPrivateIp(ip) {
    const strIp = String(ip);   // 统一转成字符串 

    // 判断是否为空
    if (!strIp || strIp.trim() === '') {
        return false;
    }

    // 匹配 10.x.x.x
    if (strIp.startsWith("10.")) {
        return true;
    }

    // 匹配 172.16.x.x ~ 172.31.x.x
    if (strIp.startsWith("172.")) {
        const parts = strIp.split(".");
        if (parts.length === 4) {
            const secondOctet = parseInt(parts[1], 10);
            if (!isNaN(secondOctet) && secondOctet >= 16 && secondOctet <= 31) {
                return true;
            }
        }
    }
    return false;
}

// 将 IP 字符串转换为网络字节序的 uint32_t
function ipToRawU32(ip) {
    const parts = ip.split('.').map(Number);
    const u32 = (
        (parts[0] << 24) |
        (parts[1] << 16) |
        (parts[2] << 8) |
        (parts[3])
    ) >>> 0;

    const buf = Memory.alloc(4);
    buf.writeU32(0x12345678);

    const b = buf.readByteArray(4);

    if (b[0] === 0x78) {
        // 主机字节序是小端，直接写入即可
        return u32;
    }
    // 大端架构下需要手动转成大端格式（即网络字节序）
    return ((u32 & 0xFF000000) >>> 24) |
        ((u32 & 0x00FF0000) >>> 8) |
        ((u32 & 0x0000FF00) << 8) |
        ((u32 & 0x000000FF) << 24);
}

function rawU32ToIp(u32) {
    const buf = Memory.alloc(4);
    buf.writeU32(u32);
    const bytes = buf.readByteArray(4);
    return `${bytes[0] & 0xFF}.${(bytes[1] & 0xFF)}.${(bytes[2] & 0xFF)}.${(bytes[3] & 0xFF)}`;
}
// 验证字符串是否为合法 IPv4 地址
function isValidIPv4(ip) {
    const ipv4Regex = /^(\d{1,3}\.){3}\d{1,3}$/;
    if (!ipv4Regex.test(ip)) return false;

    const parts = ip.split(".").map(Number);
    return parts.every(p => p >= 0 && p <= 255);
}
/**
 * native 层hook ip、网关、dns
 */
function hookNativeNetWork() {
    if (IS_HOOK_SO) {
        try {
            const getifaddrs = Module.findExportByName("libc.so", "getifaddrs");
            if (getifaddrs !== null) {
                Interceptor.attach(getifaddrs, {
                    onEnter: function (args) {
                        this.ifap = args[0]; // struct ifaddrs**
                    },
                    onLeave: function (retval) {
                        if (retval.toInt32() !== 0) {
                            logToFile("[-] getifaddrs failed with error code: " + retval.toInt32());
                            return;
                        }

                        const ifa_head = this.ifap.readPointer();
                        let current = ifa_head;

                        while (!current.isNull()) {
                            const ifa_addr_offset = Process.pointerSize * 3;
                            const addrPtr = current.add(ifa_addr_offset).readPointer();

                            if (!addrPtr.isNull()) {
                                const sa_family = Memory.readU16(addrPtr);
                                if (sa_family === 2) { // AF_INET == 2
                                    const s_addr = Memory.readU32(addrPtr.add(4)); // offset 4

                                    // 跳过回环地址
                                    if ((s_addr & 0xFF) === 127) {
                                        logToFile(`[i] 跳过回环地址 127.0.0.1`);
                                        current = current.readPointer();
                                        continue;
                                    }

                                    const originalIp = rawU32ToIp(s_addr);

                                    // 判断是否为私有地址
                                    if (isPrivateIp(originalIp)) {
                                        const fakeIpRaw = ipToRawU32(FAKE_IP);
                                        try {
                                            Memory.writeU32(addrPtr.add(4), fakeIpRaw);
                                            logToFile(`[+] 使用 getifaddrs 替换私有 IP: ${originalIp} → ${FAKE_IP}`);
                                        } catch (e) {
                                            logToFile(`[-] 写内存失败: ${e.message}`);
                                        }
                                    } else {
                                        logToFile(`[i] 跳过公网或非私有 IP: ${originalIp}`);
                                    }
                                }
                            }

                            // 获取下一个节点
                            current = current.readPointer(); // ifa_next
                        }
                    }
                });
            } else {
                logToFile("[-] 未找到 getifaddrs");
            }
        } catch (e) {
            logToFile('当前设备未找到 getifaddrs 方法');
        }

        try {
            const ioctl = Module.findExportByName("libc.so", "ioctl");
            if (ioctl !== null) {
                Interceptor.attach(ioctl, {
                    onEnter: function (args) {
                        const cmd = args[1].toInt32();
                        if (cmd === 0x8915) { // SIOCGIFADDR
                            this.cmd = cmd;
                            this.interfaceName = Memory.readUtf8String(args[0].add(4));
                            logToFile(`[+] Hook ioctl(SIOCGIFADDR) for interface: ${this.interfaceName}`);
                        }
                    },
                    onLeave: function (retval) {
                        if (this.cmd === 0x8915) {
                            const addrPtr = args[0].add(20); // struct sockaddr_in offset
                            const originalIpRaw = Memory.readU32(addrPtr.add(4)); // s_addr
                            const originalIp = rawU32ToIp(originalIpRaw);

                            // 判断是否为私有地址
                            if (isPrivateIp(originalIp)) {
                                try {
                                    const fakeIpRaw = ipToRawU32(FAKE_IP);
                                    Memory.writeU32(addrPtr.add(4), fakeIpRaw);
                                    logToFile(`[+] ioctl 修改私有 IP: ${originalIp} → ${FAKE_IP}`);
                                } catch (e) {
                                    logToFile(`[-] ioctl 写内存失败: ${e.message}`);
                                }
                            } else {
                                logToFile(`[i] ioctl 跳过非私有 IP: ${originalIp}`);
                            }
                        }
                    }
                });
            } else {
                logToFile("[-] 未找到 ioctl");
            }
        } catch (e) {
            logToFile('当前设备未找到 ioctl 方法');
        }

        // Hook fopen + fgets, 模拟 resolv.conf 文件内容
        try {
            const fopen = Module.findExportByName("libc.so", "fopen");
            if (fopen !== null) {
                Interceptor.attach(fopen, {
                    onEnter: function (args) {
                        const path = Memory.readUtf8String(args[0]);
                        if (path === "/etc/resolv.conf") {
                            logToFile("[+] fopen('/etc/resolv.conf') 被调用");
                            this.hooked = true; // 使用 this 而非全局变量
                        }
                    },
                    onLeave: function (retval) {
                        return retval;
                    }
                });
            }

            const fgets = Module.findExportByName("libc.so", "fgets");
            if (fgets !== null) {
                Interceptor.attach(fgets, {
                    onEnter: function (args) {
                        this.buf = args[0];
                        this.size = args[1];
                        this.stream = args[2];
                    },
                    onLeave: function (retval) {
                        if (this.hooked && !retval.isNull()) {
                            const line = Memory.readUtf8String(this.buf);
                            if (line.startsWith("nameserver ")) {
                                const originalDns = line.trim().split(' ')[1];

                                if (isPrivateIp(originalDns)) {
                                    try {
                                        const fakeDnsLine = `nameserver ${FAKE_DNS}\n`;
                                        Memory.writeUtf8String(this.buf, fakeDnsLine);
                                        logToFile(`[+] fopen+fgets修改 DNS, 原始DNS: ${originalDns} , 新的DNS: ${FAKE_DNS}`);
                                    } catch (e) {
                                        logToFile(`[-] fgets 写内存失败: ${e.message}`);
                                    }
                                } else {
                                    logToFile(`[i] fgets 跳过不符合条件的DNS: ${originalDns}`);
                                }
                            }
                        }
                    }
                });
            }
        } catch (e) {
            logToFile('当前设备未找到 fopen或fgets 方法');
        }

        // Hook __res_ninit, 直接修改 DNS 地址
        try {
            const res_ninit = Module.findExportByName("libc.so", "__res_ninit");
            if (res_ninit !== null) {
                Interceptor.attach(res_ninit, {
                    onEnter: function (args) {
                        const res = args[0]; // res_state 结构体指针
                        const nsaddr_list_offset = Process.pointerSize * 5;

                        for (let i = 0; i < 2; i++) { // 最多两个 DNS 服务器
                            const addrPtr = res.add(nsaddr_list_offset + i * 16); // 每个地址占16字节
                            const originalIpRaw = addrPtr.readU32();
                            const originalIp = rawU32ToIp(originalIpRaw);

                            if (originalIp === "0.0.0.0") {
                                continue;
                            }
                            if (!isPrivateIp(originalIp)) {
                                logToFile(`[+] __res_ninit 跳过DNS修改, 原始DNS: ${originalIp} `);
                                continue;
                            }
                            let fakeDns = FAKE_DNS_LIST[i];
                            const dnsIpRaw = IpUtils.ipToRawU32(fakeDns);
                            addrPtr.writeU32(dnsIpRaw); // IPv4 地址写入
                            logToFile(`[+] __res_ninit 修改 DNS, 原始DNS: ${originalIp} , 新的DNS:  ${fakeDns}`);
                        }
                    }
                });
            } else {
                logToFile("[-] 未找到 __res_ninit");
            }
        } catch (e) {
            logToFile('当前设备未找到 __res_ninit 方法');
        }

        // Hook inet_ntop（将 struct in_addr 转换为字符串）
        try {
            var inet_ntop = Module.findExportByName(null, "inet_ntop");
            if (inet_ntop) {
                Interceptor.attach(inet_ntop, {
                    onEnter: function (args) {
                        const af = args[0].toInt32(); // 地址族
                        if (af === 2) { // AF_INET (IPv4)
                            logToFile("[inet_ntop] 处理 IPv4 地址");
                        } else if (af === 10) { // AF_INET6 (IPv6)
                            logToFile("[inet_ntop] 跳过 IPv6 地址");
                        }
                    },
                    onLeave: function (retval) {
                        if (retval.isNull()) {
                            logToFile("[inet_ntop] inet_ntop 返回空");
                            return;
                        }

                        const ipStr = retval.readUtf8String();
                        if (!ipStr) {
                            logToFile("[inet_ntop] 无法读取 IP 字符串");
                            return;
                        }

                        if (isPrivateIp(ipStr)) {
                            try {
                                Memory.writeUtf8String(retval, FAKE_IP);
                                logToFile(`[inet_ntop] 替换私有 IP: ${ipStr} → ${FAKE_IP}`);
                            } catch (e) {
                                logToFile(`[-] inet_ntop 写内存失败: ${e.message}`);
                            }
                        } else {
                            logToFile(`[i] inet_ntop 跳过非私有 IP: ${ipStr}`);
                        }

                        return retval;
                    }
                });
            } else {
                logToFile("[-] 未找到 inet_ntop");
            }
        } catch (e) {
            logToFile(`[-] Hook inet_ntop 异常: ${e.message}`);
        }
    }
    // Hook C 层 __system_property_get(net.dns*)
    try {
        const __system_property_get = Module.findExportByName(null, "__system_property_get");
        if (__system_property_get !== null) {
            Interceptor.attach(__system_property_get, {
                onEnter: function (args) {
                    this.key = Memory.readUtf8String(args[0]);
                    this.value = args[1];
                },
                onLeave: function (retval) {
                    if (this.key && this.value && this.key.startsWith("net.dns")) {
                        // 提取 net.dns1/2/3 的编号
                        const indexStr = this.key.match(/\d+/);
                        if (!indexStr || !indexStr.length) {
                            logToFile(`[-] 无法解析 DNS 键名索引: ${this.key}`);
                            return;
                        }

                        const index = parseInt(indexStr[0], 10) - 1; // dns1 -> index 0
                        if (index < 0 || index >= FAKE_DNS_LIST.length) {
                            logToFile(`[-] DNS 索引超出范围: ${this.key}`);
                            return;
                        }

                        const fakeDns = FAKE_DNS_LIST[index];
                        if (!isValidIPv4(fakeDns)) {
                            logToFile(`[-] 伪造 DNS 地址无效: ${fakeDns}`);
                            return;
                        }

                        try {
                            Memory.writeUtf8String(this.value, fakeDns);
                            logToFile(`[+] Hooked ${this.key} -> 修改为 ${fakeDns}`);
                        } catch (e) {
                            logToFile(`[-] 写入伪造 DNS 失败: ${e.message}`);
                        }
                    }
                }
            });
        } else {
            logToFile("[-] 未找到 __system_property_get");
        }
    } catch (e) {
        logToFile(`[-] Hook __system_property_get 异常: ${e.message}`);
    }
}
/**
 * 也是修改内网IP的，但主要是sh及文件流方式获取的：cat /proc/net/arp 、ip route 、 ifconfig
 */
function hookInternalNetIP() {
    const Runtime = Java.use("java.lang.Runtime");
    const Process = Java.use("java.lang.Process");
    const OutputStream = Java.use("java.io.OutputStream");
    const ByteArrayInputStream = Java.use("java.io.ByteArrayInputStream");

    // 工具函数：构造伪造的 InputStream
    function makeFakeInputStream(data) {
        const ByteArrayOutputStream = Java.use("java.io.ByteArrayOutputStream");
        const out = ByteArrayOutputStream.$new();

        const PrintStream = Java.use("java.io.PrintStream");
        const printStream = PrintStream.$new(out);
        printStream.print(data);
        printStream.flush();

        return ByteArrayInputStream.$new(out.toByteArray());
    }


    Runtime.exec.overload("[Ljava.lang.String;").implementation = function (cmdArray) {
        return handleExec(this, cmdArray.join(" "), cmdArray);
    };
    Runtime.exec.overload("java.lang.String", "[Ljava.lang.String;").implementation = function (cmd, envp) {
        return handleExec(this, cmd, cmd);
    };
    Runtime.exec.overload("[Ljava.lang.String;", "[Ljava.lang.String;").implementation = function (cmdArray, envp) {
        return handleExec(this, cmdArray.join(" "), cmdArray);
    };

    const ProcessBuilder = Java.use("java.lang.ProcessBuilder");

    ProcessBuilder.start.implementation = function () {
        try {
            const commands = this.command().toArray();
            const cmdStr = commands.map(c => c.toString()).join(" ");
            const lowerCmd = cmdStr.toLowerCase();

            let fakeData = null;

            // if (lowerCmd.includes("cat") && lowerCmd.includes("arp")) {
            // fakeData = fakeArpOutput;
            // } else 
            if (lowerCmd.includes("ip") && lowerCmd.includes("route")) {
                fakeData = fakeIpRouteOutput;
            }
            // else if (lowerCmd.includes("ifconfig")) {
            // fakeData = fakeIfconfigOutput;
            // }

            if (fakeData !== null) {
                logToFile(`[+] Faking output for ProcessBuilder command: ${cmdStr}`);
                return createFakeProcess(fakeData);
            }

            return this.start();
        } catch (e) {
            notifyError("[-] ProcessBuilder.start hook error:", e.message);
            return this.start();
        }
    };

    // ========== Hook FileInputStream("/proc/net/arp") ==========
    const FileInputStream = Java.use("java.io.FileInputStream");

    // FileInputStream.$init.overload("java.lang.String").implementation = function (path) {
    //     if (path === "/proc/net/arp" || path === "proc/net/arp") {
    //         logToFile("[+] Faking read from /proc/net/arp via FileInputStream");
    //         return makeFakeInputStream(fakeArpOutput);
    //     }

    //     return this.$init(path);
    // };

    // ========== 统一 exec 处理函数 ==========
    function handleExec(runtimeObj, cmdStr, originalCmd) {
        try {
            const lowerCmd = cmdStr.toLowerCase();
            let fakeData = null;

            // if (lowerCmd.includes("cat") && lowerCmd.includes("arp")) {
            //     fakeData = fakeArpOutput;
            // } else 
            if (lowerCmd.includes("ip") && lowerCmd.includes("route")) {
                fakeData = fakeIpRouteOutput;
            }
            //  else if (lowerCmd.includes("ifconfig")) {
            //     fakeData = fakeIfconfigOutput;
            // }

            if (fakeData !== null) {
                logToFile(`[+] Faking output for command: ${originalCmd}`);
                return createFakeProcess(fakeData);
            }

            return runtimeObj.exec.apply(runtimeObj, arguments);
        } catch (e) {
            notifyError("[-] Runtime.exec hook error:", e.message);
            return runtimeObj.exec.apply(runtimeObj, arguments);
        }
    }

    // ========== 创建伪造 Process ==========
    function createFakeProcess(fakeData) {
        const FakeProcess = Java.registerClass({
            name: "com.android.Process02",
            superClass: Process,
            methods: {
                getOutputStream: {
                    returnType: 'java.io.OutputStream',
                    argumentTypes: [],
                    implementation: function () {
                        return OutputStream.nullOutputStream();
                    }
                },
                getInputStream: {
                    returnType: 'java.io.InputStream',
                    argumentTypes: [],
                    implementation: function () {
                        return makeFakeInputStream(fakeData);
                    }
                },
                getErrorStream: {
                    returnType: 'java.io.InputStream',
                    argumentTypes: [],
                    implementation: function () {
                        return ByteArrayInputStream.$new("".getBytes());
                    }
                },
                waitFor: {
                    returnType: 'int',
                    argumentTypes: [],
                    implementation: function () { return 0; }
                },
                exitValue: {
                    returnType: 'int',
                    argumentTypes: [],
                    implementation: function () { return 0; }
                },
                destroy: {
                    returnType: 'void',
                    argumentTypes: [],
                    implementation: function () { }
                }
            }
        });

        return FakeProcess.$new();
    }
}
/**
 * java层hook 网络信息: ip、网关、dns
 * hook 网关和dns的代码必须要写在ip之前，否则在云手机中不会生效，顺序不能错！！！
 */
function hookJavaNetWork() {
    const LinkProperties = Java.use("android.net.LinkProperties");
    LinkProperties.getDnsServers.overload().implementation = function () {
        const fakeList = ArrayList.$new();

        // 获取原始 DNS 列表
        const originalList = this.getDnsServers();
        const size = originalList.size();

        for (let i = 0; i < size; i++) {
            try {
                const originalAddress = originalList.get(i);
                const getHostAddress = originalAddress.getClass().getMethod("getHostAddress", null);
                const originalDns = Java.cast(getHostAddress.invoke(originalAddress, null), Java.use('java.lang.String'));

                if (isPrivateIp(originalDns)) {
                    const fakeDns = FAKE_DNS_LIST[i];
                    const inetAddress = InetAddress.getByName(fakeDns);
                    fakeList.add(inetAddress);
                    logToFile(`[+] getDnsServers()替换 DNS ${i}: ${originalDns} -> ${fakeDns}`);
                } else {
                    logToFile(`[+] 局域网 DNS ${originalDns}，跳过替换`);
                    fakeList.add(originalAddress);
                }
            } catch (e) {
                logToFile(`[!] getDnsServers()替换 DNS 失败: ${e.message}`);
            }
        }

        return fakeList;
    };
    const RouteInfo = Java.use("android.net.RouteInfo");
    RouteInfo.getGateway.overload().implementation = function () {
        try {
            // 获取原始网关地址
            const originalGateway = this.getGateway();
            if (!originalGateway) {
                logToFile("[!] 原始网关为空");
                return originalGateway;
            }
            const originalIp = originalGateway.getHostAddress();

            if (!isPrivateIp(originalIp)) {
                logToFile(`[+] 局域网网关 ${originalIp}，跳过替换`);
                return originalGateway;
            }
            // 判断原始网关是 IPv4 还是 IPv6
            const isIPv4 = originalGateway.getClass().getName() == "java.net.Inet4Address";

            let fakeIp = null;
            if (isIPv4) {
                fakeIp = FAKE_GATEWAY_V4;
            } else {
                logToFile(`[+] 跳过IPv6地址: ${originalIp}`);
                return originalGateway;
            }

            if (originalIp === "0.0.0.0" || originalIp === "::") {
                return originalGateway;
            }
            const fakeAddress = InetAddress.getByName(fakeIp);
            logToFile(`[+] RouteInfo.getGateway() 替换网关: ${originalIp} -> ${fakeIp}`);
            return fakeAddress;
        } catch (e) {
            logToFile(`[!] RouteInfo.getGateway() 替换网关失败: ${e.message}`);
            return this.getGateway(); // 回退到原始值
        }
    };

    const InetAddress = Java.use('java.net.InetAddress');
    InetAddress.getHostAddress.implementation = function () {
        let ip = this.getHostAddress();
        if (ip.includes(":")) return ip; // IPv6 不处理

        if (isPrivateIp(ip)) {
            logToFile(`[伪装IP] 原始地址: ${ip} → 替换为 ${FAKE_IP}`);
            return FAKE_IP;
        }
        return ip;
    };
    InetAddress.getLocalHost.overload().implementation = function () {
        var originalAddress = this.getLocalHost();
        var hostAddress = originalAddress.getHostAddress();
        var hostName = originalAddress.getHostName();

        // 判断是否是 IPv6 地址
        if (hostAddress.indexOf(":") !== -1) {
            logToFile("[IPv6] 检测到 IPv6 地址，跳过替换");
            return originalAddress;
        }
        // 只处理目标ip
        if (isPrivateIp(hostAddress)) {
            return originalAddress;
        }
        // 使用 InetAddress.getByAddress 构造新的 InetAddress 实例
        var fakeBytes = null;
        try {
            // 获取伪造 IP 的字节数组形式
            fakeBytes = InetAddress.getByName(FAKE_IP).getAddress();
        } catch (e) {
            logToFile("[!] 构造伪造 IP 失败: ", e.message);
            return originalAddress;
        }
        var forgedAddress = InetAddress.getByAddress(hostName, fakeBytes);
        logToFile(`[原值] 主机名: ${hostName}, IP地址: ${hostAddress} ,,, [最终返回] ${forgedAddress.toString()}`);
        return forgedAddress;
    };

    InetAddress.getAllByName.overload("java.lang.String").implementation = function (host) {
        let originalAddresses;
        try {
            originalAddresses = this.getAllByName(host);
        } catch (e) {
            logToFile(`[getAllByName] 调用失败: ${e.message}`);
            return [];
        }
        const modifiedAddresses = [];

        for (let i = 0; i < originalAddresses.length; i++) {
            const addr = originalAddresses[i];
            const ip = addr.getHostAddress();

            if (isPrivateIp(ip)) {
                const fakeIp = FAKE_IP_BASE + fakeIpCounter++;
                logToFile(`[getAllByName] 替换私有 IP: ${ip} → ${fakeIp}`);
                const fakeAddr = InetAddress.getByName(fakeIp);
                modifiedAddresses.push(fakeAddr);
            } else {
                modifiedAddresses.push(addr);
            }
        }
        return Java.array("java.net.InetAddress", modifiedAddresses);
    };

    const WifiInfo = Java.use('android.net.wifi.WifiInfo');
    WifiInfo.getIpAddress.overload().implementation = function () {
        const originalIp = Java.cast(this.getIpAddress(), InetAddress).getHostAddress();
        if (isPrivateIp(originalIp)) {
            logToFile("[WifiInfo] 获取到 Wi-Fi IP → 替换为 " + FAKE_IP);
            return InetAddress.parseNumericAddress(FAKE_IP).hashCode();
        }
        logToFile(`[+] 局域网 Wi-Fi IP ${originalIp}，跳过替换`);
        return this.getIpAddress(); // 返回原值
    };


    const ConnectivityManager = Java.use("android.net.ConnectivityManager");
    ConnectivityManager.getLinkProperties.overload('android.net.Network').implementation = function (network) {
        const result = this.getLinkProperties(network);
        if (!result) return result;

        const originalAddresses = result.getLinkAddresses();
        if (!originalAddresses) return result;

        const newAddresses = ArrayList.$new();
        let fakeIpCounter = FAKE_IP_START;

        for (let i = 0; i < originalAddresses.size(); i++) {
            const linkAddr = originalAddresses.get(i);

            let inetAddr = null;
            try {
                const addrMethod = linkAddr.getClass().getMethod("getAddress", []);
                inetAddr = addrMethod.invoke(linkAddr, []);
            } catch (e) {
                logToFile(`[!] getAddress() 调用失败: ${e.message}`);
                continue;
            }

            if (inetAddr == null) {
                logToFile(`[!] InetAddress 为空`);
                continue;
            }

            // 强制类型转换为 InetAddress
            const InetAddress = Java.use("java.net.InetAddress");
            inetAddr = Java.cast(inetAddr, InetAddress);

            let ipStr = null;
            try {
                const hostAddrMethod = inetAddr.getClass().getMethod("getHostAddress", []);
                ipStr = hostAddrMethod.invoke(inetAddr, []);

                if (ipStr && ipStr.toString) {
                    ipStr = ipStr.toString(); // 安全转为 JS 字符串
                } else {
                    throw new Error("IP 地址格式异常");
                }

            } catch (e) {
                logToFile(`[!] getHostAddress() 调用失败或结果无效: ${e.message}`);
                newAddresses.add(linkAddr);
                continue;
            }

            // 此时 ipStr 是 JS 字符串，可以直接判断
            if (!ipStr) {
                logToFile(`[!] IP 地址为空`);
                newAddresses.add(linkAddr);
                continue;
            }
            // 跳过 IPv6
            if (ipStr.includes(":")) {
                logToFile(`[IPv6] 跳过: ${ipStr}`);
                newAddresses.add(linkAddr);
                continue;
            }

            // 如果不是私有 IP，保留
            if (!isPrivateIp(ipStr)) {
                newAddresses.add(linkAddr);
                continue;
            }

            // 构造伪造 IP
            const fakeIp = FAKE_IP_BASE + fakeIpCounter++;
            let fakeInetAddress = null;
            try {
                fakeInetAddress = InetAddress.getByName(fakeIp);
            } catch (e) {
                logToFile(`[!] 构造伪造 IP 失败: ${fakeIp}，错误: ${e.message}`);
                newAddresses.add(linkAddr);
                continue;
            }

            const LinkAddress = Java.use("android.net.LinkAddress");
            const fakeLinkAddress = LinkAddress.$new(fakeInetAddress, 24);
            logToFile(`[getLinkProperties] 替换私有 IP: ${ipStr} → ${fakeIp}`);
            newAddresses.add(fakeLinkAddress);
        }

        result.setLinkAddresses(newAddresses);
        return result;
    };

    const NetworkInterface = Java.use("java.net.NetworkInterface");
    const Collections = Java.use("java.util.Collections");
    const ArrayList = Java.use("java.util.ArrayList");

    NetworkInterface.getNetworkInterfaces.implementation = function () {
        const originalInterfaces = this.getNetworkInterfaces(); // Enumeration<NetworkInterface>
        const modifiedInterfaces = [];

        while (originalInterfaces.hasMoreElements()) {
            const intfWrapper = originalInterfaces.nextElement();
            const intf = Java.cast(intfWrapper, NetworkInterface);

            const name = intf.getName();
            const index = intf.getIndex();

            const getInetAddressesMethod = intf.getClass().getMethod("getInetAddresses", null);
            const originalAddresses = Java.cast(
                getInetAddressesMethod.invoke(intf, null),
                Java.use("java.util.Enumeration")
            );

            const filteredAddresses = [];
            let fakeIpCounter = FAKE_IP_START; // 每个网卡单独计数

            while (originalAddresses.hasMoreElements()) {
                const addrWrapper = originalAddresses.nextElement();
                const addr = Java.cast(addrWrapper, InetAddress);

                const ip = addr.getHostAddress();
                // 跳过 IPv6 地址
                if (ip.includes(":")) {
                    filteredAddresses.push(addr);
                    continue;
                }
                // 判断是否为私有 IP
                if (isPrivateIp(ip)) {
                    const fakeIp = FAKE_IP_BASE + fakeIpCounter++;
                    try {
                        const fakeAddr = InetAddress.getByName(fakeIp);
                        logToFile(`[getNetworkInterfaces] 替换私有 IP: ${ip} → ${fakeIp}`);
                        filteredAddresses.push(fakeAddr);
                    } catch (e) {
                        logToFile(`[getNetworkInterfaces] 构造伪造 IP 失败: ${fakeIp}`, e.message);
                        filteredAddresses.push(addr); // 回退到原地址
                    }
                } else {
                    // 非私有 IP 直接保留（如 192.168.x.x） 
                    filteredAddresses.push(addr);
                }
            }

            const InetAddressArray = Java.array("java.net.InetAddress", filteredAddresses);
            const newIntf = NetworkInterface.$new.overload(
                "java.lang.String",
                "int",
                "[Ljava.net.InetAddress;"
            ).call(NetworkInterface, name, index, InetAddressArray);

            modifiedInterfaces.push(newIntf);
        }

        const javaList = ArrayList.$new();
        for (let i = 0; i < modifiedInterfaces.length; i++) {
            javaList.add(modifiedInterfaces[i]);
        }
        return Collections.enumeration(javaList);
    };
}
/**
 * 去掉异常栈帧
 */
function removeExceptionStack() {
    try {
        // 获取类引用
        var Throwable = Java.use("java.lang.Throwable");
        var StackTraceElement = Java.use("java.lang.StackTraceElement");

        // Hook getStackTrace()
        if (Throwable.getStackTrace && Throwable.getStackTrace.overload) {
            Throwable.getStackTrace.overload().implementation = function () {
                try {
                    // 获取原始栈
                    var originalStack = this.getStackTrace();

                    // 过滤掉 Frida 相关帧
                    var filtered = [];
                    for (var i = 0; i < originalStack.length; i++) {
                        var element = originalStack[i];
                        var className = element.getClassName();
                        var methodName = element.getMethodName();

                        // 判断是否为 Frida 插入的帧
                        if (
                            className.includes("frida") ||
                            methodName.includes("frida") ||
                            methodName.includes("invoke") ||
                            methodName.includes("hooked") ||
                            methodName.includes("proxy") ||
                            methodName.includes("Java.perform") ||
                            className.includes("com.android.internal.os.ZygoteInit")
                        ) {
                            continue;
                        }

                        // 保留原生帧
                        filtered.push(element);
                    }

                    // 如果过滤后为空，伪造一个“原生”栈
                    if (filtered.length === 0) {
                        filtered = [
                            StackTraceElement.$new("android.app.Activity", "onCreate", "Activity.java", 9012),
                            StackTraceElement.$new("android.app.Instrumentation", "callActivityOnCreate", "Instrumentation.java", 1300),
                            StackTraceElement.$new("com.example.app.MainActivity", "onCreate", "MainActivity.java", 50)
                        ];
                    }

                    // 返回过滤后的栈
                    return Java.array("java.lang.StackTraceElement", filtered);
                } catch (e) {
                    // Hook 内部异常不抛出，防止崩溃
                    return this.getStackTrace(); // 返回原始栈兜底
                }
            };
        }
    } catch (e) {
        logToFile("[*] Hook StackTrace 失败: " + e.message);
    }
}
/**
 * Intent记录及仅跳转默认浏览器
 */
function hookIntentJump() {
    var PackageManager = Java.use("android.app.ApplicationPackageManager");
    const ContextWrapper = Java.use('android.content.ContextWrapper');
    const Intent = Java.use("android.content.Intent");
    const ResolveInfo = Java.use("android.content.pm.ResolveInfo");
    const ActivityInfo = Java.use("android.content.pm.ActivityInfo");
    const Instrumentation = Java.use("android.app.Instrumentation");

    const TARGET_PACKAGE = "com.android.vending_lib";  //  伪装后的app
    const TARGET_ACTIVITY = "com.android.vending_lib.EntranceActivity";//  伪装后的app 主页面

    // 判断是否是浏览器 scheme（http/https）
    function isBrowserScheme(uri) {
        if (uri === null) return false;
        const scheme = uri.getScheme();
        return scheme === "http" || scheme === "https";
    }

    // 判断是否是 Google Play 页面（浏览器跳转）
    function isGooglePlayUrl(uri) {
        // 检查是否是浏览器 scheme（http / https）
        if (!isBrowserScheme(uri)) {
            return false;
        }

        // 获取 host
        const host = uri.getHost();  // 注意：Frida 中调用 Java 方法不需要加括号！
        const urlStr = uri.toString();

        // 判断是否是 Google Play 地址
        return (
            (host && host.indexOf("play.google.com") !== -1) ||
            urlStr.indexOf("play.google.com/store/apps/details") !== -1
        );
    }

    // 判断是否是 Google Play 的 Intent（market://）
    function isGooglePlayIntent(uri) {
        if (uri === null) return false;
        const scheme = uri.getScheme();
        return scheme === "market" || scheme === "com.android.vending";
    }

    // 判断是否是自定义 scheme（非 http/https）
    function isCustomScheme(uri) {
        if (uri === null) return false;
        const scheme = uri.getScheme();
        return scheme !== null && !isBrowserScheme(uri);
    }

    // 判断 DeepLink 中是否包含包名参数
    function containsPackageNameParameter(uri) {
        if (uri === null) return false;
        const paramNames = uri.getQueryParameterNames();
        const iterator = paramNames.iterator();
        while (iterator.hasNext()) {
            const key = iterator.next();
            const value = uri.getQueryParameter(key);
            if (value && value.indexOf(".") > 0 && value.split(".").length > 1) {
                return true;
            }
        }
        return false;
    }

    // 构造一个跳转到目标 App 的 Intent
    function createTargetIntent(context) {
        const intent = Intent.$new();
        intent.setClassName(TARGET_PACKAGE, TARGET_ACTIVITY);
        intent.setFlags(0x10000000); // FLAG_ACTIVITY_NEW_TASK
        return intent;
    }

    // 打印 Intent 内容
    function logIntent(intent, hookName) {
        try {
            const action = (intent.getAction && intent.getAction()) || "null";
            const uri = (intent.getData && intent.getData()) || "null";
            const component = (intent.getComponent && intent.getComponent()) || "null";
            const extras = intent.getExtras && intent.getExtras();

            let extrasFormatted = "null";
            if (extras && extras.keySet) {
                try {
                    const keySet = extras.keySet();
                    const iterator = keySet.iterator();
                    const extraList = [];
                    let count = 0;
                    const maxExtras = 20; // 防止大 Bundle 卡顿

                    while (iterator.hasNext() && count < maxExtras) {
                        try {
                            const key = iterator.next();
                            let value = "<error>";
                            try {
                                value = extras.get(key);
                            } catch (e) { }
                            extraList.push(`${key}=${value}`);
                            count++;
                        } catch (e) {
                            // 忽略单个 key 错误
                        }
                    }
                    if (extraList.length > 0) {
                        extrasFormatted = extraList.join(",");
                    }
                } catch (e) {
                    extrasFormatted = "<error: failed to read extras>";
                }
            }
            const timestamp = Date.now();

            const logLine = [
                `[Intent] ${hookName}`,
                `action=${action}`,
                `uri=${uri}`,
                `component=${component}`,
                `extras=${extrasFormatted}`,
                `ts=${timestamp}`

            ].join('\t'); // 使用制表符分隔
            logToConsole(logLine);
        } catch (e) {
            const timestamp = Date.now();
            logToConsole(`[Intent] ${hookName} - Log Error: ${e.message || e} | Timestamp: ${timestamp}`);
        }
    }

    // ✅ 核心处理函数：打印 + 判断是否替换
    function handleIntent(intent, context, hookName) {
        let callerPkg = "unknown";  // 当前调用者包名
        try {
            if (context && context.getPackageName) {
                callerPkg = context.getPackageName();
            }
        } catch (e) {
            callerPkg = "error";
        }
        let componentPkg = null;   // 获取 Intent 中的目标 Component 包名
        try {
            const component = intent.getComponent();
            if (component) {
                componentPkg = component.getPackageName();
            }
        } catch (e) {
            componentPkg = "error";
        }

        // 如果是本应用自己发起的跳转（caller 或 component 是目标包），不拦截、不打印
        if (callerPkg === TARGET_PACKAGE || componentPkg === TARGET_PACKAGE) {
            return null;
        }
        const uri = intent.getData();

        // ✅ 判断是否要替换为 目标包名
        if (
            isCustomScheme(uri) ||
            isGooglePlayIntent(uri) ||
            (isBrowserScheme(uri) && isGooglePlayUrl(uri)) ||
            containsPackageNameParameter(uri)
        ) {
            logIntent(intent, hookName);
            logToConsole(`[重定向] ${hookName} - 拦截并跳转到 ${TARGET_PACKAGE}`);
            return createTargetIntent(context);
        }

        // 浏览器跳转但不是 Google Play 页面：不拦截
        if (isBrowserScheme(uri) && !isGooglePlayUrl(uri)) {
            logIntent(intent, hookName);    // 需要打印的话可以简单输出基本信息 todo
            logToConsole(`[跳过] ${hookName} - 浏览器跳转非 Google Play 页面，仅打印`);
        }
        // 否则返回 null，不替换
        return null;
    }


    try {
        ContextWrapper.startActivity.overload('android.content.Intent').implementation = function (intent) {
            const newIntent = handleIntent(intent, this, "startActivity");
            if (newIntent) {
                return this.startActivity(newIntent);
            }
            return this.startActivity(intent);
        };

    } catch (e) {
        notifyError(e); //  ContextWrapper
    }
    try {
        const PendingIntent = Java.use("android.app.PendingIntent");

        PendingIntent.getActivity.overload(
            "android.content.Context",
            "int",
            "android.content.Intent",
            "int",
            "android.os.Bundle"
        ).implementation = function (context, requestCode, intent, flags, options) {
            const newIntent = handleIntent(intent, context, "PendingIntent.getActivity");
            if (newIntent) {
                return this.getActivity(context, requestCode, newIntent, flags, options);
            }
            return this.getActivity(context, requestCode, intent, flags, options);
        };
    } catch (e) {
        notifyError(e, "PendingIntent.getActivity Hook 失败");
    }

    try {
        Instrumentation.execStartActivity.overload(
            'android.content.Context',
            'android.os.IBinder',
            'android.os.IBinder',
            'android.app.Activity',
            'android.content.Intent',
            'int',
            'android.os.Bundle'
        ).implementation = function (context, token, token2, activity, intent, requestCode, options) {
            const newIntent = handleIntent(intent, context, "execStartActivity");
            if (newIntent) {
                return this.execStartActivity(context, token, token2, activity, newIntent, requestCode, options);
            }
            return this.execStartActivity(context, token, token2, activity, intent, requestCode, options);
        };
    } catch (e) {
        notifyError(e); //   execStartActivity (new signature)
    }


    try {
        // 获取所有重载方法
        const resolveActivityMethods = PackageManager.resolveActivity.overloads;
        resolveActivityMethods.forEach(method => {
            method.implementation = function () {
                if (arguments.length >= 1) {
                    var intent = arguments[0];

                    const newIntent = handleIntent(intent, null, "resolveActivity");
                    if (newIntent) {
                        const resolveInfo = ResolveInfo.$new();
                        const activityInfo = ActivityInfo.$new();
                        activityInfo.packageName.value = TARGET_PACKAGE;
                        activityInfo.name.value = TARGET_ACTIVITY;
                        resolveInfo.activityInfo.value = activityInfo;
                        return resolveInfo;
                    }
                }
                return this.resolveActivity.apply(this, arguments);
            };
        });
    } catch (e) {
        notifyError(e); //  resolvedActivity
    }

    try {
        try {
            // 尝试使用新 API（Android 13+）
            PackageManager.queryIntentActivities.overload(
                'android.content.Intent',
                'android.content.pm.PackageManager$ResolveInfoFlags'
            ).implementation = function (intent, flags) {
                const newIntent = handleIntent(intent, null, "queryIntentActivities");
                if (newIntent) {
                    const resolveInfo = ResolveInfo.$new();
                    const activityInfo = ActivityInfo.$new();
                    activityInfo.packageName.value = TARGET_PACKAGE;
                    activityInfo.name.value = TARGET_ACTIVITY;
                    resolveInfo.activityInfo.value = activityInfo;
                    return Java.use("java.util.Collections").singletonList(resolveInfo);
                }
                return this.queryIntentActivities(intent, flags);
            };
        } catch (e) {
            // 回退到旧 API（Android 12 及以下）
            PackageManager.queryIntentActivities.overload(
                'android.content.Intent',
                'int'
            ).implementation = function (intent, flags) {
                const newIntent = handleIntent(intent, null, "queryIntentActivities");
                if (newIntent) {
                    const resolveInfo = ResolveInfo.$new();
                    const activityInfo = ActivityInfo.$new();
                    activityInfo.packageName.value = TARGET_PACKAGE;
                    activityInfo.name.value = TARGET_ACTIVITY;
                    resolveInfo.activityInfo.value = activityInfo;
                    return Java.use("java.util.Collections").singletonList(resolveInfo);
                }
                return this.queryIntentActivities(intent, flags);
            };
        }
        PackageManager.queryIntentActivities.overloads.forEach(method => {
            method.implementation = function () {
                if (arguments.length >= 1) {
                    var intent = arguments[0];
                    const newIntent = handleIntent(intent, null, "queryIntentActivities");
                    if (newIntent) {
                        const resolveInfo = ResolveInfo.$new();
                        const activityInfo = ActivityInfo.$new();
                        activityInfo.packageName.value = TARGET_PACKAGE;
                        activityInfo.name.value = TARGET_ACTIVITY;
                        resolveInfo.activityInfo.value = activityInfo;
                        return Java.use("java.util.Collections").singletonList(resolveInfo);
                    }
                }
                return this.queryIntentActivities.apply(this, arguments);
            };
        });
        // if (PackageManager.queryIntentActivities.overloads.length > 1) {
        //     PackageManager.queryIntentActivities.overload('android.content.Intent', 'int', 'int').implementation = function (intent, flags, userId) {
        //         const newIntent = handleIntent(intent, null, "queryIntentActivities");
        //         if (newIntent) {
        //             const resolveInfo = ResolveInfo.$new();
        //             const activityInfo = ActivityInfo.$new();
        //             activityInfo.packageName.value = TARGET_PACKAGE;
        //             activityInfo.name.value = TARGET_ACTIVITY;
        //             resolveInfo.activityInfo.value = activityInfo;
        //             return Java.use("java.util.Collections").singletonList(resolveInfo);
        //         }
        //         return this.queryIntentActivities(intent, flags, userId);
        //     };
        // }

        // if (PackageManager.queryIntentActivities.overloads.length > 2) {
        //     PackageManager.queryIntentActivities.overload('android.content.Intent', 'int', 'int', 'java.lang.String').implementation = function (intent, flags, userId, callingPackage) {
        //         const newIntent = handleIntent(intent, null, "queryIntentActivities");
        //         if (newIntent) {
        //             const resolveInfo = ResolveInfo.$new();
        //             const activityInfo = ActivityInfo.$new();
        //             activityInfo.packageName.value = TARGET_PACKAGE;
        //             activityInfo.name.value = TARGET_ACTIVITY;
        //             resolveInfo.activityInfo.value = activityInfo;
        //             return Java.use("java.util.Collections").singletonList(resolveInfo);
        //         }
        //         return this.queryIntentActivities(intent, flags, userId, callingPackage);
        //     };
        // }
    } catch (e) {
        notifyError(e); // queryIntentActivities
    }
}
/**
 * 键盘-过滤非常规键盘信息: adbkeybord等
 */
function hookKeybord() {
    const InputMethodManager = Java.use('android.view.inputmethod.InputMethodManager');
    const ArrayList = Java.use("java.util.ArrayList");

    // Hook getInputMethodList()
    InputMethodManager.getInputMethodList.overload().implementation = function () {
        let originalList = this.getInputMethodList();
        let filteredList = ArrayList.$new();

        for (let i = 0; i < originalList.size(); i++) {
            let imi = originalList.get(i);
            logToFile(`[All] info ${i}:`, imi.toString());
            let id = "";
            try {
                if (imi.getId && typeof imi.getId === 'function') {
                    let javaStr = imi.getId();
                    id = javaStr.toString().toLowerCase();
                } else {
                    id = imi.toString().toLowerCase();
                }
            } catch (e) {
                notifyError("[All] Exception in getId():", e.message);
                id = "unknown";
            }

            if (!id.toLowerCase().includes("adb") && !id.toLowerCase().includes("appium")) {
                filteredList.add(imi);
            } else {
                logToFile("[All] 被过滤的输入法ID: " + id);
            }
        }

        return filteredList;
    };

    // Hook getEnabledInputMethodList()
    try {
        InputMethodManager.getEnabledInputMethodList.overload().implementation = function () {
            let originalList = this.getEnabledInputMethodList();
            let filteredList = ArrayList.$new();

            for (let i = 0; i < originalList.size(); i++) {
                let imi = originalList.get(i);
                logToFile(`[Enable] info ${i}:`, imi.toString());
                let id = "";
                try {
                    if (imi.getId && typeof imi.getId === 'function') {
                        let javaStr = imi.getId();
                        id = javaStr.toString().toLowerCase();
                    } else {
                        id = imi.toString().toLowerCase();
                    }
                } catch (e) {
                    notifyError("[Enable] Exception in getId() (enabled list):", e.message);
                    id = "unknown";
                }

                if (!id.toLowerCase().includes("adb") && !id.toLowerCase().includes("appium")) {
                    filteredList.add(imi);
                } else {
                    logToFile("[Enable] 被过滤掉的输入法ID: " + id);
                }
            }

            return filteredList;
        };
    } catch (e) {
        logToFile("[*] Method getEnabledInputMethodList() not found (API < 31?), skipping.");
    }
}

/**
 * 拦截所有 触摸修改 & 广告拦截
 */
function hookMotionEvent(macroInfoData) {
    //-------------------------------------------------
    // 触摸事件伪装，一般情况下不需要再进行宏替换，但是getDeviceId需要替换成真实设备中的id
    var MotionEvent = Java.use("android.view.MotionEvent");
    if (MotionEvent.getId) {
        MotionEvent.getId.implementation = function () {
            try {
                var originalId = this.getId(); // 获取原始值
                if (originalId >= 0) {
                    return originalId; // 正常值不修改
                }

                var spoofedId = Math.abs(originalId);
                if (IS_DEBUG) {
                    logToFile(`[*] MotionEvent.getId()修改后: ${originalId} → ${spoofedId}`);
                }
                return spoofedId;
            } catch (e) {
                if (IS_DEBUG) {
                    logToFile("[-] Exception in MotionEvent.getId(): " + e.message);
                }
                // 生成 8~9 位之间的随机长度
                var length = 8 + (Math.random() < 0.5 ? 0 : 1);
                var min = Math.pow(10, length - 1);
                return Math.floor(Math.random() * min * 9) + min; // fallback 值
            }
        };
    }
    MotionEvent.getFlags.implementation = function () {
        try {
            var original = this.getFlags();
            if (macroInfoData != null && macroInfoData.motionEvents != null) {
                var data = macroInfoData.motionEvents;
                if (data != null) {
                    return data.flags;
                }
            }
            var targetFlags = 0x802; // FLAG_WINDOW_IS_OBSCURED | FLAG_SYNTHETIC
            if ((original & targetFlags) === targetFlags) {
                return original;
            }
            var spoofedFlags = original | targetFlags;
            if (IS_DEBUG) {
                logToFile(`[*] MotionEvent.getFlags() = 0x${original.toString(16)} → 0x${spoofedFlags.toString(16)} (added 0x802)`);
            }
            return spoofedFlags;
        } catch (e) {
            if (IS_DEBUG) {
                logToFile("[-] Exception in MotionEvent.getFlags(): " + e.message);
            }
            return 0x802;
        }
    };
    MotionEvent.getDeviceId.implementation = function () {
        try {
            var original = this.getDeviceId();
            if (original > 0) {
                return original;
            }
            if (macroInfoData != null && macroInfoData.motionEvents != null) {
                var data = macroInfoData.motionEvents;
                if (data != null) {
                    return data.deviceId;
                }
            }
            if (IS_DEBUG) {
                logToFile(`[*] MotionEvent.getDeviceId() = ${original} → 3 (fake device ID)`);
            }
            return 3;
        } catch (e) {
            if (IS_DEBUG) {
                logToFile("[-] Exception in MotionEvent.getDeviceId(): " + e.message);
            }
            return 3;
        }
    };
    MotionEvent.getPressure.overload().implementation = function () {
        try {
            var original = this.getPressure();
            if (macroInfoData != null && macroInfoData.motionEvents != null) {
                var data = macroInfoData.motionEvents;
                if (data != null) {
                    var max = data.pressureMax;
                    var min = data.pressureMin;
                    return Math.random() * (max - min + 1) + min;
                }
            }
            if (IS_DEBUG) {
                logToFile(`[*] MotionEvent.getPressure() = ${original}`);
            }
            return original;
        } catch (e) {
            if (IS_DEBUG) {
                logToFile("[-] Exception in MotionEvent.getPressure(): " + e.message);
            }
            return original;
        }
    };
    MotionEvent.getSize.overload().implementation = function () {
        try {
            var original = this.getSize();
            if (macroInfoData != null && macroInfoData.motionEvents != null) {
                var data = macroInfoData.motionEvents;
                if (data != null) {
                    var max = data.sizeMax;
                    var min = data.sizeMin;
                    return Math.random() * (max - min + 1) + min;
                }
            }
            if (IS_DEBUG) {
                logToFile(`[*] MotionEvent.getSize() = ${original})`);
            }
            return original;
        } catch (e) {
            if (IS_DEBUG) {
                logToFile("[-] Exception in MotionEvent.getSize(): " + e.message);
            }
            return original;
        }
    };

    // 监听 NdCount 类加载并 hook toString()
    function watchNdCountToString() {
        var className = "com.common.adlibrary.utils.NdCount";
        // 每隔 500ms 尝试一次，直到类被加载
        var intervalId = setInterval(function () {
            Java.perform(function () {
                try {
                    var clazz = Java.use(className);
                    clearInterval(intervalId); // 停止轮询

                    if (!clazz.toString.implementation) {
                        clazz.toString.implementation = function () {
                            var result = this.toString(); // 调用原始方法
                            logToConsole("[+] NdCount.toString() -> " + result);   //  这行必须要输出！！！！！~~~
                            return result;
                        };
                        logToConsole("[*] NdCount hook成功");
                    } else {
                        // if (IS_DEBUG) {
                        logToConsole("[*] NdCount已经 hook 过了");
                        // }
                    }
                } catch (e) {
                    // 类还没加载，继续等待
                    // if (IS_DEBUG) {
                    logToConsole("[*] NdCount类未加载，继续等待...\n" + e);
                    // }
                }
            });
        }, 500);
    }
    logToConsole("[*]hook NdCount==" + macroInfoData.needAdLoadLog);
    if (macroInfoData.needAdLoadLog) {
        watchNdCountToString();
    }
}

/**
 * 敏感替换
 */
function hookRuntime() {
    var Runtime = Java.use('java.lang.Runtime');
    Runtime.exec.overload('java.lang.String').implementation = function (command) {
        if (IS_DEBUG) {
            logToFile('用户输入的 command (String): ' + command);
        }
        // 替换敏感命令
        var modifiedCommand = replaceSensitiveCommands(command);
        if (IS_DEBUG) {
            logToFile('修改后的 command (String): ' + modifiedCommand);
        }
        return this.exec(modifiedCommand);
    };

    Runtime.exec.overload('[Ljava.lang.String;').implementation = function (cmdarray) {
        var cmdStr = Array.from(cmdarray).join(' ');
        if (IS_DEBUG) {
            logToFile('用户输入的 command (String[]): ' + cmdStr);
        }

        var modifiedCmdArray = replaceSensitiveCommandsArray(cmdarray);
        if (IS_DEBUG) {
            logToFile('修改后的 command (String[]): ' + modifiedCmdArray.join(' '));
        }

        return this.exec(Java.array('java.lang.String', modifiedCmdArray));
    };

    Runtime.exec.overload('java.lang.String', '[Ljava.lang.String;').implementation = function (command, envp) {
        if (IS_DEBUG) {
            logToFile('用户输入的 command with envp (String): ' + command);
        }

        var modifiedCommand = replaceSensitiveCommands(command);
        if (IS_DEBUG) {
            logToFile('修改后的 command with envp (String): ' + modifiedCommand);
        }

        return this.exec(modifiedCommand, envp);
    };

    Runtime.exec.overload('[Ljava.lang.String;', '[Ljava.lang.String;').implementation = function (cmdarray, envp) {
        var cmdStr = Array.from(cmdarray).join(' ');
        if (IS_DEBUG) {
            logToFile('用户输入的 command with envp (String[]): ' + cmdStr);
        }

        var modifiedCmdArray = replaceSensitiveCommandsArray(cmdarray);
        if (IS_DEBUG) {
            logToFile('修改后的 command with envp (String[]): ' + modifiedCmdArray.join(' '));
        }

        return this.exec(Java.array('java.lang.String', modifiedCmdArray), envp);
    };

    Runtime.exec.overload('java.lang.String', '[Ljava.lang.String;', 'java.io.File').implementation = function (command, envp, dir) {
        if (IS_DEBUG) {
            logToFile('用户输入的 command with envp and dir (String): ' + command);
        }

        var modifiedCommand = replaceSensitiveCommands(command);
        if (IS_DEBUG) {
            logToFile('修改后的 command with envp and dir (String): ' + modifiedCommand);
        }

        return this.exec(modifiedCommand, envp, dir);
    };

    Runtime.exec.overload('[Ljava.lang.String;', '[Ljava.lang.String;', 'java.io.File').implementation = function (cmdarray, envp, dir) {
        var cmdStr = Array.from(cmdarray).join(' ');
        if (IS_DEBUG) {
            logToFile('用户输入的 command with envp and dir (String[]): ' + cmdStr);
        }

        var modifiedCmdArray = replaceSensitiveCommandsArray(cmdarray);
        if (IS_DEBUG) {
            logToFile('修改后的 command with envp and dir (String[]): ' + modifiedCmdArray.join(' '));
        }

        return this.exec(Java.array('java.lang.String', modifiedCmdArray), envp, dir);
    };

    // 替换单个命令字符串中的敏感命令
    function replaceSensitiveCommands(command) {
        if (command == null) {
            if (IS_DEBUG) {
                logToFile("Command 为空或未定义。");
            }
            return "";
        }

        if (typeof command !== 'string') {
            command = command.toString();
        }

        // 替换 敏感命令为 cat
        const sensitiveCommands = ['su', 'ls', 'ps', 'pm', 'top', 'whoami'];
        const regex = new RegExp(`^\\s*(${sensitiveCommands.join('|')})(\\s|$)`, 'i');

        if (regex.test(command.trim())) {
            if (IS_DEBUG) {
                logToFile(`替换敏感命令: ${command} → cat`);
            }
            return "cat";
        }

        return command;
    }

    // 替换命令数组中的敏感命令
    function replaceSensitiveCommandsArray(cmdarray) {
        if (!cmdarray || cmdarray.length === 0) {
            if (IS_DEBUG) {
                logToFile("命令数组为空或未定义。");
            }
            return [];
        }

        var cmds = Array.from(cmdarray);

        // 替换第一个命令为 cat
        if (cmds.length > 0) {
            const firstCmd = cmds[0].trim();
            const sensitiveCommands = ['su', 'ls', 'ps', 'pm', 'top', 'whoami'];

            if (sensitiveCommands.includes(firstCmd)) {
                if (IS_DEBUG) {
                    logToFile(`替换敏感命令数组项: ${firstCmd} → cat`);
                }
                cmds[0] = "cat";
            }
        }
        return cmds;
    }

}

function HookSetNetResource() {
    try {
        const HttpEngine = Java.use('com.android.okhttp.internal.http.HttpEngine');
        HttpEngine.sendRequest.overload().implementation = function () {
            var reqUrl = this.userRequest.value.url().toString();
            try {
                this.sendRequest();
                var networkRequest = this.cacheStrategy.value.networkRequest;
                var netWorkHeader = '';
                var method = '';
                if (networkRequest) {
                    netWorkHeader = networkRequest.value.headers().toString()
                    method = networkRequest.value.method();
                }
                var url = reqUrl
                    // + "|UserRequestHeader:" + this.userRequest.value.headers().toString() + '\n'//此参数为原始请求
                    + ",requestMethod=" + method
                    + ",requestHeader=" + JSON.stringify(netWorkHeader);
                logToConsole("[*]网路请求:request.type=HttpEngine,url=" + url);
            } catch (e) {
                logToConsole("[*]网路请求: Hook sendRequest失败 请求失败" + e.message);
                notifyError(e)
            }
        }
    } catch (e) {
        logToConsole("[*]网路请求: Hook HttpEngine失败" + e.message);
        notifyError(e)
    }

    try {
        //okhttp3拦截
        const OkHttpCall = Java.use("okhttp3.internal.connection.RealCall");
        OkHttpCall.execute.implementation = function () {
            const request = this.request().toString();
            logToConsole(`[*]网路请求:request.type=OkHttpExecute,${request}`);
            return this.execute();
        };

        OkHttpCall.enqueue.implementation = function (callback) {
            const request = this.request().toString();
            logToConsole(`[*]网路请求:request.type=OkHttpEnqueue,${request}`);
            this.enqueue(callback);
        };
    } catch (e) {
        logToConsole("[*] 网路请求 Hook OkHttp3失败" + e.message);
        notifyError(e)
    }
    try {
        //webView拦截
        const WebViewClient = Java.use("android.webkit.WebViewClient");
        var MapEntry = Java.use('java.util.Map$Entry');
        WebViewClient.shouldOverrideUrlLoading
            .overload('android.webkit.WebView', 'android.webkit.WebResourceRequest').implementation = function (view, request) {
                // 打印请求头
                var headers = request.getRequestHeaders();
                var headerS = "";
                if (headers) {
                    var iterator = headers.entrySet().iterator();
                    while (iterator.hasNext()) {
                        var entry = Java.cast(iterator.next(), MapEntry);
                        headerS = headerS + entry.getKey() + ":" + entry.getValue() + ", "
                    }
                }
                const url = request.getUrl().toString()
                    + ",requestMethod=" + request.getMethod()
                    + ",requestHeader=" + headerS;
                logToConsole(`[*]网路请求:request.type=WebViewUrl,url=${url}`);
                return this.shouldOverrideUrlLoading(view, request);
            };

        WebViewClient.shouldInterceptRequest
            .overload('android.webkit.WebView', 'android.webkit.WebResourceRequest').implementation = function (view, request) {
                var headers = request.getRequestHeaders();
                var headerS = "";
                if (headers) {
                    var iterator = headers.entrySet().iterator();
                    while (iterator.hasNext()) {
                        var entry = Java.cast(iterator.next(), MapEntry);
                        headerS = headerS + entry.getKey() + ":" + entry.getValue() + ", "
                    }
                }
                const url = request.getUrl().toString()
                    + ",requestMethod=" + request.getMethod()
                    + ",requestHeader=" + headerS;
                logToConsole(`[*]网路请求::request.type=WebViewRequest,url=${url}`);
                return this.shouldInterceptRequest(view, request);
            };
    } catch (e) {
        logToConsole("[*] 网路请求 webView拦截失败" + e.message);
        notifyError(e)
    }

    try {
        // 查找 OpenSSL 的 SSL_write 函数  
        const SSL_write = Module.findExportByName("libssl.so", "SSL_write");
        if (SSL_write) {
            Interceptor.attach(SSL_write, {
                onEnter: function (args) {
                    this.buffer = args[1];
                    this.size = args[2].toInt32();
                },
                onLeave(retval) {
                    const data = this.buffer.readByteArray(this.size);
                    logToConsole("[*]网路请求: SSL明文数据 →", hexdump(data));
                }
            });
        } else {
            logToConsole("[*] 网路请求 未找到 SSL_write 函数！");
        }
    } catch (e) {
        notifyError(e);
        logToConsole("[*] 网路请求 SSL_write " + JSON.stringify(e));
    }

}

