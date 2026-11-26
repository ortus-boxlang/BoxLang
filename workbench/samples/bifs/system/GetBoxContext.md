### Get the current BoxLang context

```java
ctx = getBoxContext()
writeOutput( !isNull( ctx ) )
```

Result: true

### Call a Java method on the context

The returned object is the Java IBoxContext, so its public methods can be called.

```java
ctx = getBoxContext()
writeOutput( isSimpleValue( ctx.getClass().getSimpleName() ) )
```

Result: true (the actual class name depends on where the code is executing)
