### Get the request class loader

```java
loader = getRequestClassLoader()
writeOutput( !isNull( loader ) )
```

Result: true

### Load a Java class through the loader

The loader can be passed to createObject() to load Java classes from the request class path.

```java
loader = getRequestClassLoader()
list = createObject( type = "java", className = "java.util.ArrayList", classLoader = loader ).init()
writeOutput( list.size() )
```

Result: 0
