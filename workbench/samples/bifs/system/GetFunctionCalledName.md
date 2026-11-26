### Get the name of the executing function

```java
function greet() {
    return getFunctionCalledName()
}
writeOutput( greet() )
```

Result: greet

### Called through a reference

The called name follows the variable used to invoke the function.

```java
function test() {
    return getFunctionCalledName()
}
anotherTest = test
writeOutput( anotherTest() )
```

Result: anotherTest

### Called outside of a function

```java
writeOutput( "[" & getFunctionCalledName() & "]" )
```

Result: []
