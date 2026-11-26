### Create and populate a query and output the 'name' column as a list if it is a valid query



<a href="https://try.boxlang.io/?code=eJzLS8xNLVawVSgsTS2q9Est11BQygMKKekoKJUlFiVnJBYpKWhygWUdU1KC8oEK8sBaoILBqSXOqTk5UFEdhG6gRAaZWr1S80A6M9M0FDKLA0FK4TqBsJqLs7wosyTVv7SkoLREA%2BJy5%2Fyc0tw8l8SSRDTjFDT1SvJ9MotLNIB6uWoBbPpM9Q%3D%3D" target="_blank">Run Example</a>

```java
names = queryNew( "name", "varchar" )
queryAddRow( names )
querySetCell( names, "name", "Seth" )
queryAddRow( names )
querySetCell( names, "name", "Jen" )
if( isQuery( names ) ) {
	writeOutput( queryColumnData( names, "name" ).toList() )
}

```

Result: Seth,Jen


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx9zTEOgzAMBdA9p%2FjKFJaiHqA7LJXaoZ0jMBKSS1rbEertG6ADE5P%2Ft6VnI7VHFFzgG2JOeCbh3ru6xp0sy6QYIishKkbDqJiSIeKTSb4n1%2BfXO6DV21ID7K9VqNyS13Wx13mlOcB3ic%2B%2BnHcPTPLOP7I3r%2Bg%2FthA8JQ%3D%3D" target="_blank">Run Example</a>

```java
testVar = "Hello World"
// Returns false as it is not a query.
dump( IsQuery( testVar ) )
testQuery = QueryNew( "col1" )
// Returns true as it is a query.
dump( IsQuery( testQuery ) )

```



### Structs and arrays are not queries

```java
writeOutput( isQuery( { name: "Seth" } ) & " " & isQuery( [ "Seth" ] ) & " " & isQuery( queryNew( "name" ) ) )
```

Result: false false true
