package com.badr.cache.extensions

import com.badr.cache.core.Future
import java.util.concurrent.CountDownLatch

fun <T> Future<T>.await(): T? {
    var result: T? = null
    val latch = CountDownLatch(1)
    this.onComplete {
        if (it.succeeded()) {
            result = it.result()
        }
        latch.countDown()
    }
    latch.await()
    return result
}