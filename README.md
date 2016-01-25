# ews-redirector
Small utility to background forward all e-mails from MS(tm)(r)(c) Exchange(tm)(r)(c) account to another e-mail (any type).

This utility work via Exchange EWS API (web-mail interface). After starting application remains in the system tray.  
It also can:
* Filter forwarded messages, it used own filter settings.
* Autodiscovering EWS URL.
* Work with server pull notifications (message forwarded immediately after receipt).  

# Runtime dependencies
We need Java runtime to run this application (starting with version 1.7) and GUI with system tray support (on *nix systems).

# Build dependencies:
* ews-java-api 2.0
* org.json 20151123  
I build jar via IntelliJ IDEA (community), extract libraries into application jar, all-in-one file.  
