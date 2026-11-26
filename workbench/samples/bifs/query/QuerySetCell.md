### Tag Example

 


```java
<!--- start by making a query ---> 
 <bx:query name="GetCourses" datasource="cfdocexamples"> 
 SELECT Course_ID, Descript 
 FROM Courses 
 </bx:query> 
<bx:set temp = queryAddRow( GetCourses ) > 
<bx:set Temp = querySetCell( GetCourses, "Number", 100 * CountVar ) > 
```


### Script member function example




```java
<bx:script>
	q = queryNew( "id,name" )
	q.addRow()
	q.setCell( "id", 1, 1 )
	q.setCell( "name", "one", 1 )
	writeDump( q )
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVVLQ5AoEiTqmpATlAyUKYQLBqSXOqTk5QBEdkGolHQVDIMIqCzYISOfngSiQmpTS3AKwWQBYPSJW" target="_blank">Run Example</a>

```java
q = queryNew( "id,name" )
QueryAddRow( q )
QuerySetCell( q, "id", 1, 1 )
QuerySetCell( q, "name", "one", 1 )
dump( q )

```

### Set a cell in a specific row

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
querySetCell( q, "name", "Zed", 2 )
writeOutput( arrayToList( queryColumnData( q, "name" ) ) )

```

Result: Ann,Zed

### Member function defaults to the last row

```java
q = queryNew( "id,name", "integer,varchar", [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
q.setCell( "name", "Yan" )
writeOutput( arrayToList( q.columnData( "name" ) ) )

```

Result: Ann,Yan
