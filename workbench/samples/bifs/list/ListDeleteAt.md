### Simple Example

Deletes 2nd item from the list

<a href="https://try.boxlang.io/?code=eJzLySwucUnNSS1JdSzRUFBKy8%2FXSUos0snJL0rN1cksKC7NVdJRMFLQtOYCAEGJDac%3D" target="_blank">Run Example</a>

```java
listDeleteAt( "foo,bar,lorem,ipsum", 2 );

```

Result: foo,lorem,ipsum

### Example with Custom Delimiter

Deletes 2nd item from the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwucUnNSS1JdSzRUFBKy8%2FXSUosqsnJL0rN1cksKC7NrclNrSnOz00tycjMS1fSUTDSUVCqUVLQtOYCAKizFJs%3D" target="_blank">Run Example</a>

```java
listDeleteAt( "foo,bar|lorem,ipsum|me|something", 2, "|" );

```

Result: foo,bar|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjsEKgkAQhu%2F7FIMnhSEPdZMOgQlC0aUXqO0PF1Zd3Nns8VutICGYyzD%2FfP83DkZwCuKCpGSNlxIWgl3ckppt%2FwDboAEWeKmMRcK0oYyygvKc9k8HLbhRPxPo90PF%2BxHtFQNVodNi%2Bk55GQ6xg7aUnBvjOc6FpXnT%2BT7hCzVOSmVoXUqf%2FGoptp771T%2BBL3XBVC%2FKX00I" target="_blank">Run Example</a>

```java
writeOutput( listDeleteAt( "I,love,boxlang,testFile", 4 ) ); // Expected output I,love,boxlang
// Member Function
strList = "This,is,a,the,test,file";
writeDump( strList.listDeleteAt( 3 ) );
 // Expected output This,is,the,test,file

```


