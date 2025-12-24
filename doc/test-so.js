
Java.perform(() => {
    // 延迟确保 libc 加载完成
    setTimeout(() => {
        hookFunction("fopen", (args) => {
            const path = args[0].readUtf8String() || "";
            const mode = args[1].readUtf8String() || "";
            logToFile(`fopen("${path}", "${mode}")`);
        });

        hookFunction("open", (args) => {
            const path = args[0].readUtf8String() || "";
            const flags = args[1].toInt32();
            logToFile(`open("${path}", 0x${flags.toString(16)})`);
        });

        // ... 其他函数
    }, 1000);
});


/**
 * 辅助函数来查找和hook native 方法
 * @param {string} name 被hook的函数名，如fopen
 * @param {callback} onEnterCb 方法执行前，用于拦截传入${name}的参数
 * @param {callback} onLeaveCb 方法执行后，用户获取该方法返回值（如果有）
 * @returns 
 */
function hookFunction(name, onEnterCb, onLeaveCb) {
    try {
        const address = findFunctionAddress(name);
        if (address === null) {
            console.log(`[hookFunction] 跳过 Hook（未找到地址）: ${name}`);
            return;
        }

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

        console.log(`[hookFunction] 成功 Hook 函数: ${name} @ ${address}`);
    } catch (e) {
        console.error(`[hookFunction] Hook ${name} 失败:`, e);
    }
}


/**
 * 修复版：兼容 Frida 17.4.1 + Android 10+
 * @param {string} name - 函数名（如 "fopen"）
 * @returns {NativePointer | null}
 */
function findFunctionAddress(name) {
    // 获取 libc 模块（自动适配 Android 10+ APEX 路径）
    let libcModule = null;
    try {
        libcModule = Process.getModuleByName("libc.so");
    } catch (e) {
        // 如果 getModuleByName 失败，手动查找
        const modules = Process.enumerateModules();
        for (const m of modules) {
            if (m.name.endsWith("/libc.so") || m.name === "libc.so") {
                libcModule = m;
                break;
            }
        }
    }
    if (!libcModule) {
        console.log(`[findFunctionAddress] 未找到 libc.so`);
        return null;
    }
    // 同步枚举所有符号（包括内部符号）
    try {
        const symbols = Module.enumerateSymbolsSync(libcModule.name);
        for (const sym of symbols) {
            // 匹配精确名称 或 带版本后缀（如 fopen@@LIBC_...)
            if (sym.name === name || sym.name.startsWith(name + "@")) {
                console.log(`[findFunctionAddress] 找到 ${name} @ ${sym.address} in ${libcModule.name}`);
                return sym.address;
            }
        }
    } catch (e) {
        console.error(`[findFunctionAddress] 枚举符号失败:`, e);
        return null;
    }
    console.log(`[findFunctionAddress] 未在 ${libcModule.name} 中找到函数: ${name}`);
    return null;
}