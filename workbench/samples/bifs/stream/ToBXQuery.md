### Collect a stream of structs into a Query

Requires a template query to define the column structure.

```java
template = queryNew( "id,name", "integer,varchar" )
data = [
    { id: 1, name: "Alice" },
    { id: 2, name: "Bob" }
]
q = data.stream().toBXQuery( template )
writeOutput( q.recordCount )

```

Result: 2

### Read the rows of the resulting Query

```java
template = queryNew( "name,title", "varchar,varchar" )
q = [
    { name: "Brad", title: "Developer" },
    { name: "Luis", title: "CEO" }
].stream().toBXQuery( template )
writeOutput( q.name[ 1 ] & " is a " & q.title[ 1 ] )

```

Result: Brad is a Developer

### An empty stream returns an empty Query

The columns come from the template, so the result still has the right structure.

```java
template = queryNew( "id,name", "integer,varchar" )
q = [].stream().toBXQuery( template )
writeOutput( q.recordCount & " " & q.columnList )

```

Result: 0 id,name
