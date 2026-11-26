### Full function

Map each element of the string to a new value.

<a href="https://try.boxlang.io/?code=eJw1jTEKAjEQRWtzis9WE7S3CCt4ALHwBNk4rgMhhskEEfHuq6jtezxeZjPWhhFDnNKZL%2FMQXMq31pXfkLAvD0ip8Bh3eLqVsnUtSNeohNiSCP38Glv44F7B3VWMj91qN8LJVMp8iJWQv7MN%2FgP%2FCRae5Skm" target="_blank">Run Example</a>

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
