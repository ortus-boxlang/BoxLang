### Simple listIndexExists

Check whether the index is exists or not in list


```java
<bx:set list = "Apple,Orange,Banana,Graphs" >
<bx:if listIndexExists( list, 2 ) >
	<bx:set list = listsetAt( list, 2, "Goa" ) >
</bx:if>
<bx:output>#list#</bx:output>
```

Result: Apple,Goa,Banana,Graphs

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljbEKwjAYhPc%2BxZGphR8zCG4OghUKrUufoNVfCaRJSf5oHt9aBQenO46777RGb6bZMuo8vLV4BiPsk8xJSlgTpXFXznVeXCyh%2BhQNtYeeaPTZDu5ORMJRFGGLClWhNTqeRg44JXcR410RJbTLHHsoaoisf%2FBvrT6PxzTNJb7Nzd%2FvbmVjgZ%2B9gNeUEFhScBG3wUZ%2BARF3Q6A%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listIndexExists( "Susi,LAS,,boxlang,,,test", 3 ) )
// Member Function
strList = ",I,,love,boxlang,,"
writeDump( strList.listIndexExists( 6 ) )
 // Not exists, returns false

```

### Index within the list

```java
writeOutput( listIndexExists( "a,b,c", 3 ) )

```

Result: true

### Index past the end of the list

```java
writeOutput( listIndexExists( "a,b,c", 4 ) )

```

Result: false

### Member function with an invalid index

```java
writeOutput( "a,b,c".listIndexExists( 0 ) )

```

Result: false
