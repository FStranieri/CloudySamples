package com.fs.cloudapp

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> com.huawei.hmf.tasks.Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(
                task.exception ?: RuntimeException("Unknown HMS task exception")
            )
        }
    }
}

suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(
                task.exception ?: RuntimeException("Unknown GMS task exception")
            )
        }
    }
}

val Any.TAG: String
    get() {
        return javaClass.simpleName
    }