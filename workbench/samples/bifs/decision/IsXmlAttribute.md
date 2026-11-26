### Check an attribute node returned by xmlSearch()

```java
xml = xmlParse( "<root attr='value'/>" )
attributes = xmlSearch( xml, "/root/@attr" )
writeOutput( isXmlAttribute( attributes[ 1 ] ) )
```

Result: true

### Values read from the xmlAttributes struct are not attribute nodes

```java
xml = xmlParse( "<root attr='value'/>" )
writeOutput( isXmlAttribute( xml.xmlRoot.xmlAttributes.attr ) )
```

Result: false

### An element is not an attribute

```java
xml = xmlParse( "<root attr='value'/>" )
writeOutput( isXmlAttribute( xml.xmlRoot ) )
```

Result: false
