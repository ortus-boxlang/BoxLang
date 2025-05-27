### Retrieve parent tag thisTag scope

Use getBaseTagData() to retrieve the execution mode of the parent CF_MAPPER custom tag.


```java
<bx:set tagData = getBaseTagData( "CF_MAPPER" ) >
<!--- Find the tag's execution mode Located inside the --->
<bx:if tagData.THISTAG.EXECUTIONMODE != "inactive" >
template
<bx:else>
BODY
</bx:if>
```

Result: 

### Retrieve parent tag attributes

Use getBaseTagData() to retrieve the attributes of the parent cf_iframe tag


```java
variables.PARENTATTRIBUTES = getBaseTagData( "cf_iframe" ).ATTRIBUTES;

```

Result: 

