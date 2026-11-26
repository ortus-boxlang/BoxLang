### Arcsine of 0.3



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSCzOzNNQ0DNW0FTQBAB5gwfr" target="_blank">Run Example</a>

```java
writeOutput( asin( .3 ) )

```

Result: 0.3046926540153975

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrzsxLVbBVMORKKc0t0FBwLM7M01AoBglqKmhyKejrKxjqmZobmFuaGRuZmVuaKmTmKRQlpmQm5hUDAOCHD%2Fw%3D" target="_blank">Run Example</a>

```java
sine = 1
dump( Asin( sine ) )
 // 1.570796326795 in radians

```

### Arcsine of 1

```java
writeOutput( asin( 1 ) )

```

Result: 1.5707963267948966

### Input outside the range -1 to 1

```java
try {
	asin( 2 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Input value must be in the range [-1, 1] for ASin function.
