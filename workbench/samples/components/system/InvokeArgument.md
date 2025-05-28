### Invoke a SOAP webservice and passing arguments using bx:invokeargument

Calls a remote web service to perform an addition, uses bx:invokeargument to pass the arguments to the method.


```java
<bx:invoke webservice="http://soaptest.parasoft.com/calculator.wsdl" method="add" returnvariable="answer">
    <bx:invokeargument name="x" value="2">
    <bx:invokeargument name="y" value="3">
</bx:invoke>
<bx:output>#answer#</bx:output>
```

Result: 5.0

