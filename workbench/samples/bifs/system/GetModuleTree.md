### Get the full module tree

Each node carries the module record data plus a children struct holding nested modules.

```java
tree = getModuleTree()
writeOutput( isStruct( tree ) )
```

Result: true

### Unknown module returns an empty struct

Passing a module name roots the tree at that module; an unregistered name returns an empty struct.

```java
tree = getModuleTree( "bogus" )
writeOutput( structIsEmpty( tree ) )
```

Result: true
