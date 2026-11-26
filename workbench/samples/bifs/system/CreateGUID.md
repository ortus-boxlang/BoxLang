### Create a GUID

Each call returns a new, uppercase UUID v4 string in the 8-4-4-4-12 format. The value shown here is an example; it is different every time.

```java
writeOutput( createGUID() )
```

Result: a 36-character uppercase string such as 38E5D44A-8D6A-42BF-B3A5-23FB2542786C

### GUIDs are unique

```java
a = createGUID()
b = createGUID()
writeOutput( ( a != b ) & " " & len( a ) )
```

Result: true 36
