LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := networkutils
LOCAL_SRC_FILES := network_utils_jni.c network_utils.c

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)