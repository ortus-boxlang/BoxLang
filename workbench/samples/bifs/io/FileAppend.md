### Simple Example

Appends a mock entry to a file.


```java
// Create mock log entry
logEntry = dateTimeFormat( now(), "yyyy/mm/dd HH:nn" ) & " this is a mock log entry!";
// Append line to file
fileAppend( "/path/to/file.log", logEntry );

```


### Additional Examples


```java
fileAppend( "path/to/file", "new content to append" );

```


