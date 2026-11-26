### Get the registered components

The result is a struct keyed by component name.

```java
components = getComponentList()
writeOutput( isStruct( components ) )
```

Result: true

### Inspect a component descriptor

```java
components = getComponentList()
first = components[ components.keyArray()[ 1 ] ]
writeOutput( first.keyList() )
```

Result: module,className,allowsBody,requiresBody,ignoreEnableOutputOnly,autoEvaluateBodyExpressions,attributes (the key order may vary)
