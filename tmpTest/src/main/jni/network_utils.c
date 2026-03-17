#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>
#include <ifaddrs.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <unistd.h>

// 获取所有非环回、活动状态的 IPv4 地址
char* getLocalIPv4Addresses() {
    struct ifaddrs *ifAddrStruct = NULL;
    void *tmpAddrPtr = NULL;

    // 用于拼接结果字符串
    char *result = (char *)calloc(1, 1024); // 初始分配 1KB，可根据设备数量调整
    if (!result) return NULL;

    getifaddrs(&ifAddrStruct);
    for (struct ifaddrs *ifa = ifAddrStruct; ifa != NULL; ifa = ifa->ifa_next) {
        if (!ifa->ifa_addr) continue;
        if (ifa->ifa_addr->sa_family == AF_INET) { // IPv4 地址
            tmpAddrPtr = &((struct sockaddr_in *)ifa->ifa_addr)->sin_addr;
            char addressBuffer[INET_ADDRSTRLEN];
            inet_ntop(AF_INET, tmpAddrPtr, addressBuffer, INET_ADDRSTRLEN);

            if (strcmp(addressBuffer, "127.0.0.1") != 0) { // 忽略 loopback
                char line[256];
                snprintf(line, sizeof(line), "%s: %s\n", ifa->ifa_name, addressBuffer);
                strcat(result, line);
            }
        }
    }

    if (ifAddrStruct != NULL) freeifaddrs(ifAddrStruct);

    return result;
}

// 使用 ioctl 获取指定网络接口的 IPv4 地址
char* getIPAddressByIoctl(const char* interfaceName) {
    int sockfd;
    struct ifreq ifr;
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    ifr.ifr_addr.sa_family = AF_INET;
    strncpy(ifr.ifr_name , interfaceName , IFNAMSIZ-1);

    if(ioctl(sockfd, SIOCGIFADDR, &ifr) >= 0) {
        char *ip = inet_ntoa(((struct sockaddr_in *)&ifr.ifr_addr )->sin_addr);
        close(sockfd);
        return strdup(ip); // 返回分配的字符串，需要手动释放
    } else {
        close(sockfd);
        return NULL;
    }
}