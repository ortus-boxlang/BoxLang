# BoxLang Contributing Guide

Hola amigo! I'm really excited that you are interested in contributing to BoxLang. Before submitting your contribution, please make sure to take a moment and read through the following guidelines:

- [Code Of Conduct](#code-of-conduct)
- [Bug Reporting](#bug-reporting)
- [Support Questions](#support-questions)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Security Vulnerabilities](#security-vulnerabilities)
- [Development Setup](#development-setup)
- [Language Compatiblity](#language-compatiblity)
- [Coding Styles \& Formatting](#coding-styles--formatting)
- [Financial Contributions](#financial-contributions)
- [Contributors](#contributors)

## Code Of Conduct

This project is open source, and as such, the maintainers give their free time to build and maintain the source code held within. They make the code freely available in the hope that it will be of use to other developers and/or businesses. Please be considerate towards maintainers when raising issues or presenting pull requests.  **We all follow the Golden Rule: Do to others as you want them to do to you.**

- As contributors and maintainers of this project, we pledge to respect all people who contribute through reporting issues, posting feature requests, updating documentation, submitting pull requests or patches, and other activities.
- Participants will be tolerant of opposing views.
- Examples of unacceptable behavior by participants include the use of sexual language or imagery, derogatory comments or personal attacks, trolling, public or private harassment, insults, or other unprofessional conduct.
- Project maintainers have the right and responsibility to remove, edit, or reject comments, commits, code, wiki edits, issues, and other contributions that are not aligned with this Code of Conduct. Project maintainers who do not follow the Code of Conduct may be removed from the project team.
- When interpreting the words and actions of others, participants should always assume good intentions.  Emotions cannot be derived from textual representations.
- Instances of abusive, harassing, or otherwise unacceptable behavior may be reported by opening an issue or contacting one or more of the project maintainers.

## Bug Reporting

BoxLang tracks its issues in Jira and each module track it's own issues in its repo.

- BoxLang JIra : https://ortussolutions.atlassian.net/browse/BL/issues

If you file a bug report, your issue should contain a title, a clear description of the issue, a way to replicate the issue, and any support files that we might need to replicate your issue. The goal of a bug report is to make it easy for yourself - and others - to replicate the bug and develop a fix for it.  All issues that do not contain a way to replicate will not be addressed.

## Support Questions

If you have any questions on usage, professional support or just ideas to bounce off the maintainers, please do not create an issue.  Leverage our support channels first.

- Ortus Community Discourse: https://community.ortussolutions.com
- Box Slack Team: http://boxteam.ortussolutions.com/
- Professional Support: https://www.ortussolutions.com/services/support

## Pull Request Guidelines

- The `master` branch is just a snapshot of the latest stable release. All development should be done in dedicated branches. Do not submit PRs against the master branch. They will be closed.
- All pull requests should be sent against the `development` branch.
- It's OK to have multiple small commits as you work on the PR - GitHub will automatically squash it before merging.
- Make sure all local tests pass before submitting the merge.
- Please make sure all your pull requests have companion tests.
- Please link the Jira issue in your PR title when sending the final PR

## Security Vulnerabilities

If you discover a security vulnerability, please send an email to the development team at [security@ortussolutions.com](mailto:security@ortussolutions.com?subject=security) and make sure you report it to the `#security` channel in our Box Team Slack Channel. All security vulnerabilities will be promptly addressed.

## Development Setup

We have added all the necessary information for you to develop on BoxLang in our [readme collaboration area](../readme.md#collaboration).

## Language Compatiblity

Please make sure you use JDK21+.

## Coding Styles & Formatting

We are big on coding styles and have included two codings styles for you to follow:

- [cfformat](../.cfformat.json) - For BoxLang/CFML code
- [Java](../ortus-java-style.xml) - For Java code

```bash
# Format everything
box run-script format

# Start a watcher, type away, save and auto-format for you
box run-script format:watch
```

We recommend that anytime you hack on the core you start the formatter watcher (`box run-script format:watch`). This will monitor your changes and auto-format your code for you.

You can also see the Ortus Coding Standards you must follow here: https://github.com/Ortus-Solutions/coding-standards

## Financial Contributions

You can support ColdBox and all of our Open Source initiatives at Ortus Solutions by becoming a patreon.  You can also get lots of goodies and services depending on the level of contributions.

- [Become a backer or sponsor on Patreon](https://www.patreon.com/ortussolutions)
- [One-time donations via PayPal](https://www.paypal.com/paypalme/ortussolutions)

## Contributors

Thank you to all the people who have already contributed to BoxLang! We: heart: : heart: : heart: love you!

<a href = "https://github.com/ortus-boxlang/boxlang/graphs/contributors">
  <img src = "https://contrib.rocks/image?repo=ortus-boxlang/boxlang"/>
</a>

Made with [contributors-img](https://contrib.rocks)
