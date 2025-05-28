### Simple bx:exit example

Here the loop over the 5 number. When it's meet the condition as true then the block of code get exit.


```java
<bx:output>
<bx:loop from="1" to="5" index="i">
<bx:if i == 3 >
	<bx:exit>
<bx:else>
	#i#
</bx:if>
</bx:loop>
</bx:output>
```

Result: 1 2

