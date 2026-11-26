### Boxlang polyfill

Using java data type null instead

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyCz2K83J0VDIA5JhiTmlqRqaCkAIAP0FDF8%3D" target="_blank">Run Example</a>

```java
writeOutput( isNull( nullValue() ) )

```

Result: true

### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLrYzPK83JUbBV8ANSYYk5pakamlwppbkFGgqZxSAxDYVcqBpNBU0AissPwA%3D%3D" target="_blank">Run Example</a>

```java
my_null = NullValue()
dump( isNull( my_null ) )

```


### Check that the value is null

```java
writeOutput( isNull( nullValue() ) )
```

Result: true

### Assign a null to a variable

```java
x = nullValue()
writeOutput( isNull( x ) )
```

Result: true
