### Changing a struct compared to changing its copy

myNewStruct holds a reference to myStruct, so changing myNewStruct changes myStruct as well. myOtherNewStruct is a copy, so changing it leaves myStruct untouched.

```java
myStruct = { foo : "Lorem ipsum", bar : "baz" }
myNewStruct = myStruct
myOtherNewStruct = duplicate( myStruct )
myNewStruct.foo = "ahmet sun"
myOtherNewStruct.foo = "dolor sit"
writeOutput( myStruct.foo & " -> " & myNewStruct.foo & " -> " & myOtherNewStruct.foo )
```

Result: ahmet sun -> ahmet sun -> dolor sit

### Deep copy vs shallow copy

By default nested values are copied too. With deep set to false only the top level is copied, so nested structs are shared.

```java
original = { inner : { x : 1 } }
deepCopy = duplicate( original )
shallowCopy = original.duplicate( false )
original.inner.x = 2
writeOutput( deepCopy.inner.x & " " & shallowCopy.inner.x )
```

Result: 1 2
