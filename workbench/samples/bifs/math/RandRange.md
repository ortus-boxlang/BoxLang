### Tag Example

The following example calls the Randomize function to seed the random number generator and generates 10 random numbers.  


```java
<bx:set r = randomize( 7, "SHA1PRNG" ) > 
 <bx:set local.MYINT = 1 > 
 <bx:set local.MYINT2 = 999 > 
<!--- Generate and display the random number. ---> 
 <bx:output><p><b> 
 RandRange returned: #randRange( local.MYINT, local.MYINT2, "SHA1PRNG" )# 
 </bx:output></b></p>  
```


### Script Example

 


```java
<bx:script>
	bytes = []
	bytecount = 32
	arrayResize( bytes, byteCount )
	for( i = 1; i <= byteCount; i++ ) {
		bytes[ i ] = randRange( -128, 127, "SHA1PRNG" )
	}
</bx:script>
 
 <bx:dump var="#bytes#"/>  
```


### Additional Examples


```java
writeDump( randRange( 25, 125, "SHA1PRNG" ) )
writeDump( randRange( 100, 500, "SHA1PRNG" ) )

```

### Random integer within a range

```java
n = randRange( 5, 10 )
writeOutput( n >= 5 && n <= 10 )

```

Result: true

### Both bounds are the same

```java
writeOutput( randRange( 7, 7 ) )

```

Result: 7
