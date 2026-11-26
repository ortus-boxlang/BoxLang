### Characters up to the first excluded character

```java
writeOutput( spanExcluding( "This is Great", "r" ) )

```

Result: This is G

### Several excluded characters

```java
writeOutput( "[" & spanExcluding( "Plant green! save earth!", "s" ) & "]" )

```

Result: [Plant green! ]

### Member function

```java
writeOutput( "AabByyysss".spanExcluding( "s" ) )

```

Result: AabByyy
