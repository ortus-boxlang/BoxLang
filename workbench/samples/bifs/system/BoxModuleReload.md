### Reload a single module by name

The BIF returns null.

```java
result = boxModuleReload( "bx-compat-cfml" )
writeOutput( isNull( result ) )
```

Result: true

### Reload all modules

Omit the module argument to reload every registered module.

```java
boxModuleReload()
```

Result: all registered modules are reloaded; no output
