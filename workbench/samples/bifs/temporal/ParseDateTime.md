### Tag Example

 


```java
<bx:set dateTimeVar = dateTimeFormat( now(), "yyyy.MM.dd HH:nn:ss " ) > 
 <bx:output> 
 #parseDateTime( dateTimeVar )# 
 </bx:output> 
```

Result: 

### Additional Examples


```java
datetime = dateTimeFormat( now(), "yyyy.MM.dd HH:nn:ss" );
dump( ParseDateTime( datetime ) );

```


