### Simple Example

Replaces the 2nd list element with 'foo'

<a href="https://try.boxlang.io/?code=eJzLySwuCU4tcSzRUFBKSizSyckvSs3VySwoLs1V0lEw0lFQSsvPV1LQtOYCADQuDOQ%3D" target="_blank">Run Example</a>

```java
listSetAt( "bar,lorem,ipsum", 2, "foo" );

```

Result: bar,foo,ipsum

### Example with Custom Delimiter

Inserts 'foo' into the list with a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwuCU4tcSzRUFBKSiyqyckvSs3VySwoLs2tyU2tKc7PTS3JyMxLV9JRMNJRUErLzweylGqUFDStuQCfNhPY" target="_blank">Run Example</a>

```java
listSetAt( "bar|lorem,ipsum|me|something", 2, "foo", "|" );

```

Result: bar|foo|me|something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNjjELwjAQhff8iiOThcPS1q04KOLm5NRubXJCIK2lTQThfryprVoHweVx3N1737NmcAlsQWKFNSrUSDIXB990K7Dhdia3c9OYIMhUBiklRBDlYlymk5e5ZlbMmvmnP%2F34gzAvUrL%2FGmRfCRjE9Z6eMRDHsCdV%2BYHAODCtsl7TANR07g4XQ1bDrbLhG8sXRgTPiZqa%2BqNvlTPXVug3Nlkv4HIzAou58ciayxYh5QGgO1x8" target="_blank">Run Example</a>

```java
list1 = ",a,b,c,d,e";
Dump( listSetAt( list1, "2", "Z" ) );
list2 = ",a||b||c||d||e";
Dump( listSetAt( list2, "2", "Z", "||" ) );
list3 = ",a,b,c,d,e";
Dump( listSetAt( list3, "2", "Z", ",", true ) ); // Because it includes empty field value ,Z,b,c,d,e
// MemberFunction
dump( list1.listSetAt( "4", "Y" ) );
 // ,a,b,c,Y,e

```


