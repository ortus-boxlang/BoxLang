### Get metadata for a class by name

```java
class Person {
    property name="firstName";
    function init() { return this; }
}
meta = getClassMetadata( "Person" );
writeOutput( meta.name );

```

Result: Person

### Get metadata for a Java class

```java
meta = getClassMetadata( "java.util.HashMap" );
writeOutput( meta.type );

```

Result: Class
