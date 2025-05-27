### Tag Syntax

Basic example of calling a stored procedure, passing a parameter, and getting a result set.


```java
<bx:storedproc procedure="spu_my_storedproc" datasource="myDSN">
	<bx:procparam sqltype="cf_sql_integer" value="#myParameterValue#">
	<bx:procresult name="qResults">
</bx:storedproc>
```

Result: 

### Script Syntax

Call stored procedure and get back multiple result sets.


```java
bx:storedproc procedure="spu_my_storedproc" datasource="myDSN" {
	bx:procparam sqltype="cf_sql_date" value=myDateParam;
	bx:procresult name="qSummary" resultset=1;
	bx:procresult name="qDetails" resultset=2;
}

```

Result: 

### Scripted Tag Syntax (for Lucee)

Call stored procedure and get back multiple result sets.


```java
bx:storedproc procedure="spu_my_storedproc" datasource="myDSN" {
	procparam( cfsqltype="cf_sql_date", value=myDateParam );
	procresult( name="qSummary", resultset=1 );
	procresult( name="qDetails", resultset=2 );
}

```

Result: 

