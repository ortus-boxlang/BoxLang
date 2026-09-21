### Invoke a method on an object

Calls a method dynamically and stores the result in a variable.

```java
<bx:invoke class="UserService" method="findById" returnVariable="user">
    <bx:invokeArgument name="id" value="42">
</bx:invoke>
<bx:dump var="#user#">

```

### Invoke with argumentCollection

Pass arguments as a struct instead of individual invokeArgument tags.

```java
args = { id: 42, includeDetails: true };
<bx:invoke class="UserService" method="findById" argumentCollection="#args#" returnVariable="user">

```

### Invoke a local function

When class is empty, invokes a function in the current template scope.

```java
<bx:invoke method="calculateTotal" returnVariable="total">
    <bx:invokeArgument name="items" value="#cart#">
</bx:invoke>

```

### Invoke a web service

```java
<bx:invoke
    webservice="https://api.example.com/service?wsdl"
    method="getData"
    returnVariable="result"
    username="user"
    password="secret"
    timeout="30"
>

```

### Invoke with inline arguments

Attributes on the invoke tag that aren't reserved become method arguments.

```java
<bx:invoke class="MathHelper" method="add" returnVariable="sum">
    a=10
    b=20
</bx:invoke>

```
