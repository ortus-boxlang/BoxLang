### Full function

Are any of the characters in the string greater than our condition?

<a href="https://try.boxlang.io/?code=eJw9jUEKgCAUBdd5iocrhW4gBZ2hE5hZSPUL%2BxIR3T0paDsMM4F2joFGVJC2c70fRmmEs%2FPcWTdlqtDQiUAbNKoalyii5xTpRTXkkPXbiCMG9mviLbFC%2BybbdfEqa9%2BgxB%2FV0EY8N6wmrw%3D%3D" target="_blank">Run Example</a>

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
