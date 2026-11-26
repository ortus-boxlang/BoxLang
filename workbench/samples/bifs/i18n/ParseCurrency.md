### lsParseCurrency Example

LSParseCurrency converts a locale-specific currency string to a number.

<a href="https://try.boxlang.io/?code=eJzLKQ5ILCpOdS4tKkrNS67UUFBSMTQy0DM1UFLQtOYCAKArCIo%3D" target="_blank">Run Example</a>

```java
lsParseCurrency( "$120.50" )

```

Result: 120.5

### Additional Examples


```java
<bx:output>
	#LSParseCurrency( 4.5 )#<br>
	#LSParseCurrency( "$4.50" )#<br>
	#LSParseCurrency( "£4.50", "English (UK)" )#
</bx:output>
```



### Parse using a specific locale

```java
writeOutput( parseCurrency( "£4.50", "English (UK)" ) )
```

Result: 4.5

### Values which are not currency throw an error

```java
try {
	parseCurrency( "abc", "en_US" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: The value [abc] could not be parsed using the locale [English (United States)]
