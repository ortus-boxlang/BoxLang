### Expand dot-notated keys

The struct is modified in place and also returned.

```java
data = { "user.name" : "Luis", "user.age" : 30 }
structKeyTranslate( data )
writeOutput( data.user.name & " " & data.user.age & " " & structKeyExists( data, "user.name" ) )
```

Result: Luis 30 false

### Retain the original keys using the member function

```java
data = { "user.name" : "Luis", "user.age" : 30 }
data.keyTranslate( false, true )
writeOutput( data.user.name & " " & structKeyExists( data, "user.name" ) )
```

Result: Luis true
