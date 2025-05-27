### Tag Example




```java
<bx:set myString = "My test string" >
<bx:set mySubstring1 = "Test, String" >
<bx:set mySubString2 = "Replaced, Sentence" >
<bx:output>#replaceListNoCase( myString, mySubstring1, mySubString2 )#</bx:output>
```

Result: My Replaced Sentence

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjrsKwkAURPt8xXUrhSG%2BX4iFqGChFopou48bs2IS2SS6%2Fr1PsLGbGQ7DuTlb8KxMLlVyfDlLzUubF%2BtsKnOuklhY2m3nmwpoIa9MklKrmYy8hwIkYosyZ%2FeKez7rLGGo0pi7oBrVRlSv03em9%2FznJXgyK04UO4rKVBc2S4O8cDQm4b2fqKnh6Bjbk%2FcHqfTsU8QouP20n3j4T11CQcOAEeGIGBanl2gDTbTQRgdd9NDHAMOPbvAAJBRSiQ%3D%3D" target="_blank">Run Example</a>

```java
writeDump( replaceListNoCase( "Hi USER!, Have a nice day.", "hi,user", "Welcome,buddy" ) ); // Welcome buddy!, Have a nice day.
// Member function
str = "xxxAbCdefghijxxXabcDefghij";
writeDump( str.replaceListNoCase( "a,b,c,d,e,f,g,h,i,j", "0,1,2,3,4,5,6,7,8,9" ) );

```


