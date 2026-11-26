### Characters while they are in the set

```java
writeOutput( spanIncluding( "This is Great", "Thi" ) )

```

Result: Thi

### The span ends at the first character not in the set

```java
writeOutput( "[" & spanIncluding( "Plant green! save earth!", "Plant green!r" ) & "]" )

```

Result: [Plant green! ]

### Member function

```java
writeOutput( "AabByyysss".spanIncluding( "AabBz" ) )

```

Result: AabB
