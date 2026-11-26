### Tag Syntax




```java
<bx:set MyNewArray = arrayNew( 1 ) > 
<!--- ArrayToList does not function properly if the Array has not been initialized with arraySet. ---> 
<bx:set temp = <!--- Transpiler workaround for BIF return type --->(( <bx:argument>, <bx:argument>, <bx:argument>, <bx:argument> ) => <bx:set arraySet( arg1, arg2, arg3, arg4 ) ><bx:return true>)( MyNewArray, 1, 6, "Initial Value" ) > 
<bx:output>#ArrayToList( myNewArray, ", " )#</bx:output>
```

Result: Initial Value, Initial Value, Initial Value, Initial Value, Initial Value, Initial Value

### Initialize an array

Setting a range beyond the end of an empty array expands it.

```java
arr = arrayNew( 1 )
arr.set( 1, 3, "x" )
writeOutput( arr.toList() )
```

Result: x,x,x

### Overwrite part of an array

```java
numbers = [ 1, 2, 3, 4 ]
arraySet( numbers, 2, 3, 0 )
writeOutput( numbers.toList() )
```

Result: 1,0,0,4

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLSy13LCpKrFSwVUgE0X6p5RoKhgqaXGBecGqJhkIeVImOgqGOgomOgpJjTo5CZrFCeWpOjhJQZUppbgFCFVBAX18hNzU3KbVIIa00L7kkMz%2BPCyarVwwyEWiOEdCcnNSSYoXkjMS89FSFzBJsRgEAmbsyCQ%3D%3D" target="_blank">Run Example</a>

```java
newArray = arrayNew( 1 )
arraySet( newArray, 1, 4, "All is well" )
dump( newArray )
// member function
newArray.set( 1, 2, "lets change it" )
dump( newArray )

```


