### Tag Example

The following example calls the Randomize function to seed the random number generator and generates 10 random numbers.  


```java
<bx:set randomize( 12345 ) > <!--- if one was to remove this line, the random numbers are different every time --->  
 <bx:loop index="i" from="1" to="10"> 
 <bx:output>#rand()#</bx:output> 
 </bx:loop> 
```

Result: 

### Additional Examples


```java
writeDump( Randomize( 8, "SHA1PRNG" ) );
writeDump( Randomize( 10 ) >= 0 && Randomize( 10 ) <= 1 );
randomize( 55 );
bx:loop index="i" from="1" to="3" {
	writeDump( rand() );
}

```


