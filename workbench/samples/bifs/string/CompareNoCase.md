### Case-insensitive comparison

```java
writeOutput( compareNoCase( "Boxlang", "boxlang" ) )

```

Result: 0

### Less than and greater than

```java
writeOutput( compareNoCase( "100 MAIN ST.", "675 EAST AVENUE" ) & " " & compareNoCase( "B", "a" ) )

```

Result: -1 1

### Member function

```java
writeOutput( "HELLO".compareNoCase( "hello" ) )

```

Result: 0
