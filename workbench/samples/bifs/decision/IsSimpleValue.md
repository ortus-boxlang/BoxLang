### A number is a simple value



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VAwMVLQBABH1AY7" target="_blank">Run Example</a>

```java
isSimpleValue( 42 )

```

Result: true

### A string is a simple value as well



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VBQ8kjNyclXKM8vyklRUtAEAKsqClU%3D" target="_blank">Run Example</a>

```java
isSimpleValue( "Hello world" )

```

Result: true

### A structure is a complex value

So it can't a be simple value

<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VAoLikqTS7xSy3X0FTQBACcIgn1" target="_blank">Run Example</a>

```java
isSimpleValue( structNew() )

```

Result: false

### An array is a complex value



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VBILCpKrPRLLddQMFTQVNAEAKz%2FCeA%3D" target="_blank">Run Example</a>

```java
isSimpleValue( arrayNew( 1 ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyCwOzswtyEkNS8wpTdVQUCouKcrMS1dS0FRQU1CySSqyAzK5yvHoMDQyNjE1I159empJSGZualV%2BXqqGJvHa8vLLSVGelViWmJxYDJRQSs5ILFLSUVByBHmKePtKc3IgTBI0RccSr7a6lni1haWpRZV%2BqeVA35SkFpdAPKIJAFHfkQo%3D" target="_blank">Run Example</a>

```java
writeOutput( isSimpleValue( "string" ) & "<br>" )
writeOutput( isSimpleValue( 123456 ) & "<br>" )
writeOutput( isSimpleValue( getTimezone() ) & "<br>" )
writeOutput( isSimpleValue( now() ) & "<br>" )
writeOutput( isSimpleValue( javacast( "char", "A" ) ) & "<br>" )
writeOutput( isSimpleValue( nullValue() ) & "<br>" )
writeOutput( isSimpleValue( [] ) & "<br>" )
writeOutput( isSimpleValue( {} ) & "<br>" )
writeOutput( isSimpleValue( queryNew( "test" ) ) )

```


