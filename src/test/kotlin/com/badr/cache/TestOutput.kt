package com.badr.cache

import com.badr.cache.extensions.await
import com.badr.cache.storage.Storage
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test

class TestOutput {

    @Test
    fun output() {
        val storage = Storage.newInstance()

        storage.add("test1", "value1", 5, TimeUnit.SECONDS)
        storage.add("test2", "value2", 5, TimeUnit.SECONDS)
        storage.add("test3", "value3", 5, TimeUnit.SECONDS)

        var successfulCount = 0
        var failureCount = 0

        (0..5_000_000).toSet().parallelStream().forEach {
            storage.get<String>("test${(1..3).random()}").onComplete {
                if (it.succeeded()) {
                    successfulCount++
                } else {
                    failureCount++
                }
            }
        }

        while ((storage.getKeysCount().await() ?: 0) > 0) {
            Thread.sleep(50)
        }
        println("5M iteration: successful read [$successfulCount], failure read: [$failureCount]")
    }
}