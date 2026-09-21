### Collect a stream of Map.Entry into a Struct

Converts a stream of key-value pairs into a BoxLang Struct.

```java
entries = [ {key:"a", value:1}, {key:"b", value:2} ]
    .map( (e) => entry( e.key, e.value ) )
    .toStream();
s = entries.toBXStruct();
writeOutput( s.toString() );

```

Result: {a=1, b=2}

### Create an ordered struct from a stream

```java
entries = [ {key:"z", value:26}, {key:"a", value:1} ]
    .map( (e) => entry( e.key, e.value ) )
    .toStream();
s = entries.toBXStruct( type="ordered" );
writeOutput( s.toString() );

```

Result: {z=26, a=1}

### Map keys and values then collect to struct

```java
s = [ "apple", "banana" ]
    .toStream()
    .map( (fruit) => entry( fruit.len(), fruit ) )
    .toBXStruct();
writeOutput( s.toString() );

```

Result: {5=apple, 6=banana}
