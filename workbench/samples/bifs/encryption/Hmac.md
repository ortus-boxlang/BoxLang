### Example HMAC Using HMACSHA256



<a href="https://try.boxlang.io/?code=eJzLyE1M1lBQyi1OV9JRUCpOTS5KLQGxPHwdnYM9HI1MzZQUNK25AOHlCjM%3D" target="_blank">Run Example</a>

```java
hmac( "msg", "secret", "HMACSHA256" );

```

Result: fe4f9c418f683f034f6af90d1dd5b86ac0355dd96332c59cc74598d0736107f6

### Additional Examples

<a href="https://try.boxlang.io/?code=eJytkctqAjEUhvfzFAdXDljM5DJJkC5yG9x01foAg6Y6dC52JlJ8%2B8betKBSipBN%2FpN8%2BT%2FS%2BGEo1x7uYRQ21QBxlRD8EEaz5MXvD7nSJsMk7qdTWAxVu4aw8dD7113V%2BxU8V75eDdC19T7p%2FbCrQ7w0f1BmDM0nfAIHUjpL3voqeLtrtmP4OpnOIFI5zzlHlCAjCedC2byQmaZEUUWQzuXvl7ttqLq2rKGs110kbhrYln3Z%2BOB7GM%2Bbcvk4V1maHMdR4juOGldKTk6YV%2FoiKh0jWjlCpKEKMSa45VprhQXjjAonCkMw%2F0dvzPLzzePgNt0ZIpLKHGFnHSmKPDOCFtQ4lhkpnVNYxkipjFLlHGO44DbDKP6JypHl9oKTb5fd6pCeKC2eijuRJj%2BjKPMR%2FdljcsReMEpuovQOuK%2FlKw%3D%3D" target="_blank">Run Example</a>

```java
message = "this is a test";
key = "ABC123";
// Using the required fields only
result = HMAC( message, key );
writeDump( result ); // 776770430C93778AD6F91B43A4A30B69
// Using the optional algorithm parameter (HmacSHA1)
algorithm = "HmacSHA1";
result = HMAC( message, key, algorithm );
writeDump( result ); // 049E53BAE339C4A05587D7BBBA2857548E8FC327
// Using the optional algorithm parameter (HmacSHA256)
algorithm = "HmacSHA256";
result = HMAC( message, key, algorithm );
writeDump( result ); // 0503949602EDE3FF61C84F4CE51C99EEA2961CAA144AEE552F7D120AD6A60D7D
// Using the optional encoding parameter (UTF-8)
encoding = "UTF-8";
result = HMAC( message, key, algorithm, encoding );
writeDump( result );
 // 0503949602EDE3FF61C84F4CE51C99EEA2961CAA144AEE552F7D120AD6A60D7D

```


