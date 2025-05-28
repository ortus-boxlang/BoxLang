### Average value of an array

Uses the arrayAvg function to get average value of an array

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65Ya67yosySVP%2FSkoLSEg2FRJAqx7J0DYU8JF2aCprWXADx0hU8" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
];
writeOutput( arrayAvg( numberArray ) );

```

Result: 2

### Average value of an array using the Array member function

Uses the avg member function is the same as running arrayAvg.

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yLCpKrFSwVYhW4OI01OHiNAJiY65Ya67yosySVP%2FSkoLSEg2FPIRavcSydA1NBU1rLgB0WBMr" target="_blank">Run Example</a>

```java
numberArray = [ 
	1,
	2,
	3
];
writeOutput( numberArray.avg() );

```

Result: 2

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLK81NSi0qVrBViFbg4jTU4eI0AmJjIDbhirXmSixLD0vMKU0FyjsWFSVWOpalayjkQfVoWnO5JmfkayjAVQFFFPT1FfJLSwpKS4oVjPRMuQD2qRrz" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
avgValue = ArrayAvg( numbers );
Echo( avgValue );
 // outputs 2.5

```


