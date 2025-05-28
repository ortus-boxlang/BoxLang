### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrSnXLzEvxy3dOLE7VUFByVdJRUCpJLS5RMDQyVlRS0LTmAgC4MAkY" target="_blank">Run Example</a>

```java
reFindNoCase( "E", "test 123!" );

```

Result: 2

### Script Syntax

CF2016+ example with all optional arguments

<a href="https://try.boxlang.io/?code=eJzzCvb3C04tykzMyaxK1VAoSnXLzEvxy3dOLAbylFyVdBSUSlKLSxQMjYwVgRxDHYWSotJUoKijj4%2BSgqaCpjUXAMLsEn0%3D" target="_blank">Run Example</a>

```java
JSONSerialize( reFindNoCase( "E", "test 123!", 1, true, "ALL" ) );

```

Result: [{"len":[1],"pos":[2],"match":["e"]}]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNjsFLwzAYxe%2F7Kz5z2Fo7Bs30siEj1HkSD2OCkOXwNY22uKYlyZj%2B937ZPGyCOkjII7z3fm%2FvmmDud22fwGr50NjqqSvQmwQYZjpjY3pLjVgUVcUghXQ%2B2P%2BVuI4JQQmhL0nIzWKonOnhDmKQVOfCRL%2B2i%2BNnzqc3wx2J2wvgsaFERPGvd3KY%2BW0LxgcfXGPf8jhjXRvQGKCxEEjWJOPV2BooUb9fsfnAh2g9llJdIkdyhtu%2BxpkaqSyVoLJkk6cEOSkfAx22Xj0v2Y91VCeBbY1loCRMQZ3P4nGWEOKFrjjQ%2BSn94wzDf8egc%2Fj5aGwEcgL2nWeRFW1fc4uWkg%3D%3D" target="_blank">Run Example</a>

```java
writeDump( REFindNoCase( "a+c+", "abcaaCCdd" ) );
writeDump( REFindNoCase( "a+c*", "AbcaAcCdd" ) );
writeDump( REFindNoCase( "[\?&]rep = ", "report.bxm?rep = 1234&u = 5" ) );
writeDump( REFindNoCase( "a+", "baaaA" ) );
writeDump( REFindNoCase( ".*", "" ) );
teststring1 = "The cat in the hat hat came back!";
st1 = REFind( "(['[:alpha:]']+)[ ]+(\1)", teststring1, 1, "TRUE" );
writeDump( st1[ "len" ][ 3 ] );
teststring2 = "AAAXAAAA";
st2 = REFind( "x", teststring2, 1, "TRUE" );
writeDump( arrayLen( st2[ "pos" ] ) );

```


