### The simple bx:object example

Calling the above multiply function by using bx:object tag based code.


```java
<bx:object name="multiplyObj" type="component" component="multiply">
<bx:output>
#multiplyObj.multiply( 1, 2 )#
</bx:output>
```

Result: 2

### The simple (bx:object) example

Calling the above multiply function by using bx:object script based code.


```java
<bx:script>
	bx:object name="multiplyNum" type="component" component="multiply" {
		writeOutput( multiplyNum.multiply( 6, 7 ) );
	}
</bx:script>

```

Result: 42

