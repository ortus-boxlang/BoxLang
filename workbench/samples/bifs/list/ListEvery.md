### Example for positive result

Checks whether all items in a list are greater than 2 and outputs true because all of them fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDLRMdUx0zFXsuYqL8osSfUvLSkoLdFQyAFKu5alFlVCmDoKGgqOeZUKZYk5pakKmgq2dgrVXJxFqSWlRXlQQTsFI2uuWqCcpjUXAD1HG50%3D" target="_blank">Run Example</a>

```java
list = "4,5,6,7";
writeOutput( listEvery( list, ( Any value ) => {
	return value > 2;
} ) );

```

Result: true

### Example for negative result

Checks whether all items in a list are greater than 2 and outputs false because some of them do not fulfill the requirement.

<a href="https://try.boxlang.io/?code=eJzLySwuUbBVUDLUMdIx1jFRsuYqL8osSfUvLSkoLdFQyAFKu5alFlVCmDoKGgqOeZUKZYk5pakKmgq2dgrVXJxFqSWlRXlQQTsFI2uuWqCcpjUXADlvG5E%3D" target="_blank">Run Example</a>

```java
list = "1,2,3,4";
writeOutput( listEvery( list, ( Any value ) => {
	return value > 2;
} ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxdjMEKwjAQBc%2FmKx49pZA%2FCBUEvfkTAbclEGPY7laL%2BO%2FW5FRvwzDMyBplxoAulJLIFQrsHhzyRJ031zjLZSFeLcYaOlic8oolJCVXMeYbvRqmLUeP4Yi3OTw5Cp31XmxL0PudrIt%2F2Q6bYxLlDGElbz4%2F9QXZuzbm" target="_blank">Run Example</a>

```java
fruits = "apple,pear,orange";
ListEvery( fruits, ( Any value, Any index, Any list ) => {
	writeDump( index );
	writeDump( value );
	writeDump( list );
	return true;
} );

```

Result: true

