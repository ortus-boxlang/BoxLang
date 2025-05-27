### simple isArray example

For CF9+ CF10+

<a href="https://try.boxlang.io/?code=eJxLzs%2FJL3IsKkqsVLBViFbg4lSqTM3JyS9X0gEy04tSU%2FPArKLUFCWuWGuu8qLMklT%2F0pKC0hINhcxisEYNhWSEIZoKmtZcAGG2GiQ%3D" target="_blank">Run Example</a>

```java
colorArray = [ 
	"yellow",
	"green",
	"red"
];
writeOutput( isArray( colorArray ) );

```

Result: yes

### simple isArray example

For Lucee4.5+

<a href="https://try.boxlang.io/?code=eJxLzs%2FJL3IsKkqsVLBViFbg4lSqTM3JyS9X0gEy04tSU%2FPArKLUFCWuWGuu8qLMklT%2F0pKC0hINhcxisEYNhWSEIZoKmtZcAGG2GiQ%3D" target="_blank">Run Example</a>

```java
colorArray = [ 
	"yellow",
	"green",
	"red"
];
writeOutput( isArray( colorArray ) );

```

Result: true

### isArray example with number

For CF9+ CF10+

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65Ya67yosySVP%2FSkoLSEg2FzGKwOg2FPIQmHQVDBU0FTWsuABqtFVc%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
];
writeOutput( isArray( numberArray, 1 ) );

```

Result: yes

### isArray example with number

For Lucee4.5+

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65Ya67yosySVP%2FSkoLSEg2FzGKwOg2FPIQmHQVDBU0FTWsuABqtFVc%3D" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
];
writeOutput( isArray( numberArray, 1 ) );

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUMhJTErNsVVyzS0oqVRwLCpKrFTSUShLLLLNLAbzNBSiYxU0FTStucoxNIEV%2BKWWo%2BtIhIprKBiCtOLXrFCQX5xZkpmfZ4zbGGMFTR0Qgd0kp%2Fz8nNTEPKDmnNJUdENKikpTcWkMLinKzEvHrk8JbLsSRCsAyt1jGg%3D%3D" target="_blank">Run Example</a>

```java
writeDump( label="Empty Array", var=isArray( [] ) );
writeDump( label="ArrayNew", var=isArray( arrayNew( 1 ) ) );
writeDump( label="ArrayNew position3", var=isArray( arrayNew( 3 ), 3 ) );
writeDump( label="Boolean value", var=isArray( true ) );
writeDump( label="String value", var=isArray( "array" ) );

```


