### The simple StructSome example

Here we have simple example about structsome function.


```java
<bx:script>
	struct = {
		"Name" : "Raja",
		"age" : 20,
		"mark" : 80
	};
	result = structSome( struct, ( Any key, Any value ) => {
		return key == "Name";
	} );
	writeOutput( (result ? "" : "No") & " Key Exists." );
</bx:script>

```

Result: Key Exists.

### The structSome member function example

Here we have simple example about structsome as member function.


```java
<bx:script>
	struct = {
		"Name" : "Raja",
		"age" : 20,
		"mark" : 80
	};
	result = struct.some( ( Any key, Any value ) => {
		return key == "average";
	} );
	writeOutput( (result ? "" : "No") & " Key Exists." );
</bx:script>

```

Result: No Key Exists.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1jb0KAjEQhOvsU2yZgyConSHCIRbWin3UtTF3J%2FkRjiPv7uZylWA3s9%2FOTIhocEIQa9zhBEK0h8vpemQTfSIQWYHY%2FKKndWFh238MsgZPIbkyELjsHs9DR5K1QoltP%2BKLRjWLj3WJsEGzL12P1L0l37yZ7wqdvZEz%2FI2NBuEpJt%2FXzKrOasgF1eAyyv4Lv8A9hg%3D%3D" target="_blank">Run Example</a>

```java
st = { 
	1 : {
		ACTIVE : true
	},
	2 : {
		ACTIVE : false
	},
	3 : {
		ACTIVE : false
	}
};
result = structSome( st, ( Any key, Any value ) => {
	dump( var=value, label=key );
	return value.ACTIVE;
} );
dump( result );

```


