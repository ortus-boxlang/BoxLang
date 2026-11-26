### Convert a string

Creates a JavaScript assignment statement for the variable name you pass.

```java
writeOutput( toScript( "This is a string", "jsVar" ) )
```

Result: jsVar = "This is a string";

### Convert a number and a boolean

```java
writeOutput( toScript( 42, "answer" ) & " " & toScript( true, "flag" ) )
```

Result: answer = 42; flag = true;

### Convert an array

```java
writeOutput( toScript( [ 1, 2, 3 ], "numbers" ) )
```

Result: numbers = [1,2,3];

### Convert a struct

```java
data = [ name: "Luis", age: 42 ]
writeOutput( toScript( data, "person" ) )
```

Result: person = {"name":"Luis","age":42};

### Convert a date

Dates become a JavaScript `Date` object.

```java
writeOutput( toScript( createDate( 2024, 1, 2 ), "start" ) )
```

Result: start = new Date('2024-01-02T00:00:00Z');

### Convert a query

Queries are serialized with `jsonSerialize()`, so they use the default query format, an array of structs.

```java
q = queryNew( "id", "integer", [ [ 1 ] ] )
writeOutput( toScript( q, "rows" ) )
```

Result: rows = [{"id":1}];

### Empty strings and null

```java
writeOutput( toScript( "", "empty" ) & " " & toScript( nullValue(), "nothing" ) )
```

Result: empty = ''; nothing = null;
