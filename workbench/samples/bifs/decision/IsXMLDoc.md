### Checks if a given XML is valid




```java
<bx:xml variable="example">
	<boxlangengines>
		<engine>
			<name>Adobe ColdFusion</name>
		</engine>
		<engine>
			<name>Boxlang</name>
		</engine>
		<engine>
			<name>Railo</name>
		</engine>
		<engine>
			<name>Open BlueDragon</name>
		</engine>
	</boxlangengines>
</bx:xml>
<bx:script>
	writeOutput( isXMLDoc( example ) );
</bx:script>

```

Result: YES

### Additional Examples


```java
<bx:xml variable="xmlobject">
	<office>
		<employee>
			<emp_name>boxlang_dev</emp_name>
			<emp_no>121</emp_no>
		</employee>
	</office>
	</bx:xml>
<bx:dump var="#isxmldoc( xmlobject )#"/>
```


