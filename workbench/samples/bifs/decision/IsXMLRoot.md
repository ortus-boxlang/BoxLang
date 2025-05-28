### "boxlangengines" is root element




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
	writeOutput( isXMLRoot( example.BOXLANGENGINES ) );
</bx:script>

```

Result: YES

### "engine" is child of "boxlangengines"




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
	writeOutput( isXMLRoot( example.BOXLANGENGINES.ENGINE ) );
</bx:script>

```

Result: NO

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
<bx:dump var="#IsXmlroot( xmlobject.OFFICE )#"/>
```


