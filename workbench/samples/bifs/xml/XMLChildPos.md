### Get the position of an XML child element

```java
xml = xmlParse( "<root><a/><b/><c/></root>" )
result = xmlChildPos( xml.xmlRoot, "b", 1 )
writeOutput( result )

```

Result: 2

### Find the Nth child with a given name

The second b element is the third child of root.

```java
xml = xmlParse( "<root><a/><b id='1'/><b id='2'/><c>text</c></root>" )
writeOutput( xmlChildPos( xml.xmlRoot, "b", 2 ) )
```

Result: 3

### Use the member function

```java
xml = xmlParse( "<root><a/><b id='1'/><b id='2'/><c>text</c></root>" )
writeOutput( xml.xmlRoot.childPos( "a", 1 ) )
```

Result: 1

### Returns -1 when there is no such child

```java
xml = xmlParse( "<root><a/><b id='1'/><b id='2'/><c>text</c></root>" )
writeOutput( xmlChildPos( xml.xmlRoot, "b", 3 ) )
```

Result: -1
