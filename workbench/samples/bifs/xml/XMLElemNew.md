### Simple xmlelemnew Example

Here, we've created myXml with root and child nodes using xmlelemnew().


```java
<bx:set myXml = xmlNew() >
<bx:set myXml.XMLROOT = xmlelemnew( myXml, "sampleXml" ) >
<bx:set myXml.SAMPLEXML.XMLTEXT = "This is Root node text" >
<bx:loop from="1" to="3" index="i">
	<bx:set myXml.SAMPLEXML.XMLCHILDREN[ i ] = xmlelemnew( myXml, "childNode#i#" ) >
	<bx:set myXml.SAMPLEXML.XMLCHILDREN[ i ].XMLTEXT = "This is Child node#i# text" >
</bx:loop>
<bx:dump var="#myXml#">
```


### Additional Examples


```java
xml_document = XmlNew(); // new XML document to populate
xml_root = XmlElemNew( xml_document, "notes" )
xml_document.XMLROOT = xml_root; // set the root node of the XML document
xml_child = XmlElemNew( xml_document, "note" ); // first child node
xml_secondary_child = XmlElemNew( xml_document, "to" ); // child node for the first child node
xml_child.XMLCHILDREN.append( xml_secondary_child )
xml_document.XMLROOT.XMLCHILDREN.append( xml_child ); // add the first child node to the XML document
dump( xml_document )

```


### Build a document with a root and a child

```java
doc = xmlNew()
doc.xmlRoot = xmlElemNew( doc, "notes" )
child = xmlElemNew( doc, "note" )
child.xmlText = "hello"
doc.xmlRoot.xmlChildren.append( child )
writeOutput( doc.xmlRoot.xmlChildren[ 1 ].xmlName & " " & doc.xmlRoot.xmlChildren[ 1 ].xmlText )
```

Result: note hello

### Create an element with a namespace

The document needs a root element before further elements are created from it.

```java
doc = xmlParse( "<root/>" )
elem = xmlElemNew( doc, "foo", "http://example.com/ns" )
writeOutput( elem.xmlName )
```

Result: foo
