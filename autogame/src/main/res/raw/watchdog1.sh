#!/system/bin/sh

# 第一个守护进程

#adb push watchdog1.sh /data/local/tmp/watchdog1
#adb push watchdog2.sh /data/local/tmp/watchdog2
#adb shell chmod +x /data/local/tmp/watchdog1
#adb shell chmod +x /data/local/tmp/watchdog2

while true; do
    PID=$(pgrep -f keepalive_service)
    if [ -z "$PID" ]; then
        am start-service -n com.cymf.autogame/.guard.KeepAliveService
    fi
    sleep 10
done