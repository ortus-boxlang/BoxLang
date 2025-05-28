### Check if Date is instance of java.util.Date

Dates in BL are instances of the java class: `java.util.Date`

<a href="https://try.boxlang.io/?code=eJzLLPbMKy5JzEtO9U%2FTUMjLL9fQ1FFQykosS9QrLcnM0XNJLElVUtC05gIAKTkNDA%3D%3D" target="_blank">Run Example</a>

```java
isInstanceOf( now(), "java.util.Date" );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io?code=eJwrL8osSXUpzS3QUMgs9swrLknMS071T9NQqK7VUVDKSixL1CstyczR800sUFLQVNC0VtDXVygpKk3lKselU8kpvyInMS9dCbcBaYk5xaSYAOLoBZcUZQLFwIZwwZ0BABvAPcs%3D" target="_blank">Run Example</a>

```java
writeDump( isInstanceOf( {}, "java.util.Map" ) ); // true
writeDump( isInstanceOf( "Boxlang", "java.util.Map" ) ); // false
writeDump( isInstanceOf( "Boxlang", "java.lang.String" ) );
 // true

```


