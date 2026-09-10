### Get the position of an XML child element

```java
xml = xmlParse( "<root><a/><b/><c/></root>" );
result = xmlChildPos( xml.xmlRoot, "b" );
writeOutput( result );

```

Result: 2
