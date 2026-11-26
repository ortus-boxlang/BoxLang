### xmlGetNodeType Example

A string identifying the XML node type.


```java
<bx:xml variable="xmlobject1">
	<notes>
		<note>
			<to>Alice</to>
			<from>Bob</from>
			<heading>Reminder</heading>
			<body>Here is the message you requested.</body>
		</note>
		<author>
			<first>John</first>
			<last>Doe</last>
		</author>
	</notes>
</bx:xml> 
<bx:output>
	 xmlobject:#XMLGetNodeType( xmlobject1 )#
	 xmlRoot:#XMLGetNodeType( xmlobject1.XMLROOT )#
</bx:output>
```


### Additional Examples


```java
xml_stream = "
    <?xml version=""1.0"" encoding=""UTF-8""?>
    <notes>
      <note>
        <to>Alice</to>
        <from>Bob</from>
        <heading>Reminder</heading>
        <body>Here is the message you requested.</body>
      </note>
      <note>
        <to>Bob</to>
        <from>Alice</from>
        <heading>Your request</heading>
        <body>I got your message; all is well.</body>
      </note>
    </notes>"
xml_document = XmlParse( trim( xml_stream ) )
dump( XmlGetNodeType( xml_document.XMLROOT.XMLCHILDREN[ 1 ].XMLCHILDREN.last() ) )

```


### Node types of a document and its root element

```java
xml = xmlParse( "<root><a/></root>" )
writeOutput( xmlGetNodeType( xml ) & " " & xmlGetNodeType( xml.xmlRoot ) )
```

Result: DOCUMENT_NODE ELEMENT_NODE

### Use the member function

```java
xml = xmlParse( "<root><a/></root>" )
writeOutput( xml.xmlRoot.getNodeType() )
```

Result: ELEMENT_NODE
