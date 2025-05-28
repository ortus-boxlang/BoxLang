### Simple Example

Prints the statements using application meta data.

<a href="https://try.boxlang.io/?code=eJx9j8tugzAQRfd8xciLCjZh36e8cKosDJUgHzCBCYyEHYQH9feL2%2BZBpHYzkud47tHNc9iSND3gOA7coPDJgyNBaFEwiQNeoCPRV24XHEGaPSV5Dh8Te1nde3SUfE4sVM4yzpKC0ncYOICCB0hj0KbQ1mwG8p30aQZvcFnCI6jZh5EaPjK1KltO1PNhelVwKw8UQkwWdnSa5c5drelZ%2FS2pTFXtyqLeWVPu6%2F%2FTHXrsyJH%2FS3D9sK73K7G60O%2FGmqJeKiryeBiWSrFiy%2BHnkUXxFylogwQ%3D" target="_blank">Run Example</a>

```java
// Fetch application meta data
data = getApplicationMetadata();
// Print application name
writeOutput( "Application name is " & (data.NAME.length() ? data.NAME : "unspecified") & "<br>" );
// Print session timeout
writeOutput( "Session timeout is " & data.SESSIONTIMEOUT & "<br>" );
// Print session management
writeOutput( "Session management is " & (data.SESSIONMANAGEMENT ? "enabled" : "disabled") );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJzLTS1JVLBVSE8tcSwoyMlMTizJzM%2FzBQq6JJYkamhac6WU5hZoKOSClAF5AI%2FJD6M%3D" target="_blank">Run Example</a>

```java
meta = getApplicationMetaData();
dump( meta );

```


