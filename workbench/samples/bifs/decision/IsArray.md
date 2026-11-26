### simple isArray example


<a href="https://try.boxlang.io/?code=eJxLzs%2FJL3IsKkqsVLBViFbg4lSqTM3JyS9X0gEy04tSU%2FPArKLUFCWuWK7yosySVP%2FSkoLSEg2FzGKwPg2FZIQZmgqaABfzGaQ%3D" target="_blank">Run Example</a>

```java
colorArray = [ 
	"yellow",
	"green",
	"red"
]
writeOutput( isArray( colorArray ) )

```

Result: yes

### simple isArray example


<a href="https://try.boxlang.io/?code=eJxLzs%2FJL3IsKkqsVLBViFbg4lSqTM3JyS9X0gEy04tSU%2FPArKLUFCWuWK7yosySVP%2FSkoLSEg2FzGKwPg2FZIQZmgqaABfzGaQ%3D" target="_blank">Run Example</a>

```java
colorArray = [ 
	"yellow",
	"green",
	"red"
]
writeOutput( isArray( colorArray ) )

```

Result: true

### isArray example with number


<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65YrvKizJJU%2F9KSgtISDYXMYrAyDYU8hB4dBUMFTQVNAN89FNc%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
]
writeOutput( isArray( numberArray, 1 ) )

```

Result: yes

### isArray example with number


<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65YrvKizJJU%2F9KSgtISDYXMYrAyDYU8hB4dBUMFTQVNAN89FNc%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
]
writeOutput( isArray( numberArray, 1 ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVVyzS0oqVRwLCpKrFTSUShLLLLNLAbzNBSiYxU0FTS5yjH0gOX9UsvRNSRCxTUUDEE68epVKMgvzizJzM8zxm2KsYKmDojAapBTfn5OamIeUG9OaSq6GSVFpak49AWXFGXmpWPXpgS2WwmkEwC2yGHp" target="_blank">Run Example</a>

```java
writeDump( label="Empty Array", var=isArray( [] ) )
writeDump( label="ArrayNew", var=isArray( arrayNew( 1 ) ) )
writeDump( label="ArrayNew position3", var=isArray( arrayNew( 3 ), 3 ) )
writeDump( label="Boolean value", var=isArray( true ) )
writeDump( label="String value", var=isArray( "array" ) )

```


