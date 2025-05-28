### Append a value to an array

Uses the arrayAppend function to append a value to the end of the array

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWGuuRJCEY0FBal6KhkIxTKWOgomCpjVXeVFmSap%2FaUlBaYmGglewv19walFmYk5mVSqSWgVNkFIA6BUetw%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
];
arrayAppend( someArray, 4 );
writeOutput( JSONSerialize( someArray ) );

```

Result: [1,2,3,4]

### Append a value to an array using the Array member function

Invoking the append function on an array is the same as running arrayAppend.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWGuuYpikXmJBQWpeioaCiYKmNVd5UWZJqn9pSUFpiYaCV7C%2FX3BqUWZiTmZVqoYCXIuCJkgpACxLHJo%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
];
someArray.append( 4 );
writeOutput( JSONSerialize( someArray ) );

```

Result: [1,2,3,4]

### Append more than one item

You can merge two arrays when third parameter is set to true.

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNNTh4jQCYmOuWGsusIRjQUFqXoqGQjFMpY5CNBenCVCNKRCbccXqKJQUlaYqaFpzlRdllqS6lOYWaCh4Bfv7BacWZSbmZFalImlW0AQpBACAdyFm" target="_blank">Run Example</a>

```java
someArray = [ 
	1,
	2,
	3
];
ArrayAppend( someArray, [
	4,
	5,
	6
], true );
writeDump( JSONSerialize( someArray ) );

```

Result: [1,2,3,4,5,6]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy9kL0KwkAQhOvsU0ypsBDyc0kgWASs9QHCFYpX3nlc7grf3hVBr4iCjcU0s7M7H%2BuSPZuwYIcZVFRMRS1qRC3pkaYQTrfJe%2BMuG7hnlqGwHWmfrH95YqAscUzRp7jIrYpRMxpG%2B4hrcu%2BelRp7DeaQJ5RMOlEvGj6D5Hs%2FIc1QjI7RMwbof%2FAxYkhmjZK%2BfC6DpDtjNG%2Bu" target="_blank">Run Example</a>

```java
numbers = [ 
	1,
	2,
	3,
	4
];
ArrayAppend( numbers, 5 );
Dump( numbers ); // Outputs [ 1, 2, 3, 4, 5 ]
numbers = [
	1,
	2,
	3,
	4
];
moreNumbers = [
	5,
	6,
	7,
	8
];
ArrayAppend( numbers, moreNumbers );
Dump( numbers ); // Outputs [ 1, 2, 3, 4, [ 5, 6, 7, 8 ] ]
numbers = [
	1,
	2,
	3,
	4
];
moreNumbers = [
	5,
	6,
	7,
	8
];
ArrayAppend( numbers, moreNumbers, true );
Dump( numbers );
 // Outputs [ 1, 2, 3, 4, 5, 6, 7, 8 ]

```


