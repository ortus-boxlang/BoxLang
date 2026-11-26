### Example HMAC Using HMACSHA256



<a href="https://try.boxlang.io/?code=eJzLyE1M1lBQyi1OV9JRUCpOTS5KLQGxPHwdnYM9HI1MzZQUNAHNiQnu" target="_blank">Run Example</a>

```java
hmac( "msg", "secret", "HMACSHA256" )

```

Result: fe4f9c418f683f034f6af90d1dd5b86ac0355dd96332c59cc74598d0736107f6

### Additional Examples

<a href="https://try.boxlang.io/?code=eJytkclqwzAURff%2BiodXMaREoyVRupAlmWy6avMBJlETUw%2BprVDy91U6JYWklBLQ6r6no3tQ68exWnu4gzRs6hHiqSD4MaTJs98fYl0YTGiazGawGOtuDWHjYfAvu3rwK3iqfbMaoe%2BafTL4cdeEeGd%2Br80E2g%2F0FA6gLHkd6uDtrt1O4HMxu4UIFSIXAjGKjKJCSG3zUuGCUc00RUWufj7cb0Pdd1UDVbPuI3HTwrYaqtYHP8Bk3lbLh7nGWXIcR4WvOP2t4vQEebktYspxWmhHqTJMI86lsKIoCk0kF5xJJ0tDifhHa8Lz873j4BrNOaKKqRwRZx0tyxwbyUpmHMdGKec0UTHSGjOmneOclMJiguJ%2F6BxZYS8Y%2BW7Zrw7pidDisbyRWfI9iirv0V8tpkfqWZ%2FkGj5v3Eni0w%3D%3D" target="_blank">Run Example</a>

```java
message = "this is a test"
key = "ABC123"
// Using the required fields only
result = HMAC( message, key )
writeDump( result ); // 776770430C93778AD6F91B43A4A30B69
// Using the optional algorithm parameter (HmacSHA1)
algorithm = "HmacSHA1"
result = HMAC( message, key, algorithm )
writeDump( result ); // 049E53BAE339C4A05587D7BBBA2857548E8FC327
// Using the optional algorithm parameter (HmacSHA256)
algorithm = "HmacSHA256"
result = HMAC( message, key, algorithm )
writeDump( result ); // 0503949602EDE3FF61C84F4CE51C99EEA2961CAA144AEE552F7D120AD6A60D7D
// Using the optional encoding parameter (UTF-8)
encoding = "UTF-8"
result = HMAC( message, key, algorithm, encoding )
writeDump( result )
 // 0503949602EDE3FF61C84F4CE51C99EEA2961CAA144AEE552F7D120AD6A60D7D

```



### Default algorithm (HmacMD5)

```java
writeOutput( hmac( "msg", "secret" ) )
```

Result: 6de3b563fd112a668aef22e8d2e08b00

### Using the member function

```java
writeOutput( "msg".hmac( "secret", "HMACSHA256" ) )
```

Result: fe4f9c418f683f034f6af90d1dd5b86ac0355dd96332c59cc74598d0736107f6

### Keys must be strings or binary

```java
try {
	hmac( "msg", 12345 )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Invalid key supplied to function [hmac]. Key must be a binary object or string
