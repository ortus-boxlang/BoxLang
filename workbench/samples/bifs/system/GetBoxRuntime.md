### Get the BoxLang runtime

The runtime is the Java BoxRuntime singleton.

```java
runtime = getBoxRuntime()
writeOutput( !isNull( runtime ) )
```

Result: true

### Read the version from the runtime

```java
runtime = getBoxRuntime()
writeOutput( runtime.getVersionInfo().version == getBoxVersionInfo().version )
```

Result: true
