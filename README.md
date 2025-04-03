# ⚡︎ Project Jericho - BoxLang JVM Language

[![Latest Release](https://img.shields.io/github/v/release/boxlang/boxlang?color=blue)](https://github.com/ortus-boxlang/boxlang/releases)
[![Build Status](https://github.com/ortus-boxlang/boxlang/actions/workflows/snapshot.yml/badge.svg)](https://github.com/ortus-boxlang/boxlang/actions)
[![License](https://img.shields.io/badge/license-Apache_2.0-green.svg)](LICENSE)

<img src="https://boxlang.ortusbooks.com/~gitbook/image?url=https%3A%2F%2F1598982152-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252F4ENvvb4b3Cmrr1qKj7y4%252Fuploads%252FhsDo8xdo1p6eBQwZCBd4%252Flogo-gradient-dark.png%3Falt%3Dmedia%26token%3Dbe335c82-2365-4847-805d-abd496de4560&width=768&dpr=1&quality=100&sign=f4047481&sv=2">

**BoxLang** is a powerful, multi-runtime **dynamic programming language** built for the JVM.  🚀 **Productive**, ☕ **100% Java Interoperable**, 🏗️ **Modern & Expressive**.


----

Because of God's grace, this project exists. If you don't like this, then don't read it, it's not for you.

>"Therefore being justified by faith, we have peace with God through our Lord Jesus Christ:
By whom also we have access by faith into this grace wherein we stand, and rejoice in hope of the glory of God.
And not only so, but we glory in tribulations also: knowing that tribulation worketh patience;
And patience, experience; and experience, hope:
And hope maketh not ashamed; because the love of God is shed abroad in our hearts by the
Holy Ghost which is given unto us. ." Romans 5:5

----

<p>&nbsp;</p>

## What is BoxLang?

**BoxLang** is a modern dynamic JVM language that can be deployed on multiple runtimes: operating system (Windows/Mac/*nix/Embedded), web server, lambda, iOS, android, web assembly, and more. **BoxLang** combines many features from different programming languages, including Java, CFML, Python, Ruby, Go, and PHP, to provide developers with a modern and expressive syntax.

## Why BoxLang?

> - 🏎️ **Rapid Application Development (RAD)**
> - 🔄 **Dynamic, modular, and lightweight**
> - ☕ **100% Java & JVM-friendly**
> - 🎯 **Multi-runtime: CLI, Web, Lambda, Mobile, and more**
> - 🔥 **Not only a language but a framework**


<p>&nbsp;</p>
<img src="https://boxlang.ortusbooks.com/~gitbook/image?url=https%3A%2F%2F1598982152-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252F4ENvvb4b3Cmrr1qKj7y4%252Fuploads%252FwodvII6Drg83rKAyuBhU%252Fbl-runtime-bg.png%3Falt%3Dmedia%26token%3Dbfd368a0-3f5d-4680-8a24-cc50442dd3ae&width=768&dpr=1&quality=100&sign=9e93a7ba&sv=2">
<p>&nbsp;</p>

**BoxLang** has been designed to be a highly adaptable and dynamic language to take advantage of all the modern features of the JVM and was designed with several goals in mind:

- Be a rapid application development (RAD) scripting language and middleware.
- Unstagnate the dynamic language ecosystem in Java.
- Be dynamic, modular, lightweight, and fast.
- Be 100% interoperable with Java.
- Be modern, functional, and fluent (Think mixing CFML, Node, Kotlin, Java, and Clojure)
- Be able to support multiple runtimes and deployment targets:
  - Native OS Binaries (CLI Tooling, compilers, etc.)
  - MiniServer
  - Servlet Containers - CommandBox/Tomcat/Jetty/JBoss
  - JSR223 Scripting Engines
  - AWS Lambda
  - Microsoft Azure Functions (Coming Soon)
  - Android/iOS Devices (Coming Soon)
  - Web assembly (Coming Soon)
- Compile down to Java ByteCode
- Allow backward compatibility with the existing ColdFusion/CFML language.
- Great IDE, Debugger and Tooling: https://boxlang.ortusbooks.com/getting-started/ide-tooling
- Scripting (Any OS and Shebang) and REPL capabilities

You can find our docs here: https://boxlang.ortusbooks.com/

## License

Apache License, Version 2.0.

## Professional Open-Source

This project is a professional open source project and is available as FREE and open source to use.  However, we also offer a BoxLang +/++ version that is commercially supported and with extra features.  Here are some of the features you can get with our commercial plans:

- Professional Support and Priority Queuing
- Remote Assistance and Troubleshooting
- New Feature Requests and Custom Development
- Custom SLAs
- Application Modernization and Migration Services
- Performance Audits
- Enterprise Modules and Integrations
- Much More

Visit us at [BoxLang.io Plans](https://boxlang.io/plans) for more information about our commercial plans.

## Requirements

**JRE 21** is our compiled code JDK Baseline.

## Installation

Visit our full guide here: https://boxlang.ortusbooks.com/getting-started/installation.  You can also use our quick installers below and get started with BoxLang in seconds.

### Bash/ZSH Quick Installer

```bash
/bin/bash -c "$(curl -fsSL https://downloads.ortussolutions.com/ortussolutions/boxlang/install-boxlang.sh)"
```

### Windows PowerShell Quick Installer

```powershell
iex ((New-Object System.Net.WebClient).DownloadString('https://raw.githubusercontent.com/ortus-boxlang/boxlang-quick-installer/main/src/install-boxlang.ps1'))
```

Once installed you will have the following binaries available:

- `boxlang` - The BoxLang language binary
- `boxlang-miniserver` - The BoxLang MiniServer binary
- `install-boxlang` - The BoxLang installer scripts
- `install-bx-module` - The BoxLang module installer script

https://boxlang.ortusbooks.com/getting-started/running-boxlang

```bash
# Startup the REPL
boxlang

# Execute a script
boxlang myscript.bxs

# Execute a class main() method
boxlang MyClass.bx

# Start the MiniServer
boxlang-miniserver

# Install a single module
install-bx-module bx-pdf

# Install a specific version of a module
install-bx-module bx-compat-cfml@1.11.0

# Install multiple modules
install-bx-module bx-compat-cfml bx-esapi bx-pdf
```

## BoxLang IDE

<img src="https://boxlang.ortusbooks.com/~gitbook/image?url=https%3A%2F%2F1598982152-files.gitbook.io%2F%7E%2Ffiles%2Fv0%2Fb%2Fgitbook-x-prod.appspot.com%2Fo%2Fspaces%252F4ENvvb4b3Cmrr1qKj7y4%252Fuploads%252FNv4M8hpT2ZF15vNlU9kf%252Fimage.png%3Falt%3Dmedia%26token%3D9ad1083f-ba07-4522-a435-e508cab125d9&width=768&dpr=1&quality=100&sign=cd734817&sv=2">

<p>&nbsp;</p>

BoxLang has its own IDE based on VSCode that you can use to write, test, and debug your BoxLang code.  You can download it from the following link: https://marketplace.visualstudio.com/items?itemName=ortus-solutions.vscode-boxlang

### Features at a Glance

- Syntax Highlighting
- Code Completion
- Code Formatting
- Code Folding
- Code Linting
- Code Navigation
- Code Snippets
- Language Server (LSP)
- Built-in Debugger
- MiniServer Management
- OS Management
- Much More.

Learn more about the BoxLang IDE here: https://boxlang.ortusbooks.com/getting-started/ide-tooling

## Resources

- 🌐 [Website](https://boxlang.io)
- 💼 [Professional Version](https://boxlang.io/plans)
- 📖 [Documentation](https://boxlang.ortusbooks.com)
- 💬 [Slack](https://boxteam.ortussolutions.com)
- 🌍 [Community](https://community.ortussolutions.com)
- 📰 [Latest News](https://www.ortussolutions.com/blog)
- 🕸️ [Modules](https://forgebox.io/)

Suport this project by becoming a sponsor or buying a [BoxLang +/++ Subscription](https://boxlang.io/plans). Your logo will show up here with a link to your website. [Become a sponsor](https://www.patreon.com/ortussolutions)

🐞 **Reporting a Bug**

- BoxLang: https://ortussolutions.atlassian.net/browse/BL
- BoxLang IDE: https://ortussolutions.atlassian.net/browse/BLIDE
- BoxLang Modules: https://ortussolutions.atlassian.net/browse/BLMODULES

📱 **Follow us on Social Media:**

- 🐦 [X (Twitter)](https://x.com/tryboxlang)
- 📘 [Facebook](https://www.facebook.com/tryboxlang)
- 🔗 [LinkedIn](https://www.linkedin.com/company/tryboxlang)
- 🎥 [YouTube](http://youtube.com/ortussolutions)


----

Made with ♥️ in USA 🇺🇸, El Salvador 🇸🇻 and Spain 🇪🇸
