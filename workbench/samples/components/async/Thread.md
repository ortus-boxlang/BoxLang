### Script Syntax

CF9+


```java
// do single thread stuff
bx:thread action="run" name="myThread";
bx:thread action="join" name="myThread,myOtherThread";

```

Result: 

### Tag Syntax




```java
<bx:thread action="run" name="myThread">
 <!--- Do single thread stuff ---> 
 </bx:thread> 
 <bx:thread action="join" name="myThread,myOtherThread"/>
```

Result: 

