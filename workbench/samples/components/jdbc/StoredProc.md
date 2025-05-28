### Tag Syntax

Basic example of calling a stored procedure, passing a parameter, and getting a result set.


```java
<bx:storedproc procedure="spu_my_storedproc" datasource="myDSN">
	<bx:procparam sqltype="integer" value="#myParameterValue#">
	<bx:procresult name="qResults">
</bx:storedproc>
```


### Script Syntax

Call stored procedure and get back multiple result sets.


```java
bx:storedproc procedure="spu_my_storedproc" datasource="myDSN" {
	bx:procparam sqltype="date" value=myDateParam;
	bx:procresult name="qSummary" resultset=1;
	bx:procresult name="qDetails" resultset=2;
}

```


### Scripted Tag Syntax

Call stored procedure and get back multiple result sets.


```java
bx:storedproc procedure="spu_my_storedproc" datasource="myDSN" {
	procparam( sqltype="date", value=myDateParam );
	procresult( name="qSummary", resultset=1 );
	procresult( name="qDetails", resultset=2 );
}

```


