### Angle Cosine of 0.3



<a href="https://try.boxlang.io/?code=eJxLTM4v1lDQM1bQtOYCABboAt4%3D" target="_blank">Run Example</a>

```java
acos( .3 )

```

Result: 1.2661036727794992

### Additional Examples


```java
<bx:set inputValue = .5 >
<bx:output>#aCos( inputValue )#</bx:output>
```

### Arccosine of 1

```java
writeOutput( acos( 1 ) )

```

Result: 0

### Input outside the range -1 to 1

```java
try {
	acos( 2 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Input value must be in the range [-1, 1] for ACos function.
