/*
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009,2010 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * JNI wrapper functions for the ndnd process.  This uses android_main.c to
 * procedurally startup ndnd.
 *
 * The startup process is:
 *    1) call setenv to set any environment variables for NDND, as per
 *       normal NDND startup (e.g. working directory, capacity, debug level, etc.)
 *
 *    2) call ndndCreate
 *
 *    --> at this time, ndnd is ready to service requests
 *
 *    3) call ndndRun
 *
 *    --> caller is now blocked until ndnd exits
 *
 * To exit NDND, call kill.  This sets the "running" member of the ndnd handle
 * to zero, so ndnd will exit on its next main loop.  You should cleanup the
 * ndnd handle by calling ndndDestroy on it.
 *
 * The JNI methods are in the package org.ndnx.android.services.ndnd.  There are
 * also versions in org.ndnx.android.test.services.ndnd for JUnit testing
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>

// The Android log header
#include <android/log.h>

#include "ndnd_private.h"

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndCreate
	(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndRun
	(JNIEnv *env, jobject thiz);
JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndDestroy
	(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_setenv
	(JNIEnv *env, jobject thiz, jstring jkey, jstring jvalue, jint joverwrite);

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_kill
  (JNIEnv *env, jobject thiz);

extern int start_ndnd();

static struct ndnd_handle *h = NULL;

static int
androidlogger(void *loggerdata, const char *format, va_list ap)
{
	int len = 0;
	len = __android_log_vprint(ANDROID_LOG_INFO, "NDND", format, ap);
    return len;
}

JNIEXPORT void JNICALL Java_org_ndnx_android_test_ndnd_NdndThread_launch
  (JNIEnv *env, jobject thiz)
{
	Java_org_ndnx_android_services_ndnd_NdndService_ndndCreate(env, thiz);
	Java_org_ndnx_android_services_ndnd_NdndService_ndndRun(env, thiz);
	Java_org_ndnx_android_services_ndnd_NdndService_ndndDestroy(env, thiz);
    __android_log_print(ANDROID_LOG_INFO,"NDND", "ndnd launch exiting");
}

JNIEXPORT void JNICALL Java_org_ndnx_android_test_ndnd_NdndThread_setenv
  (JNIEnv *env, jobject thiz, jstring jkey, jstring jvalue, jint joverwrite) 
{
	Java_org_ndnx_android_services_ndnd_NdndService_setenv(env, thiz, jkey, jvalue, joverwrite);
}

JNIEXPORT void JNICALL Java_org_ndnx_android_test_ndnd_NdndThread_kill
  (JNIEnv *env, jobject thiz)
{
	Java_org_ndnx_android_services_ndnd_NdndService_kill(env, thiz);
}


JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndCreate
  (JNIEnv *env, jobject thiz)
{
    h = ndnd_create("ndnd", androidlogger, NULL);
    if (h == NULL) {
		__android_log_print(ANDROID_LOG_ERROR,"NDND", "ndnd_create returned NULL");
        return;
    }
}

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndRun
  (JNIEnv *env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_INFO,"NDND", "calling ndnd_run (%p)", h);

    ndnd_run(h);

    __android_log_print(ANDROID_LOG_INFO,"NDND", "ndnd_run exited");
}

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_ndndDestroy
  (JNIEnv *env, jobject thiz)
{
    __android_log_print(ANDROID_LOG_INFO,"NDND", "ndnd stopping");
    ndnd_destroy(&h);
}

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_setenv
  (JNIEnv *env, jobject thiz, jstring jkey, jstring jvalue, jint joverwrite)
{
	const char *key = (*env)->GetStringUTFChars(env, jkey, NULL);
	const char *value = (*env)->GetStringUTFChars(env, jvalue, NULL);

    __android_log_print(ANDROID_LOG_INFO,"NDND", "NdndService_setenv %s = %s", key, value);

	setenv(key, value, joverwrite);

	(*env)->ReleaseStringUTFChars(env, jkey, key);
	(*env)->ReleaseStringUTFChars(env, jvalue, value);

	return;
}

JNIEXPORT void JNICALL Java_org_ndnx_android_services_ndnd_NdndService_kill
  (JNIEnv *env, jobject thiz)
{
    if( h != NULL ) {
		__android_log_print(ANDROID_LOG_INFO,"NDND", "NdndService_kill set kill flag (%p)", h);
    	h->running = 0;
	} else {
		__android_log_print(ANDROID_LOG_INFO,"NDND", "NdndService_kill null handle");
	}

	return;
}

