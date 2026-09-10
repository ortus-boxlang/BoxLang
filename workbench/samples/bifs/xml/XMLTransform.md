### Transform XML using an XSLT stylesheet

```java
xml = xmlParse( "<root><item>test</item></root>" );
xslt = '<?xml version="1.0"?><xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"><xsl:template match="/"><output><xsl:value-of select="/root/item"/></output></xsl:template></xsl:stylesheet>';
result = xmlTransform( xml, xslt );
writeOutput( isString( result ) );

```

Result: true
