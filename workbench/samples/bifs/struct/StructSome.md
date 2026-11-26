### The simple StructSome example

Here we have simple example about structsome function.


```java
<bx:script>
	struct = {
		"Name" : "Raja",
		"age" : 20,
		"mark" : 80
	}
	result = structSome( struct, ( Any key, Any value ) => {
		return key == "Name"
	} )
	writeOutput( (result ? "" : "No") & " Key Exists." )
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
	}
	result = struct.some( ( Any key, Any value ) => {
		return key == "average"
	} )
	writeOutput( (result ? "" : "No") & " Key Exists." )
</bx:script>

```

Result: No Key Exists.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1jb0KAjEQhOvdp9gygSConRDhEAtrxT7q2pg7j%2FwIx5F3d%2B9ylWA3O9%2FOTExkaSSENe1oRIDmcDldj3KkkBmhGITNL3o6Hxe2%2FcewYOCY%2FdQfpeuezu%2BWlWhDippuoBcPZhYf5zOTJrufqh657ZV4wc6%2BIe9u7K18k0YInHLoamRVR7EIqKllUX8BYjw8VQ%3D%3D" target="_blank">Run Example</a>

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
}
result = structSome( st, ( Any key, Any value ) => {
	dump( var=value, label=key )
	return value.ACTIVE
} )
dump( result )

```

Result: dumps true (key 1 is ACTIVE); the callback may stop iterating after the first match


