#!/system/bin/sh

# 第二个守护进程

while true; do
    PID=$(pgrep -f watchdog1)
    if [ -z "$PID" ]; then
        sh /data/local/tmp/watchdog1.sh &
    fi
    sleep 10
done