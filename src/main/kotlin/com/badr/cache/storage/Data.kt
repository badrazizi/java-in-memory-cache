package com.badr.cache.storage

import java.util.concurrent.TimeUnit

class Data<T : Any> {
    var key: String = ""
        private set

    lateinit var value: T
        private set

    var addedTime: Long = System.currentTimeMillis() / 1000
        private set

    var ttl: Long = 0
        private set

    var ttlUnit: TimeUnit = TimeUnit.SECONDS
        private set

    fun setKey(value: String) = apply {
        this.key = value
    }

    fun updateAddedTime() = apply {
        this.addedTime = System.currentTimeMillis() / 1000
    }

    fun setValue(value: T) = apply {
        this.value = value
    }

    fun setTTL(value: Long) = apply {
        this.ttl = value
    }

    fun setTTLUnit(value: TimeUnit) = apply {
        this.ttlUnit = value
    }

    override fun toString(): String = "$key: $value"

    override fun hashCode(): Int {
        var result = 2
        result = 27 * result + key.hashCode()
        result = 27 * result + value.hashCode()
        result = 27 * result + addedTime.hashCode()
        result = 27 * result + ttl.hashCode()
        result = 27 * result + ttlUnit.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Data<*>

        if (key != other.key) return false
        if (value != other.value) return false
        if (ttl != other.ttl) return false
        if (ttlUnit != other.ttlUnit) return false

        return true
    }
}
