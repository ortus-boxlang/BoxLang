### Get the context root

Outside of a servlet container the context root is always /.

```java
writeOutput( getContextRoot() )
```

Result: /

### Build a URL from the context root

```java
writeOutput( getContextRoot() & "index.bxm" )
```

Result: /index.bxm
