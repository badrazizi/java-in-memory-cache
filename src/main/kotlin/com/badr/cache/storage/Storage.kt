package com.badr.cache.storage

import com.badr.cache.core.Future
import com.badr.cache.core.Promise
import com.badr.cache.extensions.safe
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

class Storage {
    private val evictionPeriodic = Executors.newScheduledThreadPool(1)
    private val ioThread = Executors.newFixedThreadPool(1)

    private val storage: HashMap<String, Data<Any>> = hashMapOf()

    private val evictionRunnable: Runnable = Runnable {
        safe {
            ioThread.submit {
                evict()
            }
        }
    }

    init {
        evictionPeriodic.scheduleAtFixedRate(evictionRunnable, 0, 1, TimeUnit.SECONDS)
    }

    fun isTerminated(): Boolean = ioThread.isTerminated && evictionPeriodic.isTerminated

    fun isRunning(): Boolean = !ioThread.isTerminated && !evictionPeriodic.isTerminated

    fun shutDown() {
        evictionPeriodic.shutdown()
        ioThread.shutdown()
    }

    @JvmOverloads
    @Throws(IllegalStateException::class)
    fun shutDownAwait(timeout: Long = 30, unit: TimeUnit = TimeUnit.SECONDS) {
        check(timeout > 0) { "timeout should be larger than 0" }

        evictionPeriodic.shutdown()
        ioThread.shutdown()

        evictionPeriodic.awaitTermination(timeout, unit)
        ioThread.awaitTermination(timeout, unit)
    }

    fun shutDownNow(): List<Runnable> {
        val l1 = evictionPeriodic.shutdownNow()
        val l2 = ioThread.shutdownNow()

        return l1 + l2
    }

    @JvmOverloads
    fun <T : Any> add(key: String, value: T, lifeTime: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<Boolean> {
        val promise = Promise.promise<Boolean>()

        try {
            ioThread.submit {
                storage[key] = Data<Any>()
                    .setKey(key)
                    .setValue(value)
                    .updateAddedTime()
                    .setTTL(lifeTime)
                    .setTTLUnit(unit)

                promise.complete(true)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): Future<T> {
        val promise = Promise.promise<T>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.fail("Cache storage is empty")
                    return@submit
                }

                if (storage.containsKey(key)) {
                    val data = storage[key]
                    try {
                        if (data != null) {
                            val cast = data.value as T
                            data.updateAddedTime()
                            promise.complete(cast)
                        } else {
                            promise.fail("Could not find any value for $key")
                        }
                    } catch (e: Exception) {
                        promise.fail(e)
                    }
                } else {
                    promise.fail("Could not find any value for $key")
                }
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> get(predicate: (T) -> Boolean): Future<T> {
        val promise = Promise.promise<T>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.fail("Cache storage is empty")
                    return@submit
                }

                val iterator = storage.iterator()
                while (iterator.hasNext()) {
                    val data = iterator.next().value
                    safe {
                        val cast = data.value as T
                        if (predicate(cast)) {
                            data.updateAddedTime()
                            promise.complete(cast)
                            return@safe
                        }
                    }
                }

                promise.fail("Could not find any value for giving predicate")
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun get(vararg keys: String): Future<HashMap<String, Any>> {
        val promise = Promise.promise<HashMap<String, Any>>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.fail("Cache storage is empty")
                    return@submit
                }

                val hashMap = hashMapOf<String, Any>()
                for (key in keys) {
                    val data = storage[key]
                    if (data != null) {
                        data.updateAddedTime()
                        hashMap[data.key] = data.value
                    }
                }

                promise.complete(hashMap)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun getKeys(vararg regexes: Regex): Future<List<String>> {
        val promise = Promise.promise<List<String>>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.complete(emptyList())
                    return@submit
                }

                val keys = mutableListOf<String>()
                val iterator = storage.iterator()
                while (iterator.hasNext()) {
                    val data = iterator.next()
                    for (regex in regexes) {
                        if (regex.containsMatchIn(data.key)) keys.add(data.key)
                    }
                }

                promise.complete(keys)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun getAllKeys(): Future<List<String>> {
        val promise = Promise.promise<List<String>>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.complete(emptyList())
                    return@submit
                }

                val keys = mutableListOf<String>()
                val iterator = storage.iterator()
                while (iterator.hasNext()) {
                    val data = iterator.next()
                    keys.add(data.key)
                }

                promise.complete(keys)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun getKeysCount(): Future<Int> {
        val promise = Promise.promise<Int>()

        try {
            ioThread.submit {
                promise.complete(storage.size)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun have(key: String): Future<Boolean> {
        val promise = Promise.promise<Boolean>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.fail("Cache storage is empty")
                    return@submit
                }

                if (storage.containsKey(key)) {
                    promise.complete(true)
                } else {
                    promise.fail("Cache storage does not have $key")
                }
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    private fun evict() = safe {
        if (storage.size == 0) {
            return@safe
        }

        val iterator = storage.iterator()
        while (iterator.hasNext()) {
            val data = iterator.next().value

            if (data.ttl <= 0L) {
                continue
            }

            val lifeTime = data.ttlUnit.toSeconds(data.ttl)
            if ((data.addedTime + lifeTime) <= (System.currentTimeMillis() / 1000)) {
                iterator.remove()
            }
        }
    }

    fun evict(vararg keys: String): Future<Int> {
        val promise = Promise.promise<Int>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.complete(0)
                    return@submit
                }

                var count = 0
                for (key in keys) {
                    if (storage.containsKey(key)) {
                        val data = storage[key]
                        if (data != null) {
                            storage.remove(key)
                            count++
                        }
                    }
                }

                promise.complete(count)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun evictAllExcept(vararg keys: String): Future<Int> {
        val promise = Promise.promise<Int>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.complete(0)
                    return@submit
                }

                var count = 0
                val iterator = storage.iterator()
                while (iterator.hasNext()) {
                    val data = iterator.next()
                    if (data.key !in keys) {
                        iterator.remove()
                        count++
                    }
                }

                promise.complete(count)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    fun evict(regex: Regex): Future<Int> {
        val promise = Promise.promise<Int>()

        try {
            ioThread.submit {
                if (storage.size == 0) {
                    promise.complete(0)
                    return@submit
                }
                var count = 0
                val iterator = storage.iterator()
                while (iterator.hasNext()) {
                    val data = iterator.next()
                    if (regex.containsMatchIn(data.key)) {
                        iterator.remove()
                        count++
                    }
                }

                promise.complete(count)
            }
        } catch (e: Exception) {
            promise.fail(e)
        }

        return promise.future()
    }

    companion object {
        private var storage: Storage = Storage()

        fun getDefault(): Storage {
            return storage
        }

        fun newInstance(): Storage = Storage()
    }
}
