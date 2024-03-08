# Apache Derby JDBC for BoxLang

## Versioning

This module bundles Apache Derby 10.16.1.1 because it is the latest version that still supports java 17 - see [note in the Apache Derby 10.16 dev guide](https://db.apache.org/derby/docs/10.16/devguide/).

## 🔐 Vulnerabilities

**WARNING:** This project has an open vulnerability in the LDAP authentication. To fix this, we may need to create a custom build with the fix backported.

* https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-46337
* https://db.apache.org/derby/derby_downloads.html
* https://nvd.nist.gov/vuln/detail/CVE-2022-46337#range-10072835