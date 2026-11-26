### Tag Syntax




```java
<bx:set MyArray = arrayNew( 1 ) > 
 <!--- Resize that array to the number of records in the query. ---> 
 <bx:set temp = <!--- Transpiler workaround for BIF return type --->(( <bx:argument>, <bx:argument> ) => <bx:set arrayResize( arg1, arg2 ) ><bx:return true>)( MyArray, 8 ) > 
  <bx:dump var="#MyArray#"/>  
```


### Pad an array to a minimum size

```java
numbers = [ 1, 2 ]
numbers.resize( 4 )
writeOutput( numbers.len() )
```

Result: 4

### An array is never shrunk

If the array already has at least that many elements it is left unchanged.

```java
numbers = [ 1, 2, 3 ]
arrayResize( numbers, 1 )
writeOutput( numbers.len() )
```

Result: 3

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhiuVKLCpKrAxKLc6sStVQyIMo1VEwNFDQ5HIpzS2AiwH5%2BvoKuakgjkJaaV5ySWZ%2BHhdUUq8IaoChKaY%2BACCdJmg%3D" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
]
arrayResize( numbers, 10 )
Dump( numbers )
// member function
numbers.resize( 15 )
Dump( numbers )

```


