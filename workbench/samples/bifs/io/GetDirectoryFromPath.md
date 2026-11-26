### Basic Example



<a href="https://try.boxlang.io/?code=eJxLTy1xySxKTS7JL6p0K8rPDUgsydBQUHK2iilJzS2IScvMSdUrqShRUtAEAFQoDwU%3D" target="_blank">Run Example</a>

```java
getDirectoryFromPath( "C:\temp\file.txt" )

```

Result: C:\temp\

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrzs9NjU%2FJLEpNLskvqlSwVUhPLXGBcd2K8nMDEksyNBSUCoCUkoImV0ppboGGQjGqLk0AmaUZNw%3D%3D" target="_blank">Run Example</a>

```java
some_directory = getDirectoryFromPath( "path" )
dump( some_directory )

```

### Get the directory of a file path

```java
writeOutput( getDirectoryFromPath( "/var/www/app/index.bxm" ) )
```

Result: /var/www/app/

### Paths ending with a separator are unchanged, and paths without one return a slash

```java
writeOutput( getDirectoryFromPath( "/var/www/app/" ) & " " & getDirectoryFromPath( "index.bxm" ) )
```

Result: /var/www/app/ /
