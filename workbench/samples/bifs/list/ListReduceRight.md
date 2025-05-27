### Simple listReduceRight Example

Demonstrate how the function works from right to left.

<a href="https://try.boxlang.io/?code=eJw1jUEKwjAQRdfmFJ8sJIW5Qajg3lVvUJtBAyaUMbUt0rt3SHX1H58HL623%2BC5oYXu600DBepN5%2Fp0vnY7DNHAXH8%2FikKpOcLjmFaPwhyplXspBMSwH9CJo0F7wNSfhMkmuPs5V1lHTm41gLRpvZolFS2l0%2BPf13QHc5DHQ" target="_blank">Run Example</a>

```java
myList = "a,b,c,d";
newList = listReduceRight( myList, ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx;
}, "" );
writedump( newList );

```

Result: d4c3b2a1

### listReduceRight as a Member Function

Demonstrate the member function.

<a href="https://try.boxlang.io/?code=eJw1jU0KwjAQhdfmFI8sJIXgBUIF9656g9oMGjChjNM%2FxLs7pLqZ%2BXjvg5e3a3oJWtje3%2Fzgow2m0PILc21PTz0dxWmgLt0f4uBwKRtGptlXKrTKTimuO%2FTMaNCe8TYHJpm4VB%2FHKutTM5iPh7Voglk4iS7k0eE%2Fr%2BkXv84xsg%3D%3D" target="_blank">Run Example</a>

```java
myList = "a,b,c,d";
newList = myList.listReduceRight( ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx;
}, "" );
writedump( newList );

```

Result: d4c3b2a1

### Empty Elements

Demonstrate the behavior when there is an empty element.

<a href="https://try.boxlang.io/?code=eJzLrfTJLC5RsFVQStQBgSQgTgbiFCVrrrzUcqhkLliVXg6QCEpNKU1ODcpMzyjRUNBQcMyrVCgoSi3TAbPyUitKIKzMlAoFTQVbO4VqLs6i1JLSojywMgU1sBogBVRgzVWro6CkpKBpzVVelFkCNDi3QEMBZitQFADheTA5" target="_blank">Run Example</a>

```java
myList = "a,,,,,b,,,c,,,d";
newList = myList.listReduceRight( ( Any prev, Any next, Any idx ) => {
	return prev & next & idx;
}, "" );
writedump( newList );

```

Result: d4c3b2a1

