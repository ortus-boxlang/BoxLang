### Check if given path is an XML element




```java
<bx:xml variable="example">
	<coldfusionengines>
		<engine>
			<name>Adobe ColdFusion</name>
		</engine>
		<engine>
			<name>Lucee</name>
		</engine>
		<engine>
			<name>Railo</name>
		</engine>
		<engine>
			<name>Open BlueDragon</name>
		</engine>
	</coldfusionengines>
</bx:xml>
<bx:script>
	writeOutput( isXMLElem( example.COLDFUSIONENGINES ) );
</bx:script>

```

Result: YES

### Additional Examples


```java
<bx:xml variable="xmlobject">
	<office>
		<employee>
			<emp_name>lucee_dev</emp_name>
			<emp_no>121</emp_no>
		</employee>
	</office>
</bx:xml>
<bx:dump var="#IsXmlElem( xmlobject.OFFICE )#"/>
```


