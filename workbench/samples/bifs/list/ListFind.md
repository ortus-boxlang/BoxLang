### Basic Example

Find item in a list and return the index.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnHLzEvxy3dOLE7VUFBKLCjISdXJL0rMS0%2FVSUrMA0IlHQUliICSgqaCJgD%2BZRXs" target="_blank">Run Example</a>

```java
writeOutput( listFindNoCase( "apple,orange,banana", "orange" ) )

```

Result: 2

### Different Delimiters

Find item in a pipe delimited list and return the index.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnHLzEvxy3dOLE7VUFBKLCjISa3JL0rMS0%2BtSUrMA0IlHQUliACIVaOkoKmgCQCDUheY" target="_blank">Run Example</a>

```java
writeOutput( listFindNoCase( "apple|orange|banana", "orange", "|" ) )

```

Result: 2

### Member Syntax

listFindNoCase as a member function

<a href="https://try.boxlang.io/?code=eJxLKyrNLClWsFVQSiwoyEmtyS9KzEtPrUlKzANCJa7yosySVP%2FSkoLSEg2FNLBavZzM4hK3zLwUv3znxOJUDQUl%2FyBHP3dXJR0FpRolBU0FTQAh%2Bxx7" target="_blank">Run Example</a>

```java
fruits = "apple|orange|banana"
writeOutput( fruits.listFindNoCase( "ORANGE", "|" ) )

```

Result: 2

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjrEOgjAURfd%2BxUsnSIgMuhmTqogxQVhdgb5ok5aS8gp8voWw6XrvzT1ncoqw8tR7ikCrgXLVydJe6wEj4I9E2xGTxs667t4J4dJr5AnwS%2FUqzuWdQwzxEdIUbnOPLaEEu77BnoXwiaZBB7nvWlK2g0nRBwRI1MoEsGMDuSJQ4RRgojZiQwmJI2fTIpd500ew7XY%2FilkYBh2xirB%2FIocvLWtK6w%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listFindNoCase( "I,love,boxlang,testFile", "BOXLANG" ) ); // Expected output 3
// Member Function with @ delimiter
strList = "I@am@boxlang@dev"
writeDump( strList.listFindNoCase( "Dev", "@" ) )
 // Expected output 4

```


