### Transform XML using an XSLT stylesheet

```java
xml = xmlParse( "<root><item>test</item></root>" )
xslt = '<?xml version="1.0"?><xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:template match="/"><output><xsl:value-of select="/root/item"/></output></xsl:template></xsl:stylesheet>'
result = xmlTransform( xml, xslt )
writeOutput( isSimpleValue( result ) )

```

Result: true

### Pass parameters to the stylesheet

The result is a string containing the markup shown below.

```java
xsl = '<?xml version="1.0"?><xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:output omit-xml-declaration="yes"/><xsl:param name="greeting"/><xsl:template match="/"><out><xsl:value-of select="$greeting"/>:<xsl:value-of select="/root/c"/></out></xsl:template></xsl:stylesheet>'
xml = xmlParse( "<root><c>text</c></root>" )
writeOutput( xmlTransform( xml, xsl, { greeting: "hi" } ) )
```

Result: `<out>hi:text</out>`

### Transform an XML string

```java
xsl = '<?xml version="1.0"?><xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:output omit-xml-declaration="yes"/><xsl:param name="greeting"/><xsl:template match="/"><out><xsl:value-of select="$greeting"/>:<xsl:value-of select="/root/c"/></out></xsl:template></xsl:stylesheet>'
writeOutput( xmlTransform( "<root><c>s</c></root>", xsl ) )
```

Result: `<out>:s</out>`

### An invalid stylesheet throws an error

```java
try {
	xmlTransform( "<root/>", "<notxsl/>" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Error transforming XML
