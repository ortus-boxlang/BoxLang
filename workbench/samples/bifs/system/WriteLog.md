### Write a log entry

Logs a message to orders.log in the BoxLang logs directory.

```java
writeLog( text = "Order 1234 processed successfully", type = "information", log = "orders" )
```

Result: the line is appended to the orders log file; no output

### Log an error to the default application log

```java
writeLog( text = "Payment failed", type = "error" )
```

Result: the message is written to application.log at the error level; no output
