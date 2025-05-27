### Is 127.0.0.1 localhost



<a href="https://try.boxlang.io/?code=eJzLLPbJT07M8cgvLtFQUDI0MtczAEJDJQVNay4AfssHNQ%3D%3D" target="_blank">Run Example</a>

```java
isLocalHost( "127.0.0.1" );

```

Result: true

### Is ::1 localhost

Test the IPv6 Loopback address. IPv6 only has one loopback address.

<a href="https://try.boxlang.io/?code=eJzLLPbJT07M8cgvLtFQULKyMlRS0LTmAgBVSwYl" target="_blank">Run Example</a>

```java
isLocalHost( "::1" );

```

Result: true

### Is 127.8.8.8 localhost

IPv4 network standards reserve the entire 127.0.0.0/8 address block for loopback networking purposes however they are not usually mapped to `localhost` by default.

<a href="https://try.boxlang.io/?code=eJzLLPbJT07M8cgvLtFQUDI0MtezAEElBU1rLgB%2FhQdM" target="_blank">Run Example</a>

```java
isLocalHost( "127.8.8.8" );

```

Result: true

### Is 8.8.8.8 localhost

Not a localhost IP.

<a href="https://try.boxlang.io/?code=eJzLLPbJT07M8cgvLtFQULLQA0MlBU1rLgBwuQbq" target="_blank">Run Example</a>

```java
isLocalHost( "8.8.8.8" );

```

Result: false

### Additional Examples


```java
ip = "127.0.0.1";
writeDump( islocalhost( ip ) ); // true
writeDump( islocalhost( GetLocalHostIP() ) );
 // true

```


