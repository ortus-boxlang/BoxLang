### Get the list of loaded modules

The result is a struct keyed by module name.

```java
modules = getModuleList()
writeOutput( isStruct( modules ) )
```

Result: true

### List the module names

```java
modules = getModuleList()
writeOutput( modules.keyList() )
```

Result: a comma-delimited list of the registered module names (depends on the installed modules)
