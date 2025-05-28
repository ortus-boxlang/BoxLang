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
	q = queryNew( "id,name" );
	q.addRow();
	q.setCell( "id", 1, 1 );
	q.setCell( "name", "one", 1 );
	writeDump( q );
</bx:script>

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrVLBVKCxNLar0Sy3XUFDKTNHJS8xNVVLQtOYKBAk7pqQE5QNlCuEiwaklzqk5OUAhHZB6JR0FQyDCLg02C0jn54EosKKU0twCqHEAHzIjhw%3D%3D" target="_blank">Run Example</a>

```java
q = queryNew( "id,name" );
QueryAddRow( q );
QuerySetCell( q, "id", 1, 1 );
QuerySetCell( q, "name", "one", 1 );
dump( q );

```


