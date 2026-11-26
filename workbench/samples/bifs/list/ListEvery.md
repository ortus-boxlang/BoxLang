### Example for positive result

Checks whether all items in a list are greater than 2 and outputs true because all of them fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDLRMdUx0zFX4iovyixJ9S8tKSgt0VDIAcq6lqUWVUKYOgoaCo55lQpliTmlqQqaCrZ2CtVcnEWpJaVFeVBBOwUjrlqglCYA1YEa4g%3D%3D" target="_blank">Run Example</a>

```java
list = "4,5,6,7"
writeOutput( listEvery( list, ( Any value ) => {
	return value > 2
} ) )

```

Result: true

### Example for negative result

Checks whether all items in a list are greater than 2 and outputs false because some of them do not fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDLUMdIx1jFR4iovyixJ9S8tKSgt0VDIAcq6lqUWVUKYOgoaCo55lQpliTmlqQqaCrZ2CtVcnEWpJaVFeVBBOwUjrlqglCYA0dka1g%3D%3D" target="_blank">Run Example</a>

```java
list = "1,2,3,4"
writeOutput( listEvery( list, ( Any value ) => {
	return value > 2
} ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjEEKgzAQRdfmFB9XCeQKFgq66yUCHUsgTcM4Yyvi3S3Jqt09Hu%2F%2FmTXKggF9KCWRLxTYvzjkB%2FXmFheZVuLNYq6dh8U1b1hDUvIVY77Tp2H65nAYLthN9%2BYoNOqz2JbA%2Fbj68Ofa3nRMopwhrGQOuBNzNTV6" target="_blank">Run Example</a>

```java
fruits = "apple,pear,orange"
ListEvery( fruits, ( Any value, Any index, Any list ) => {
	writeDump( index )
	writeDump( value )
	writeDump( list )
	return true
} )

```

Result: true

### Member function

```java
writeOutput( "2,4,6".listEvery( ( value ) -> value > 1 ) )

```

Result: true
