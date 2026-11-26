### Full function

Are any of the characters in the string greater than our condition?

<a href="https://try.boxlang.io/?code=eJw9zEEKgCAURdFxruLhSKGWkNAaWoGZhWS%2FsC8R0d6LgqaXywm0cQo0ooa0nev9MErhbIydddMTFRo6EGiFRmVwiiJ5zoneZCAHKS6xp8B%2BybxmVmhfr11mr57p00v8pIa%2BAYfOJak%3D" target="_blank">Run Example</a>

```java
instring = "abcdefg"
callback = ( Any inp ) -> {
	return inp > "f"
}
writeoutput( StringSome( instring, callback ) )

```

Result: true

### Member function

```java
instring = "abcdefg"
callback = ( inp ) -> inp > "f"
writeOutput( instring.stringSome( callback ) )

```

Result: true

### No character matches

```java
writeOutput( stringSome( "boxlang", ( ch ) -> ch >= "z" ) )

```

Result: false
