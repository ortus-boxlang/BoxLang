### Example for positive result

Checks whether all items in an array are greater than 2 and outputs true because all of them fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI00eHiNAViMyA254rlKi%2FKLEn1Ly0pKC3RUEgEKXQtSy2qhLJ1FDQUHPMqFcoSc0pTFTQVbO0Uqrk4i1JLSovyoIJ2CkZctUApTQCpeBz1" target="_blank">Run Example</a>

```java
array = [ 
	4,
	5,
	6,
	7
]
writeOutput( arrayEvery( array, ( Any value ) => {
	return value > 2
} ) )

```

Result: true

### Example for negative result

Checks whether all items in an array are greater than 2 and outputs false because some of them do not fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJxLLCpKrFSwVYhW4OI01OHiNAJiYyA24YrlKi%2FKLEn1Ly0pKC3RUEgEKXQtSy2qhLJ1FDQUHPMqFcoSc0pTFTQVbO0Uqrk4i1JLSovyoIJ2CkZctUApTQCliBzp" target="_blank">Run Example</a>

```java
array = [ 
	1,
	2,
	3,
	4
]
writeOutput( arrayEvery( array, ( Any value ) => {
	return value > 2
} ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljkELgjAYhs%2Fbr3jxpDC0Qi%2BBgocKgrp0jJCB89Kc8umCEf73JhgYXR8envdtXSWJpEOOOzh7c8au5eWAPYIjSfMMhCflaQbphrNJ%2FDo3q1bGbvtvnLthrWSpV%2FiDS62rTtd%2Btl0exOqlyIUIURqHXtHQGUTIC%2FgcqdGSWWg8p4p8PjQJjGSVQIaI17btQ3zLEUeSoJF6UB%2BQWTq5" target="_blank">Run Example</a>

```java
my_array = [ 
	{
		NAME : "Frank",
		AGE : 40
	},
	{
		NAME : "Sue",
		AGE : 21
	},
	{
		NAME : "Jose",
		AGE : 54
	}
]
all_old = my_array.every( ( Any person ) => {
	return person.AGE >= 40
}, true, 5 )
dump( all_old )
 // false

```


