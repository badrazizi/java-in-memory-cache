package com.badr.cache.storage

import java.util.concurrent.TimeUnit

class Data<T: Any> {
  var key: String = ""
    private set

  lateinit var value: T
    private set

  var added: Long = System.currentTimeMillis() / 1000
    private set

  var lifeTime: Long = 0
    private set

  var timeUnit: TimeUnit = TimeUnit.SECONDS
    private set

  fun setKey(value: String) = apply {
    this.key = value
  }

  fun updateAdded() = apply {
    this.added = System.currentTimeMillis() / 1000
  }

  fun setValue(value: T) = apply {
    this.value = value
  }

  fun setLifeTime(value: Long) = apply {
    this.lifeTime = value
  }

  fun setTimeUnit(value: TimeUnit) = apply {
    this.timeUnit = value
  }

  override fun toString(): String = "$key: $value"

  override fun hashCode(): Int {
    var result = 2
    result = 27 * result + key.hashCode()
    result = 27 * result + value.hashCode()
    result = 27 * result + added.hashCode()
    result = 27 * result + lifeTime.hashCode()
    result = 27 * result + timeUnit.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Data<*>

    if (key != other.key) return false
    if (value != other.value) return false
    if (lifeTime != other.lifeTime) return false
    if (timeUnit != other.timeUnit) return false

    return true
  }


}
