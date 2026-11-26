### Validate against an XML Schema

Validates that note.xml is valid according to the schema note.xsd. 

<a href="https://try.boxlang.io/?code=eJyryM0JS8zJTEksSdVQUMooKSkottLXLy8v1ys3Lk7OyM%2FPKdZLzs%2FVr8jN0c%2FLL0nVAzKUdIhTWZyipKCpFxziGBIabM0FAPhjJN4%3D" target="_blank">Run Example</a>

```java
xmlValidate( "https://www.w3schools.com/xml/note.xml", "https://www.w3schools.com/xml/note.xsd" ).STATUS

```

Result: false

### Additional Examples


```java
validator = "
		<?xml version=""1.0""?>
		<xs:schema xmlns:xs=""http://www.w3.org/2001/XMLSchema"">
			<xs:element name=""note"">
				<xs:complexType>
					<xs:sequence>
						<xs:element name=""to"" type=""xs:string""/>
						<xs:element name=""from"" type=""xs:string""/>
						<xs:element name=""heading"" type=""xs:string""/>
						<xs:element name=""body"" type=""xs:string""/>
					</xs:sequence>
				</xs:complexType>
			</xs:element>
		</xs:schema>
		"
xml_stream = "
			<?xml version=""1.0"" encoding=""UTF-8""?>
			<note>
				<to>Alice</to>
				<from>Bob</from>
				<heading>Reminder</heading>
				<body>Here is the message you requested.</body>
			</note>"
xml_document = XmlParse( trim( xml_stream ) )
dump( xmlValidate( xml_document, trim( validator ) ) )

```


### Validate a document that matches the schema

```java
xsd = '<?xml version="1.0"?><xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"><xs:element name="note"><xs:complexType><xs:sequence><xs:element name="to" type="xs:string"/><xs:element name="body" type="xs:string"/></xs:sequence></xs:complexType></xs:element></xs:schema>'
result = xmlValidate( "<note><to>A</to><body>B</body></note>", xsd )
writeOutput( result.status )
```

Result: true

### Validate a document that does not match the schema

```java
xsd = '<?xml version="1.0"?><xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"><xs:element name="note"><xs:complexType><xs:sequence><xs:element name="to" type="xs:string"/><xs:element name="body" type="xs:string"/></xs:sequence></xs:complexType></xs:element></xs:schema>'
result = xmlValidate( "<note><to>A</to></note>", xsd )
writeOutput( result.status & " " & result.errors.len() )
```

Result: false 1

### Validate an XML object

```java
xsd = '<?xml version="1.0"?><xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"><xs:element name="note"><xs:complexType><xs:sequence><xs:element name="to" type="xs:string"/><xs:element name="body" type="xs:string"/></xs:sequence></xs:complexType></xs:element></xs:schema>'
xml = xmlParse( "<note><to>A</to><body>B</body></note>" )
writeOutput( xmlValidate( xml, xsd ).status )
```

Result: true
