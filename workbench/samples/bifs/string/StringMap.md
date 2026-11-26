### Full function

Map each element of the string to a new value.

<a href="https://try.boxlang.io/?code=eJw1jUEKwjAQRdfmFJ%2BuJui%2BqxZ6AHHhCWI6rQMhhsmEIsW7K6jb9%2BC9xGasFQO6cIszL2vnYnrUpvxhhCk%2FIbnAYxixu4OyNc2I96CEUKMI%2FfwRPbx7uU3F%2BNKsNCNcTSWv51AI6Xs64Z%2F38G%2FmjSgw" target="_blank">Run Example</a>

```java
letters = "abcdefg"
closure = ( Any inp ) => {
	return char( ascii( inp ) + 7 )
}
writeOutput( StringMap( letters, closure ) )

```

Result: hijklmn

### Use the index of each character

```java
writeOutput( stringMap( "abc", ( ch, i ) -> ch & i ) )

```

Result: a1b2c3

### Uppercase every other character

```java
writeOutput( stringMap( "abcdef", ( ch, i ) => i % 2 == 0 ? uCase( ch ) : ch ) )

```

Result: aBcDeF
