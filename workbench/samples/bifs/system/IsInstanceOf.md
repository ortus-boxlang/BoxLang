### Check if Date is instance of java.util.Date

BoxLang dates are DateTime values, not instances of `java.util.Date`.

<a href="https://try.boxlang.io/?code=eJzLLPbMKy5JzEtO9U%2FTUMjLL9fQ1FFQykosS9QrLcnM0XNJLElVUtC05gIAKTkNDA%3D%3D" target="_blank">Run Example</a>

```java
isInstanceOf( now(), "java.util.Date" )

```

Result: false

### Check against Java types

```java
writeOutput( isInstanceOf( {}, "java.util.Map" ) & " " & isInstanceOf( "BoxLang", "java.util.Map" ) & " " & isInstanceOf( "BoxLang", "java.lang.String" ) )
```

Result: true false true
