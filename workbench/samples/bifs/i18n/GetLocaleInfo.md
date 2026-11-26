### Get information about the page's locale



<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL19AEAC57BW4%3D" target="_blank">Run Example</a>

```java
getLocaleInfo()

```

Result: {
  country : "US",
  iso : {
    country : "USA",
    language : "eng"
  },
  display : {
    country : "United States",
    language : "English"
  },
  language : "en",
  name : "English (United States)",
  variant : ""
}

### Output page's locale in a divergent language

Outputs the language locale of the page in German.

<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL11BIKS6AcG2V3FOLchPzlBQ09Vw8gwN8HCP1fBz93EMd3V0B36kQ1g%3D%3D" target="_blank">Run Example</a>

```java
getLocaleInfo( dspLocale="German" ).DISPLAY.LANGUAGE

```

Result: Englisch

### Output German locale in a page's language

Outputs the German locale in the language defined for the page.

<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL11DIAbNtlVJSdV1clXQUUooLINK26TCFLpnFBTmJlX6Juakamgqaei6ewQE%2BjpF6zv6hfiFBkQB%2B5hsc" target="_blank">Run Example</a>

```java
getLocaleInfo( locale="de-DE", dspLocale=getLocaleDisplayName() ).DISPLAY.COUNTRY

```

Result: Germany

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwTy3xyU9OzEn1zEvL19BU0AQAYZkHlQ%3D%3D" target="_blank">Run Example</a>

```java
dump( GetLocaleInfo() )

```


