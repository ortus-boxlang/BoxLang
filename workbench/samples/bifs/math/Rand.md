### simple example Using rand()

To generate a random number between 0 to 1

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQKErMS9HQVNC05gIAaa4HiQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( rand() );

```


### simple example Using rand() with algorithm

To generate a random number between 0 to 1 by using bxmX_COMPAT algorithm


```java
writeOutput( rand( "bxmX_COMPAT" ) );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUChKzEvR0FSws1UwUFBTg3FtbBUMFTStucrRFCooBXs4GgYE%2BbkrKeDUBADz0hiR" target="_blank">Run Example</a>

```java
writeDump( rand() >= 0 && rand() <= 1 );
writeDump( rand( "SHA1PRNG" ) >= 0 && rand() <= 1 );

```


