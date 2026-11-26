### Sleep for one second

The duration is in milliseconds.

```java
start = getTickCount()
sleep( 1000 )
writeOutput( getTickCount() - start >= 1000 )
```

Result: true

### Time a sleep with bx:timer

```java
bx:timer label="Sleep for 1 second" type="inline" {
    sleep( 1000 )
}
```

Result: Sleep for 1 second: 1000ms (the timing is approximate)
