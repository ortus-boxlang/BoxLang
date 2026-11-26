### isXML Example

Returns true if the string is well-formed XML.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJVbBVULJJyq%2FIScxLT81Lz8xLLbbjUrCBMIEsBZu8xNxUO8eU%2FKRUBef8nBS30uLM%2FDwbfbAwUKU%2BXCmGJieIsUSpDUrMzMknSqV%2FQWqeglNOaapLUWI6VpfY6KN5SImrvCizJNW%2FtKSgtERDIbM4IjdHQyEVGgKaCpoASqdX9A%3D%3D" target="_blank">Run Example</a>

```java
example = "<boxlangengines>
 <engine>
  <name>Adobe ColdFusion</name>
 </engine>
 <engine>
  <name>Boxlang</name>
 </engine>
 <engine>
  <name>Railo</name>
 </engine>
 <engine>
  <name>Open BlueDragon</name>
 </engine>
</boxlangengines>"
writeOutput( isXml( example ) )

```

Result: true

### isXML Example for Invalid XML

Returns false if the string is not well-formed XML.

<a href="https://try.boxlang.io/?code=eJxLrUjMLchJNVSwVVCyScqvyEnMS0%2FNS8%2FMSy2241KwgTCBLAWbvMTcVDvHlPykVAXn%2FJwUt9LizPw8G32wMFClPlwphiYniLFQtUpc5UWZJan%2BpSUFpSUaCpnFEbk5GgqpMHdoKmgCAFMBMNY%3D" target="_blank">Run Example</a>

```java
example1 = "<boxlangengines>
 <engine>
  <name>Adobe ColdFusion</name>
 </engine>
 <engine>
  <name>Boxlang</name>"
writeOutput( isXml( example1 ) )

```

Result: false

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLz8lPSswJSyxSsFVQsnFNLCrJsONSsClKLSzNLEpNAbIVbPISc1PtQopSU4tt9MFsoAJ9JBVYVIcnlqQWEa3aMb0oM7k0p6S0KJVoPR6luYl5mSWVWDXY6EN8osRVXpRZkupfWlJQWqKhkFkckZujoZAO97OmgiYAW9RUfA%3D%3D" target="_blank">Run Example</a>

```java
globalVar = "<Earth>
 <required>
  <name>Trees</name>
 </required>
 <required>
  <name>Water</name>
 </required>
 <required>
  <name>Agriculture</name>
 </required>
 <required>
  <name>Humanity</name>
 </required>
</Earth>"
writeOutput( isXml( globalVar ) )

```


