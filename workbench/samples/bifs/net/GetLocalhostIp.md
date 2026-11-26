### Get the localhost IP address

```java
ip = getLocalhostIp()
writeOutput( isSimpleValue( ip ) )

```

Result: true

### Get all local IP addresses

```java
ips = getLocalhostIp( true )
writeOutput( isArray( ips ) && ips.len() > 0 )

```

Result: true

### Refresh the cached list of addresses

```java
ips = getLocalhostIp( all: true, refresh: true )
writeOutput( isArray( ips ) )

```

Result: true
