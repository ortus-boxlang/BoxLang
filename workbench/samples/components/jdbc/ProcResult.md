### Capture a stored procedure result set

Registers a variable to hold the result set returned by a stored procedure.

```java
<bx:storedproc procedure="getUsers" datasource="myDS">
    <bx:procResult name="users">
    <bx:dump var="#users#">
</bx:storedproc>

```

### Capture multiple result sets

When a stored procedure returns multiple result sets, use `resultSet` to specify which one.

```java
<bx:storedproc procedure="getReportData" datasource="myDS">
    <bx:procResult name="summary" resultSet="1">
    <bx:procResult name="details" resultSet="2">
</bx:storedproc>

```

### Limit rows in a result set

```java
<bx:storedproc procedure="getLargeDataset" datasource="myDS">
    <bx:procResult name="top100" maxRows="100">
</bx:storedproc>

```
