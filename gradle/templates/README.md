# SBK launcher templates

`sbkUnixStartScript.txt` and `sbkWindowsStartScript.txt` are repository-owned
templates used by Gradle's `CreateStartScripts` tasks. They are derived from
the Apache-licensed Gradle 9.4 application-plugin templates and deliberately
replace Gradle's default Java discovery with the SBK Java bootstrap contract.

The build configures each launcher with only its pathing JAR and application
JAR. It renders the single platform-specific JMX token before passing the
template to Gradle's template engine. Generated scripts must therefore never
be modified with marker searches or regular-expression replacements.

When upgrading Gradle:

1. Compare these files with Gradle's current `unixStartScript.txt` and
   `windowsStartScript.txt` resources.
2. Carry forward upstream quoting, platform, and argument-handling fixes.
3. Preserve the SBK bootstrap block, `sbk.appHome` argument, and JMX token.
4. Run `verifySbkJavaHomeStartScripts`, `installDist`, `distTar`, and `check`.
5. Smoke-test every installed launcher and validate both JMX modes.
