package com.badr.cache.extensions

fun <T>safe(method: () -> T): T? {
  return try {
    method()
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

fun <T>safeWithDefault(default: T, method: () -> T): T {
  return try {
    method()
  } catch (e: Exception) {
    e.printStackTrace()
    default
  }
}
