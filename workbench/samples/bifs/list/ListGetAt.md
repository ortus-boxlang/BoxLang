### Simple Example

Returns the 2nd element in the list

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnFPLXEEMpXS8vN1khKLdHLyi1JzdTILiktzlXQUjBQ0FTQB%2BggRfA%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listGetAt( "foo,bar,lorem,ipsum", 2 ) )

```

Result: bar

### Example with Delimiter

Returns the 3rd element in the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnFPLXEEMpXS8vN1khKLanLyi1JzdTILiktza3JTa4rzc1NLMjLz0pV0FIx1FJRqlBQ0FTQBpf0YcQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listGetAt( "foo,bar|lorem,ipsum|me|something", 3, "|" ) )

```

Result: me

### Example with IncludeEmptyValues

Returns the 4th element in the list, treating the empty element as a value

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnFPLXEEMpXS8vN1khKLdHRy8otSc3UyC4pLc5V0FEx0FJR0gHRJUWmqgqaCJgDdSxRy" target="_blank">Run Example</a>

```java
writeOutput( listGetAt( "foo,bar,,lorem,ipsum", 4, ",", true ) )

```

Result: lorem

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxNzk0KwjAQBeB9TvHIpi0EZyGuxIVgdaMI3qBJBgz0j3RCPb6JuOhqZuAx31tjEH4mmZPU6MMiN5ZzXnVnjDXOaIO9gS5TYmI0aI4gwoslxXGBVfl48GA54ppGJ2EasQZ5o6IKnvswZCCqReI9f8cJmjoiS47Ia7UW%2FpKGucY%2FsduUOGSZNrIqcvuZ2Ql7TL%2FWcF%2FipDvw" target="_blank">Run Example</a>

```java
writeOutput( listGetAt( "a,,b,c,", 3, ",", true ) ); // Returns b
// Member Function with '/' delimiter
strList = "/a//b/c//d"
writeDump( strList.listGetAt( 5, "/", true ) )
 // Expected output c

```

### Position out of bounds

```java
try {
	listGetAt( "a,b,c", 5 )
} catch ( any e ) {
	writeOutput( e.message )
}

```

Result: Index [5] out of bounds for list with 3 elements.
