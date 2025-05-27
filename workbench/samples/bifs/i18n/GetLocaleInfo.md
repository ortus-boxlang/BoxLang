### Get information about the page's locale



<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL19C05gIAOdcFsw%3D%3D" target="_blank">Run Example</a>

```java
getLocaleInfo();

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

<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL11BIKS6AcG2V3FOLchPzlBQ09Vw8gwN8HCP1fBz93EMd3V2tuQAB5BEb" target="_blank">Run Example</a>

```java
getLocaleInfo( dspLocale="German" ).DISPLAY.LANGUAGE;

```

Result: Englisch

### Output German locale in a page's language

Outputs the German locale in the language defined for the page.

<a href="https://try.boxlang.io/?code=eJxLTy3xyU9OzEn1zEvL11DIAbNtlVJSdV1clXQUUooLINK26TCFLpnFBTmJlX6Juakamgqaei6ewQE%2BjpF6zv6hfiFBkdZcALWeG2E%3D" target="_blank">Run Example</a>

```java
getLocaleInfo( locale="de-DE", dspLocale=getLocaleDisplayName() ).DISPLAY.COUNTRY;

```

Result: Germany

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLKc0t0FBwTy3xyU9OzEn1zEvL19BU0LTmAgBxQwfa" target="_blank">Run Example</a>

```java
dump( GetLocaleInfo() );

```


