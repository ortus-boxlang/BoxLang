### Get metadata for a class by path

The path is resolved like any class path, so models/Person.bx can be requested as models.Person.

```java
meta = getClassMetadata( "models.Person" )
writeOutput( meta.name )
```

Result: the fully qualified class name, for example models.Person

### Get metadata from an instance

```java
person = new models.Person()
meta = getClassMetadata( person )
writeOutput( meta.type )
```

Result: Class
