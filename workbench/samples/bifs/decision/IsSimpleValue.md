### A number is a simple value



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VAwMVLQtOYCAFTKBoA%3D" target="_blank">Run Example</a>

```java
isSimpleValue( 42 );

```

Result: true

### A string is a simple value as well



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VBQ8kjNyclXKM8vyklRUtC05gIAwFQKmg%3D%3D" target="_blank">Run Example</a>

```java
isSimpleValue( "Hello world" );

```

Result: true

### A structure is a complex value

So it can't a be simple value

<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VAoLikqTS7xSy3X0FTQtOYCALCMCjo%3D" target="_blank">Run Example</a>

```java
isSimpleValue( structNew() );

```

Result: false

### An array is a complex value



<a href="https://try.boxlang.io/?code=eJzLLA7OzC3ISQ1LzClN1VBILCpKrPRLLddQMFTQVNC05gIAwT8KJQ%3D%3D" target="_blank">Run Example</a>

```java
isSimpleValue( arrayNew( 1 ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyCwOzswtyEkNS8wpTdVQUCouKcrMS1dS0FRQU1CySSqyAzKtucrxaDE0MjYxNSNBQ3pqSUhmbmpVfl6qhiYJ%2BvLyy0lSn5VYlpicWAyUUErOSCxS0lFQcgR5jAQbS3NyIExSdEXHkqC4upYExYWlqUWVfqnlQB%2BVpBaXQDwD1AMAZpWTJw%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( isSimpleValue( "string" ) & "<br>" );
writeOutput( isSimpleValue( 123456 ) & "<br>" );
writeOutput( isSimpleValue( getTimezone() ) & "<br>" );
writeOutput( isSimpleValue( now() ) & "<br>" );
writeOutput( isSimpleValue( javacast( "char", "A" ) ) & "<br>" );
writeOutput( isSimpleValue( nullValue() ) & "<br>" );
writeOutput( isSimpleValue( [] ) & "<br>" );
writeOutput( isSimpleValue( {} ) & "<br>" );
writeOutput( isSimpleValue( queryNew( "test" ) ) );

```


