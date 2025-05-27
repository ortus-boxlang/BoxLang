### Example for cfsilent tag

Content within cfsilent tag will not be displayed


```java
 <bx:set a = 10 > 
 <bx:set b = 5 > 
 <bx:silent> 
    <bx:output> This is from inside cfsilent #a + b# </bx:output> 
 </bx:silent> 
 <bx:output>This is from outside cfsilent #a - b#</bx:output> 
```

Result: This is from outside cfsilent 5

