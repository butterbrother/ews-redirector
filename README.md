# ews-redirector
Small utility to background forward all e-mails from MS(tm)(r)(c) Exchange(tm)(r)(c) account to another e-mail (any type).

This utility work via Exchange EWS API (web-mail interface). After starting application remains in the system tray. 
![tray-windows](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_win_tray.png)

It also can:
* Filter forwarded messages, it used own filter settings.
* Autodiscovering EWS URL.
* Work with server pull notifications (message forwarded immediately after receipt).  

# Screenshots  
Configuration window:  
![config-linux](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_lin_config.png)
![config-windows](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_win_config.png) 

Filtering rules:  
![filters-linux](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_lin_rules.png) 
![filters-windows](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_win_rules.png)  

Filter configuration:  
![filter-config-windows](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_win_rule.png)  
![filter-config-linux](https://github.com/butterbrother/ews-redirector/raw/master/img/redirector_lin_rule.png)

# Runtime dependencies
We need Java runtime to run this application (starting with version 1.7) and GUI with system tray support (on *nix systems).

# Build dependencies:
* ews-java-api 2.0
* org.json 20151123  

You can build if with [maven](https://maven.apache.org/):  
`mvn assembly:assembly`  
It download dependencies and build application with all it.
