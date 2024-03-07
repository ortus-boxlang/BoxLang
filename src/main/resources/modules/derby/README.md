# Apache Derby JDBC for BoxLang

Note we use 10.16.1.1 because it is the latest version that still supports java 17 - see https://db.apache.org/derby/docs/10.16/devguide/

## JDBC Driver Loading

Currently this driver module fails to register the Derby driver in the JDBC driver registry at `java.sql.DriverManager`. We need to either do this in the ModuleConfig.cfc's `onLoad()` method, or via a new/custom `jdbcDrivers = [ class : "org.apache.derby.iapi.jdbc.AutoloadedDriver" ]` construct in `function configure( runtime ){}`.

## 🔐 Vulnerabilities

**WARNING:** This project has an open vulnerability in the LDAP authentication. To fix this, we may need to create a custom build with the fix backported.

* https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2022-46337
* https://db.apache.org/derby/derby_downloads.html
* https://nvd.nist.gov/vuln/detail/CVE-2022-46337#range-10072835