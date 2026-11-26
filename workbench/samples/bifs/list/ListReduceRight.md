### Simple listReduceRight Example

Demonstrate how the function works from right to left.

<a href="https://try.boxlang.io/?code=eJw1jUEKgzAQRdfNKT5ZSAJzBQvuXXkDa4Y2YIJMx6qU3r0htqv%2F%2BDx46ejjU9HCjnSjiYI1mbffN5cZOKwTD%2FH%2BUIdUbYJDlw8swi%2BqlHnXk2LYTxhF4NFe8TYXYV0lVx9NlcsU03wI1sKbTaKWTloc%2FnX%2FBfYdMNo%3D" target="_blank">Run Example</a>

```java
myList = "a,b,c,d"
newList = listReduceRight( myList, ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx
}, "" )
writedump( newList )

```

Result: d4c3b2a1

### listReduceRight as a Member Function

Demonstrate the member function.

<a href="https://try.boxlang.io/?code=eJzLrfTJLC5RsFVQStRJ0knWSVHiyksth4rlgiX1coBEUGpKaXJqUGZ6RomGgoaCY16lQkFRapkOmJWXWlECYWWmVEAYiUVFCpoKtnYK1VycRaklpUV5YPUKamDFQAqokqtWR0FJSUGTq7woswRofm6BhgLMck0A2bowvA%3D%3D" target="_blank">Run Example</a>

```java
myList = "a,b,c,d"
newList = myList.listReduceRight( ( Any prev, Any next, Any idx, Any arr ) => {
	return prev & next & idx
}, "" )
writedump( newList )

```

Result: d4c3b2a1

### Empty Elements

Demonstrate the behavior when there is an empty element.

<a href="https://try.boxlang.io/?code=eJzLrfTJLC5RsFVQStQBgSQgTgbiFCWuvNRyqFwuWJFeDpAISk0pTU4NykzPKNFQ0FBwzKtUKChKLdMBs%2FJSK0ogrMyUCgVNBVs7hWouzqLUktKiPLAyBTWwGiAFVMBVq6OgpKSgyVVelFkCNDa3QEMBZqcmAAILL0M%3D" target="_blank">Run Example</a>

```java
myList = "a,,,,,b,,,c,,,d"
newList = myList.listReduceRight( ( Any prev, Any next, Any idx ) => {
	return prev & next & idx
}, "" )
writedump( newList )

```

Result: d4c3b2a1

