### Simple stringReduceRight Example

Demonstrate how the function works from right to left.

<a href="https://try.boxlang.io/?code=eJxFjkEKwjAQRdfmFJ8sJIXcIFTwCvUEbTPULDKUcWpbxLsbUtHVf3zeMD%2FvN5XEE1rYfhijDYZp%2FXWPCh3FZaQuTXd1yN8LD4cr75iFnr4S06YHpbgd0IugQXvBy5yEdBGuPs5VLlHMYN4e1qIJZpWk5VeeHf4rSv8BBnM1kw%3D%3D" target="_blank">Run Example</a>

```java
myString = "abcd";
newString = stringReduceRight( myString, ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx;
}, "" );
writedump( newString );

```

Result: d4c3b2a1

### How Do You Do This In Lucee?

This function will be added to Lucee in Version 6. But if you need to reverse a string now, use the `reverse()` function.

<a href="https://try.boxlang.io/?code=eJzLrQwuKcrMS1ewVVBKTEpOUbLmyksth4vlQqX1ilLLUouKUzU0rbnKizJLUlNKcws0FBBKgeIA3H4ZwQ%3D%3D" target="_blank">Run Example</a>

```java
myString = "abcd";
newString = myString.reverse();
writedump( newString );

```

Result: dcba

