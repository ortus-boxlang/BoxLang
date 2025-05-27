### Script Syntax




```java
bx:lock timeout="60" scope="session" type="exclusive" {
	session.MYVAR = "Hello";
}

```

Result: 

### Tag Syntax




```java
<bx:lock timeout="60" scope="session" type="exclusive"> 
 <bx:set session.MYVAR = "Hello" > 
 </bx:lock>
```

Result: 

