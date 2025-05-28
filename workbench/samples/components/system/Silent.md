### Example for bx:silent tag

Content within bx:silent tag will not be displayed


```java
 <bx:set a = 10 > 
 <bx:set b = 5 > 
 <bx:silent> 
    <bx:output> This is from inside bx:silent #a + b# </bx:output> 
 </bx:silent> 
 <bx:output>This is from outside bx:silent #a - b#</bx:output> 
```

Result: This is from outside bx:silent 5

