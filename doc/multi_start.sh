#!/usr/bin/env bash
#
# auto_multi_hook.sh
# 指定设备的 Frida 多进程自动注入脚本 
#

# ==================== 配置区 ====================
DEVICE_SERIAL="98.98.125.9:20732"
PACKAGE_MAIN="com.mergegames.gossipharbor"
PACKAGE_GAME="com.mergegames.gossipharbor:Game"
FRIDA_SCRIPT="tmp.js"
LOG_MAIN="main.log"
LOG_GAME="game.log"
FRIDA_HOST="127.0.0.1:28202"

# 强制使用指定设备
ADB="adb -s $DEVICE_SERIAL"
# =================================================

# -------------------- 函数：等待并注入进程 --------------------
wait_and_hook() {
    local target_proc="$1"
    local log_file="$2"
    local proc_label="$3"

    echo "⏳ 等待 $proc_label 启动: $target_proc ..."

    local pid=""
    while [ -z "$pid" ]; do
        pid=$($ADB shell ps | grep "$target_proc" | awk '{print $2}' | tr -d '\r' | head -n1)
        if [ -z "$pid" ]; then
            sleep 1
        else
            echo "✅ $proc_label 已启动 | PID: $pid"
            echo "🔥 正在注入 Frida ... (日志: $log_file)"
            frida -H "$FRIDA_HOST" -p "$pid" -l "$FRIDA_SCRIPT" >> "$log_file" 2>&1 &
        fi
    done
}

# -------------------- 主流程 --------------------

# 检查依赖
command -v adb >/dev/null 2>&1 || { echo "❌ 错误: adb 未安装"; exit 1; }
command -v frida >/dev/null 2>&1 || { echo "❌ 错误: frida 命令未安装"; exit 1; }

# 检查设备是否在线
echo "🔌 正在检查设备连接状态: $DEVICE_SERIAL"
if ! $ADB get-state >/dev/null 2>&1; then
    echo "❌ 错误: 无法连接到设备 $DEVICE_SERIAL"
    echo "请检查："
    echo "  1. 设备是否在线"
    echo "  2. 是否已执行: adb connect 98.98.125.9:20732"
    exit 1
fi

# 检查 Frida 服务器连接
if ! frida-ps -H "$FRIDA_HOST" >/dev/null 2>&1; then
    echo "❌ 错误: 无法连接到 Frida 服务器 $FRIDA_HOST"
    echo "请确保: "
    echo "  adb forward tcp:28202 tcp:28202"
    echo "  手机上已运行 frida-server"
    exit 1
fi

# 清理日志
> "$LOG_MAIN"
> "$LOG_GAME"

echo "✅ 环境检查通过，目标设备: $DEVICE_SERIAL"

echo "🚀 正在自动启动应用: $PACKAGE_MAIN"
$ADB shell monkey -p $PACKAGE_MAIN -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ 应用启动命令已发送"
else
    echo "⚠️  警告: 启动应用可能失败（可能未安装或无权限）"
fi

echo "📌 脚本将自动等待进程并注入..."

# 等待主进程
wait_and_hook "$PACKAGE_MAIN" "$LOG_MAIN" "主进程"

# 等待子进程
sleep 1
wait_and_hook "$PACKAGE_GAME" "$LOG_GAME" "游戏进程 (:Game)"

# 完成提示
echo ""
echo "🎉 Frida 已注入到双进程！"
echo "📊 日志文件:"
echo "   📄 主进程: $LOG_MAIN"
echo "   📄 游戏进程: $LOG_GAME"
echo ""
echo "🔍 实时查看日志:"
echo "   tail -f $LOG_MAIN"
echo "   tail -f $LOG_GAME"
echo "========================================"