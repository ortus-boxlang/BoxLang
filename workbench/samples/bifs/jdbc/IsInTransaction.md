### Check if within a database transaction

```java
writeOutput( isBoolean( isInTransaction() ) )

```

Result: true

### False outside a transaction

```java
writeOutput( isInTransaction() )
```

Result: false

### True inside a transaction block

```java
transaction {
	writeOutput( isInTransaction() )
}
```

Result: true

### Using the isWithinTransaction alias

```java
transaction {
	writeOutput( isWithinTransaction() )
}
writeOutput( " " & isWithinTransaction() )
```

Result: true false
