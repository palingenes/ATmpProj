# auto_hook.py
# Frida 多进程注入控制器 (Python 版)
# 无需 Node.js！

import frida
import sys
import time
import logging

# ==================== 配置区 ====================
DEVICE_SERIAL = '98.98.125.9:20891'          # 修改为你的设备
PACKAGE_NAME = 'com.mergegames.gossipharbor'  # 目标包名
HOOK_SCRIPT_PATH = './hook.js'               # 你的 Frida JS 脚本
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
        # 读取 hook.js
        with open(HOOK_SCRIPT_PATH, 'r', encoding='utf-8') as f:
            source = f.read()
        script = session.create_script(source)
        script.on('message', on_message)
        script.load()
        log(f"🟢 已注入脚本到子进程 PID={child.pid}")
        # 恢复子进程
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

        # 启动 App
        log(f"[*] 即将启动 App: {PACKAGE_NAME}")
        pid = device.spawn([PACKAGE_NAME])
        log(f"[*] spawn() 返回 PID: {pid}")

        # 附加主进程
        session = device.attach(pid)
        log(f"[*] 已附加到主进程 PID={pid}")

        # ⭐ 开启 Child Gating
        session.enable_child_gating()
        log("[*] Child Gating 已启用")

        # 注入主进程
        with open(HOOK_SCRIPT_PATH, 'r', encoding='utf-8') as f:
            source = f.read()
        script = session.create_script(source)
        script.on('message', on_message)
        script.load()
        log("🟢 已注入脚本到主进程")

        # 恢复主进程
        device.resume(pid)
        log(f"▶️  主进程 PID={pid} 已恢复，App 启动")

        log("🎉 等待子进程... 按 Ctrl+C 退出")
        sys.stdin.read()  # 保持运行

    except KeyboardInterrupt:
        log("👋 退出中...")
    except Exception as e:
        log(f"❌ 错误: {e}")
        sys.exit(1)

if __name__ == '__main__':
    main()