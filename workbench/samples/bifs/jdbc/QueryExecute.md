### Simple Example

SQL Only Example. Assumes that a default datasource has been specified (by setting the variable this.datasource in Application.bx)

```java
qryResult = queryExecute("SELECT * FROM Employees");
```

### Passing Query Parameters using Struct

Use `:structKeyName` in your sql then pass a struct with corresponding key names.

```java
qryResult = queryExecute(
  "SELECT * FROM Employees WHERE empid = :empid AND country = :country", 
  {
    country="USA", 
    empid=1
  }
);
```

### Passing Query Parameters using Array

When passing with an array use the `?` symbol as a placeholder in your sql

```java
qryResult = queryExecute(
  "SELECT * FROM Employees WHERE empid = ? AND country = ?", 
  [
    1,
    "USA"
  ]
);
```

