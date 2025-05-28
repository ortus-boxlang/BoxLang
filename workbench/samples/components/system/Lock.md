### Script Syntax




```java
bx:lock timeout="60" scope="session" type="exclusive" {
	session.MYVAR = "Hello";
}

```


### Tag Syntax




```java
<bx:lock timeout="60" scope="session" type="exclusive"> 
 <bx:set session.MYVAR = "Hello" > 
 </bx:lock>
```


