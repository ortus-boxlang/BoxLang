### Translate struct keys using a mapping

```java
data = { first_name: "Luis", last_name: "Majano" };
mapping = { first_name: "firstName", last_name: "lastName" };
result = structKeyTranslate( data, mapping );
writeOutput( result.containsKey( "firstName" ) );

```

Result: true

### Using the member function

```java
data = { usr_name: "admin" };
result = data.keyTranslate( { usr_name: "username" } );
writeOutput( result.username );

```

Result: admin
