# Single Thread In-Memory Cache

### Async generic thread-safe cache

### usage
- add
> ```kotlin 
> fun <T : Any> add(key: String, value: T, lifeTime: Long = 0, unit: TimeUnit = TimeUnit.SECONDS): Future<Boolean>
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.add(key, value, time, timeUnit).onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```

- get by key
> ```kotlin 
> fun <T> get(key: String): Future<T>
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.get<Post>(title).onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```

- get by predicate
> ```kotlin 
> fun <T> get(predicate: (T) -> Boolean): Future<T>
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.get<Post> { p -> p.title == title }.onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```

- get multiple value as HashMap<String, Any>
> ```kotlin 
> fun get(vararg keys: String): Future<HashMap<String, Any>> 
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.get(title1, title2, title3).onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```

- get keys using regex
> ```kotlin 
> fun getKeys(vararg regexs: Regex): Future<List<String>>
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.get("Posts\\.${id}".toRegex()).onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```

- evict by key
> ```kotlin 
> fun evict(vararg keys: String): Future<Int>
> ```
```kotlin
val posts: Storage = Storage.newInstance()
posts.evict(title).onComplete { ar ->
    if (ar.succeeded()) {
        // do something
    } else {
        // do something
    }
}
```
