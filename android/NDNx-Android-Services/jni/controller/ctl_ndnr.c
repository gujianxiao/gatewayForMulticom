/**
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2012 Palo Alto Research Center, Inc.
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
#include <jni.h>
#include <stdlib.h>
#include <stdio.h>

// The Android log header
#include <android/log.h>

#include "ndnr_private.h"

#ifndef _Included_org_ndnx_android_services_repo_RepoService
#define _Included_org_ndnx_android_services_repo_RepoService
#ifdef __cplusplus
extern "C" {
#endif

extern int start_ndnr();

static struct ndnr_handle *h = NULL;

static int
androidlogger(void *loggerdata, const char *format, va_list ap)
{
	int len = 0;
	len = __android_log_vprint(ANDROID_LOG_INFO, "NDNR", format, ap);
	return len;
}

/*
 * Class:     org_ndnx_android_services_repo_RepoService
 * Method:    ndnrCreate
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_org_ndnx_android_services_repo_RepoService_ndnrCreate
  (JNIEnv * env, jobject this, jstring version) {
    h = r_init_create("ndnr", androidlogger, NULL);
    if (h == NULL) {
        __android_log_print(ANDROID_LOG_ERROR,"NDNR", "ndnrCreate - r_init_create returned NULL");
        return -1;
    }
 	return 0;
  }

/*
 * Class:     org_ndnx_android_services_repo_RepoService
 * Method:    ndnrRun
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_ndnx_android_services_repo_RepoService_ndnrRun
  (JNIEnv * env, jobject this) {
	__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrRun - calling r_dispatch_run(%p)", h);
	r_dispatch_run(h);
	__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrRun - r_dispatch_run exited");
  }

/*
 * Class:     org_ndnx_android_services_repo_RepoService
 * Method:    ndnrDestroy
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_ndnx_android_services_repo_RepoService_ndnrDestroy
  (JNIEnv * env, jobject this) {
	__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrDestroy - ndnr stopping");
	r_init_destroy(&h);
  }

/*
 * Class:     org_ndnx_android_services_repo_RepoService
 * Method:    ndnrKill
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_org_ndnx_android_services_repo_RepoService_ndnrKill
  (JNIEnv * env, jobject this) {
	if( h != NULL ) {
		__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrKill set kill flag (%p)", h);
		h->running = 0;
	} else {
		__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrKill null handle");
	}
	return 0;
  }

/*
 * Class:     org_ndnx_android_services_repo_RepoService
 * Method:    ndnrSetenv
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_org_ndnx_android_services_repo_RepoService_ndnrSetenv
  (JNIEnv * env, jobject this, jstring jkey, jstring jvalue, jint joverwrite) {
	const char *key = (*env)->GetStringUTFChars(env, jkey, NULL);
	const char *value = (*env)->GetStringUTFChars(env, jvalue, NULL);

	__android_log_print(ANDROID_LOG_INFO,"NDNR", "ndnrSetenv %s = %s", key, value);

	setenv(key, value, joverwrite);

	(*env)->ReleaseStringUTFChars(env, jkey, key);
	(*env)->ReleaseStringUTFChars(env, jvalue, value);
  }

#ifdef __cplusplus
}
#endif
#endif
