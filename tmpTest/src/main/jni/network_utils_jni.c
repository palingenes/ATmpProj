#include <jni.h>
#include <string.h>
#include "network_utils.c"

JNIEXPORT void JNICALL
Java_com_cymf_tmp_utils_GetIpUtil_getLocalIPv4Addresses(JNIEnv *env, jobject /* this */) {
char* ips = getLocalIPv4Addresses();
jstring result = (*env)->NewStringUTF(env, ips ? ips : "No valid IPv4 addresses found.");
free(ips); // 注意：必须手动释放内存
return result;
}

JNIEXPORT jstring JNICALL
Java_com_cymf_tmp_utils_GetIpUtil_getIPAddressByIoctl(JNIEnv *env, jobject /* this */, jstring interfaceName_) {
    const char *interfaceName = (*env)->GetStringUTFChars(env, interfaceName_, 0);
    char* ip = getIPAddressByIoctl(interfaceName);
    (*env)->ReleaseStringUTFChars(env, interfaceName_, interfaceName);

    return (*env)->NewStringUTF(env, ip ? ip : "Failed to get IP");
}