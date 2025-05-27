### Basic Example




```java
<bx:storedproc procedure="foo_proc" dataSource="MY_SYBASE_TEST" username="sa" password="mygoodpw" dbServer="scup" dbName="pubs2" returnCode="Yes" debug="Yes">
<bx:procresult name="RS1"> 
<bx:procresult name="RS3" resultSet="3"> 
<bx:procparam type="IN" sqltype="CF_SQL_INTEGER" value="1" dbVarName="@param1"> 
<bx:procparam type="OUT" sqltype="CF_SQL_DATE" variable="FOO" dbVarName="@param2">
</bx:storedproc>
```

Result: 

