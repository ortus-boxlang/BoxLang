### isStruct Example

Returns true if variable is a Boxlang structure or is a Java object that implements the java.lang.Map interface. 

<a href="https://try.boxlang.io/?code=eJzLLA4uKSpNLtFQKAbTfqnlGpoKmtZcAHq5CFg%3D" target="_blank">Run Example</a>

```java
isStruct( structNew() );

```

Result: true

### isStruct Example for False

Returns false is the object in the variable parameter is a user-defined function UDF).  In the example below exponent is a function created by the user

<a href="https://try.boxlang.io/?code=eJzLLA4uKSpNLtFQSCwqSqz0Sy3XUDBU0FTQtOYCAIeoCEM%3D" target="_blank">Run Example</a>

```java
isStruct( arrayNew( 1 ) );

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLilSsFUoLikqTS7xSy3X0LTmKi%2FKLEn1Ly0pKC3RUMgsDgbLaYDUKGgqAOUB1fIRGQ%3D%3D" target="_blank">Run Example</a>

```java
str = structNew();
writeOutput( isStruct( str ) );

```


