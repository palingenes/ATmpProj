import frida
import sys
import logging

# ==================== 配置区 ====================
DEVICE_SERIAL = '98.98.125.9:20891'          # 你的设备 IP:端口
PACKAGE_NAME = 'com.mergegames.gossipharbor'  # 目标包名
HOOK_SCRIPT_PATH = './hook.js'               # 你的 JS 脚本
LOG_FILE = 'frida_multi.log'
# =================================================

# 设置日志
logging.basicConfig(
    level=logging.INFO,
    format='[%(asctime)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S',
    handlers=[
        logging.FileHandler(LOG_FILE, encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)

def log(msg):
    logging.info(msg)

def on_message(message, data):
    if message['type'] == 'send':
        log(f"[Hook] {message['payload']}")
    else:
        log(f"[Frida] {message}")

def on_child_added(child):
    log(f"[*] 检测到新子进程: PID={child.pid}, 参数='{child.parameters}'")
    try:
        session = device.attach(child.pid)
        with open(HOOK_SCRIPT_PATH, 'r', encoding='utf-8') as f:
            source = f.read()
        script = session.create_script(source)
        script.on('message', on_message)
        script.load()
        log(f"🟢 已注入脚本到子进程 PID={child.pid}")
        device.resume(child.pid)
        log(f"▶️  已恢复子进程 PID={child.pid}")
    except Exception as e:
        log(f"❌ 注入失败: {e}")

def on_child_removed(child):
    log(f"[*] 子进程退出: PID={child.pid}")

def on_output(pid, fd, data):
    if data:
        output = data.decode('utf-8', errors='replace').strip()
        if output:
            log(f"[输出][PID={pid}][FD={fd}]: {output}")

def main():
    global device

    try:
        log(f"🔍 正在连接设备: {DEVICE_SERIAL}")
        device = frida.get_device(DEVICE_SERIAL, timeout=5)
        log(f"✅ 成功连接设备: {device.name}")

        # 监听事件
        device.on('child-added', on_child_added)
        device.on('child-removed', on_child_removed)
        device.on('output', on_output)

        # ===== 第一步：等待用户手动启动 App =====
        log(f"📱 请现在手动启动 App: {PACKAGE_NAME}")
        log("⏳ 正在监听 pending 子进程...")

        while True:
            # 枚举所有 pending 子进程
            pending_children = device.enumerate_pending_children()
            for child in pending_children:
                if PACKAGE_NAME in child.parameters:
                    log(f"🎯 发现目标 pending 进程: PID={child.pid}, 参数='{child.parameters}'")

                    # 附加主进程
                    session = device.attach(child.pid)
                    log(f"[*] 已附加到主进程 PID={child.pid}")

                    # ⭐ 开启 Child Gating
                    session.enable_child_gating()
                    log("[*] Child Gating 已启用")

                    # 注入脚本
                    with open(HOOK_SCRIPT_PATH, 'r', encoding='utf-8') as f:
                        source = f.read()
                    script = session.create_script(source)
                    script.on('message', on_message)
                    script.load()
                    log("🟢 已注入脚本到主进程")

                    # 最后才 resume，App 真正开始运行
                    device.resume(child.pid)
                    log(f"▶️  主进程 PID={child.pid} 已恢复，App 正常运行！")
                    log("🎉 成功接管！后续子进程将自动注入。")

                    # 退出循环，保持连接
                    sys.stdin.read()
                    return

            # 每 1 秒检查一次
            time.sleep(1)

    except KeyboardInterrupt:
        log("👋 退出中...")
    except Exception as e:
        log(f"❌ 错误: {e}")
        sys.exit(1)

if __name__ == '__main__':
    import time  # 忘记导入了，补上
    main()