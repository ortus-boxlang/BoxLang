### Compare Two Dates by Year



<a href="https://try.boxlang.io/?code=eJxLSSxJdc7PLUgsStVQUDI00jc20DcyMDRV0gHzDIzgvEogUFLQtOYCAGFDDFs%3D" target="_blank">Run Example</a>

```java
dateCompare( "12/30/2015", "12/02/2015", "yyyy" );

```

Result: 0

### Compare Two Dates by Day

Returns 1 because date1 is greater than date 2

<a href="https://try.boxlang.io/?code=eJxLSSxJdc7PLUgsStVQUDI00jc20DcyMDRV0gHzDIzgvBQlBU1rLgA4GArb" target="_blank">Run Example</a>

```java
dateCompare( "12/30/2015", "12/02/2015", "d" );

```

Result: 1

### Member function example



<a href="https://try.boxlang.io/?code=eJxLMVSwVUguSk0sSXUBYg0FJSMDIxMlHQUlA0MoqaBpzZVihFOZEYg0NIEoM9RLzs8tSCwCqgBqAYoAAGSPFq0%3D" target="_blank">Run Example</a>

```java
d1 = createDate( "2024", "01", "01" );
d2 = createDate( "2024", "02", "14" );
d1.compare( d2 );

```

Result: -1

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQSEksSXXOzy1ILErVUMjLL9fQ1FFQMjTUNzTQN7S0NFJS0FRQU1BS0HABqjNUyCxWyAEyihRKMhLzwHqNNG2SiuyAyqy5ynGai2wgAeNTC0sTcxRK8sk1HOwFqKkIQxOLcjJRXA02EwBD4Ewp" target="_blank">Run Example</a>

```java
writeOutput( dateCompare( now(), "11/10/1992" ) & " (Date1 is later than date2)<br>" );
writeOutput( dateCompare( "11/10/1992", "11/10/1992" ) & " (Date1 is equal to date2)<br>" );
writeOutput( dateCompare( "11/10/1992", now() ) & " Date1 is earlier than date2" );

```


<a href="https://try.boxlang.io/?code=eJx1zsEKwjAMBuD7niL0IC2IW3YbUy969xmiLWywrTOmjL29ncrcEA%2BBBPJ%2FiYUD3NiRuHMsDaMj1tD5QRswW2h9J9VitjTOE5gysRjzCjHFLMWiyFWZDFyLuwTpg2iwu5Nve%2BIoL7dieAMKppMI9QOa2DBIRV28IC7fX%2FmoJj%2F779kPomfF3QM1IP5tmBlZC%2FhLfAXipl598gKe58xUzw%3D%3D" target="_blank">Run Example</a>

```java
d = createDate( year( now() ), month( now() ), day( now() ) );
d1 = "11/10/1992";
writeOutput( d.Compare( "11/10/1992" ) & " Date1 is later than date2<br>" );
0;
writeOutput( d.Compare( d ) & " (Date1 is equal to date2)<br>" );
writeOutput( d1.Compare( d ) & " Date1 is earlier than date2" );

```


