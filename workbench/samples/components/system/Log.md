### Write an informational log entry

```java
<bx:log text="Application started successfully" type="Information">

```

### Write a warning to the log

```java
<bx:log text="Cache miss rate exceeding threshold" type="Warning">

```

### Write an error log entry

```java
<bx:log text="Database connection failed" type="Error">

```

### Log to a custom logger/file

```java
<bx:log text="Payment processed" log="payments" type="Information">

```

### Log with application name disabled

```java
<bx:log text="Debug trace" application="false" type="Debug">

```
