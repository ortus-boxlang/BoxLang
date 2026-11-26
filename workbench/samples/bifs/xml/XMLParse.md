### Parse XML read from a file

Read XML from a file and use the xmlParse method to parse it into an XML data structure.


```java
<bx:file action="read" file="#tempxml#" variable="xmlString">
<bx:set myXML = xmlParse( xmlString ) >
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjzEOwjAMRfeewupUFnKBEAkmRsTEhlLyaSPFDTip1N6eUBRhL89Pf%2FheONxTFlimA7UNldFTzDAbliNHcwz%2BAa0KVfmUyOYUe602qnqEdX4azBXsJwfRqpqa6KNbzRkC8onyCGKkZAfQGmcSvGekDLfXagv%2B6qjap23czK%2BObhwuVhI6yuK5o%2BX%2Fw%2B67HwdnQAo%3D" target="_blank">Run Example</a>

```java
xml_stream = "
    <note>
      <to>Alice</to>
      <from>Bob</from>
      <heading>Reminder</heading>
      <body>Here is the message you requested.</body>
    </note>
  "
dump( XmlParse( trim( xml_stream ) ) )

```


### Parse a string and read an element

```java
xml = xmlParse( "<a><b>1</b></a>" )
writeOutput( xml.xmlRoot.xmlName & " " & xml.xmlRoot.b.xmlText )
```

Result: a 1

### Parse case-insensitively

```java
xml = xmlParse( "<A><B>1</B></A>", false )
writeOutput( xml.a.b.xmlText )
```

Result: 1

### Malformed XML throws an error

```java
try {
	xmlParse( "<a><b>1</b>" )
} catch( any e ) {
	writeOutput( e.message )
}
```

Result: Error parsing XML document
