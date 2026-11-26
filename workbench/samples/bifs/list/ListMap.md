### Script Syntax



<a href="https://try.boxlang.io/?code=eJxdT8FKw0AQPe9%2BxdBcEgj9gJYKEfdQTCukoRXEw4iDXRI3YZpkFem%2FO2trGz0sM%2FPmzXtvC7TupfGwgMluT9ykcI%2BMFUpt%2FB5tmCtkW4XOVg2je5O2RI%2BdvBRW6MPdZK7poyN2WOf20AU9gZgG4gMVF5NaditsY%2BATlEIMmfuEIf0p9lRqSGBxA19aDcjgyG%2Bx7iloRjaaRcP0rBsnkZiof8bjcRoMs7Yl9xpfhRI5Yup6dhdsro8B9mw7uuvfJeGTVhJAFdlyffuwg9lvZK2O6XlltqbYmBHjz3evRPNYmmKd5flyUwptHFBI%2Bjk4fwPtEnrw" target="_blank">Run Example</a>

```java
Rainbow = "Whero, Karaka, Kowhai, Kakariki, Kikorangi, Tawatawa, Mawhero"
externalList = ""
reverseRainbow = listMap( rainbow, ( Any v, Any i, Any l ) => {
	var newValue = "#i#:#v.reverse()#"
	externalList = externalList.listAppend( newValue )
	return newValue
} )
writeDump( [
	{
		RAINBOW : rainbow
	},
	{
		REVERSERAINBOW : reverseRainbow
	},
	{
		EXTERNALLIST : externalList
	}
] )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxljVsKwjAQRb%2FNKi75kBayg1LBBQhuYaBTCYwxTBIfiHtv2og%2F%2Fh2453JmLT4njLAUo7CLTOpuSuHCdjAP9Zmnco0d5ib2g2l0lqIk9Sg%2B5RP9DIcOx%2FDCnaSw29CHiZ8NVxk9xgPeZqeci4ZmYg%2BbavGzFv6y31idFrkIOmQ%3D" target="_blank">Run Example</a>

```java
fruits = "apple,pear,orange"
writedump( fruits )
fruitsPlural = listMap( fruits, ( Any value, Any index, Any list ) => {
	return value & "s"
} )
writedump( fruitsPlural )

```

### Transform each item

```java
writeOutput( listMap( "a,b,c", ( value ) -> value.ucase() ) )

```

Result: A,B,C

### Use the index

```java
writeOutput( listMap( "a,b,c", ( value, index ) -> index & ":" & value ) )

```

Result: 1:a,2:b,3:c

### Member function

```java
writeOutput( "1,2,3".listMap( ( value ) -> value * 2 ) )

```

Result: 2,4,6
