### The simple cfobject example

Calling the above multiply function by using cfobject tag based code.


```java
<bx:object name="multiplyObj" type="component" component="multiply">
<bx:output>
#multiplyObj.multiply( 1, 2 )#
</bx:output>
```

Result: 2

### The simple (cfobject) example

Calling the above multiply function by using cfobject script based code.


```java
<bx:script>
	bx:object name="multiplyNum" type="component" component="multiply" {
		writeOutput( multiplyNum.multiply( 6, 7 ) );
	}
</bx:script>

```

Result: 42

