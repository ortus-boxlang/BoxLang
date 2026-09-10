### Add elements using the add member function

```java
s = setNew();
s.add( "apple" );
s.add( "banana" );
s.add( "apple" );
writeOutput( s.size() );

```

Result: 2

### Using the append alias

`append` is an alias for `add`.

```java
s = setNew();
s.append( "red" );
s.append( "green" );
s.append( "red" );
writeOutput( s.size() );

```

Result: 2
