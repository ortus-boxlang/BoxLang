### Base64 value to binary to string

```java
base64Value = toBase64( "stringValue" )
binaryValue = toBinary( base64Value )
stringValue = toString( binaryValue )
writeOutput( stringValue )
```

Result: stringValue

### Convert a number to a string

```java
writeOutput( toString( 12.50 ) )
```

Result: 12.5

### Member syntax

```java
num = 42
writeOutput( num.toString() )
```

Result: 42

### Convert binary data using an encoding

```java
bytes = charsetDecode( "Hello", "utf-8" )
writeOutput( toString( bytes, "utf-8" ) )
```

Result: Hello

### Null becomes an empty string

```java
writeOutput( "[" & toString( nullValue() ) & "]" )
```

Result: []

### Convert a date

```java
writeOutput( toString( createDate( 2024, 1, 2 ) ) )
```

Result: {ts '2024-01-02 00:00:00'}
