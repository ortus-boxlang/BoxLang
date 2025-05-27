### The simple xmlnew example

Here, We created myXml by using xmlNew function. Then created root node(sampleXml) for myXml and set the rootnode text


```java
<bx:set myXml = xmlNew() >
<bx:set myXml.XMLROOT = xmlelemnew( myXml, "sampleXml" ) >
<bx:set myXml.SAMPLEXML.XMLTEXT = "This is root node text" >
<bx:dump var="#myXml#">
```

Result: 

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyryM2JT8lPLs1NzStRsFWIyM3xSy3X0LTmSinNLdBQqECWBooCAJUEEBk%3D" target="_blank">Run Example</a>

```java
xml_document = XmlNew();
dump( xml_document );

```


