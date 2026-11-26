### Paths that match no mapping are returned as they are

```java
writeOutput( contractPath( "/opt/unmapped/report.txt" ) )
```

Result: /opt/unmapped/report.txt

### Contract a path using an application mapping

Given a mapping of `/assets` to `/var/www/assets`, for example in `Application.bx` with `this.mappings[ "/assets" ] = "/var/www/assets"`, an absolute
path inside that folder is contracted to the mapping name.

```java
writeOutput( contractPath( "/var/www/assets/img/logo.png" ) )
```

Result: /assets/img/logo.png
