### Trims first and last comma from list string



<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQyMksLnHOzy1ITAZylHRS09OLdXIzc7J1kopSE1N00nLyS4t0lBQ0FTQBOxgS%2Bw%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( listCompact( ",eggs,milk,bread,flour," ) )

```

Result: eggs,milk,bread,flour

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjs0KwjAQhO95iiWnll0M%2Flw96VFR8AliXW0gSUOysa9vMSA4t4Hhm88YuLmQPMOzxkHcFNWcnfClSqrSgXdFDlNIdliKJqKrt1HolZkjFftmYptlJNLQQ6%2BMgTOHO%2BcfDmYnIwy1yBTgwd6FhZ5VkXwqAnvQiLjGDW5xhy26GRxrSB203epfA79nH%2F65PBY%3D" target="_blank">Run Example</a>

```java
// Simple function
writeOutput( listCompact( ",,,Plant,green,save,earth,," ) )
// Member function with custom delimiter
strLst = "+++1+2+3+4+++++++"
writeDump( strLst.listCompact( "+" ) )

```

### Member function

```java
writeOutput( ",,x,y,,".listCompact() )

```

Result: x,y

### Nothing to compact

```java
writeOutput( listCompact( "a,b,c" ) )

```

Result: a,b,c
