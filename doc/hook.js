// multi_hook.js
function main() {
    Java.perform(function () {
        var processName = Java.use("android.app.ActivityThread").currentProcessName().toString();
        var pid = Process.id;

        console.log("");
        console.log(`🔥 [Frida 注入] PID: ${pid} | 进程: ${processName}`);
        console.log("=".repeat(50));

        // 根据进程名执行不同逻辑
        if (processName === "com.mergegames.gossipharbor") {
            console.log("📱 主进程: 可 Hook 登录、UI 逻辑");
        } else if (processName === "com.mergegames.gossipharbor:Game") {
            console.log("🎮 游戏进程: 可 Hook SDK、Unity 逻辑");
        }

        // 示例：Hook Log.e
        try {
            var Log = Java.use("android.util.Log");
            Log.e.overload('java.lang.String', 'java.lang.String').implementation = function (tag, msg) {
                console.log(`[LOG.e] ${processName} | ${tag} | ${msg}`);
                return this.e(tag, msg);
            };
        } catch (e) {
            console.log("[⚠️] Hook Log.e 失败:", e.message);
        }
    });
}

// 安全执行
if (Java.available) {
    setImmediate(main);
} else {
    console.log("❌ Java 环境不可用");
}