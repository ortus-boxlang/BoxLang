### Full function

Are any of the characters in the string greater than our condition?

<a href="https://try.boxlang.io/?code=eJw9jUEKgCAUBdd5iocrhW4gBZ2hE5hZSPUL%2BxIR3T0paDsMM4F2joFGVJC2c70fRmmEs%2FPcWTdlqtDQiUAbNKoalyii5xTpRTXkkPXbiCMG9mviLbFC%2BybbdfEqa9%2BgxB%2FV0EY8N6wmrw%3D%3D" target="_blank">Run Example</a>

```java
instring = "abcdefg";
callback = ( Any inp ) => {
	return inp > "f";
};
writeoutput( StringSome( instring, callback ) );

```

Result: YES

### Member function

Are any of the characters in the string greater than our condition?


```java
instring = "abcdefg";
callback = ( Any inp ) => {
	return inp > "f";
};
writeoutput( instring.some( callback ) );

```

Result: YES

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrQwuKcrMS1ewVVDKKU1OTVWy5kpOzMlJSkzOBoppKDjmVSpUKGgq2NoBKTugqkQla2uu8qLMklSX0twCDQWI%2FuD83FQNhVyoaToKcDM0FTQRJsYbYjeziiQzgaaATQUAZwM52Q%3D%3D" target="_blank">Run Example</a>

```java
myString = "lucee";
callback = ( Any x ) => x >= "a";;
writeDump( StringSome( myString, callback ) );
callback_1 = ( Any x ) => x >= "z";;
writeDump( StringSome( myString, callback_1 ) );

```


