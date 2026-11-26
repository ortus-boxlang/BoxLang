### Simple example with empty array

To get the smallest numeric value of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFVIBNF%2BqeUaCoYKmlzlRZklqf6lJQWlJRoQKd%2FMPA2FYrgGTQVNAKjEFRo%3D" target="_blank">Run Example</a>

```java
someArray = arrayNew( 1 )
writeOutput( arrayMin( someArray ) )

```

Result: 0

### Get smallest numeric value of an array

Uses the arrayMin function to get the smallest numeric value of an array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDLW4eI0MQUSFuZAwgjE44rlKi%2FKLEn1Ly0pKC3RUEgEqffNzNNQKIbr1lTQBADdeBSj" target="_blank">Run Example</a>

```java
someArray = [ 
	23,
	45,
	87,
	2,
	4
]
writeOutput( arrayMin( someArray ) )

```

Result: 2

### Get smallest numeric value of an array using member function



<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDLW4eI0MQUSlpZAwhDE44rlKi%2FKLEn1Ly0pKC3RUCiG6dHzzczT0FTQBABkKxJ0" target="_blank">Run Example</a>

```java
someArray = [ 
	23,
	45,
	99,
	1,
	4
]
writeOutput( someArray.Min() )

```

Result: 1

### Additional Examples


```java
aNames = [ 10412, 42, 33, 2, 999, 12769, 888 ]
dump( arrayMin( aNames ) )
// member function
dump( aNames.min() )

```


