### Get all runtime settings

With no key, the whole merged runtimeSettings struct is returned.

```java
settings = getRuntimeSetting()
writeOutput( isStruct( settings ) )
```

Result: true

### Read a nested setting defined in the Application

Settings in this.runtimeSettings are deep merged over the ones in boxlang.json and can be read with a dotted path.

```java
// Application.bx
class {
    this.name = "myApp"
    this.runtimeSettings = { features : { beta : true } }
}

// in any template
writeOutput( getRuntimeSetting( "features.beta" ) )
```

Result: true

### Provide a default value

The default is returned when the setting does not exist.

```java
writeOutput( getRuntimeSetting( "myapp.notDefined", "fallback" ) )
```

Result: fallback

### Missing setting without a default

```java
try {
    getRuntimeSetting( "myapp.notDefined" )
} catch ( any e ) {
    writeOutput( e.message )
}
```

Result: The runtime setting [myapp.notDefined] was not found and no default value was provided.
