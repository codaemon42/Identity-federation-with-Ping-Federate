@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0
set DIRNAME=%DIRNAME%..

set SERVER=%DIRNAME%\server\default
set SERVER_LIB=%SERVER%\lib
set SERVER_CONF=%SERVER%\conf

set CLASSPATH=%DIRNAME%\bin\pf-consoleutils.jar
set CLASSPATH=%CLASSPATH%;%DIRNAME%\bin\pf-startup.jar
set CLASSPATH=%CLASSPATH%;%SERVER_CONF%\conf\generated-hivemodule\"

REM Read an optional running configuration file
@if ["%RUN_CONF%"] == [""] (
    set "RUN_CONF=%~dsp0%conf.bat"
)
@if exist "%RUN_CONF%" (
    call "%RUN_CONF%"
)

set "RUN_PROPERTIES="
set "RUN_PROPERTIES_FILE=%~dsp0%run.properties"
if exist %RUN_PROPERTIES_FILE% (
    set "RUN_PROPERTIES=%RUN_PROPERTIES_FILE%"
) else (
    echo "Missing %RUN_PROPERTIES_FILE%, using defaults."
)

set CLASSPATH=%CLASSPATH%;%SERVER_LIB%\*

if not exist log mkdir log

if "%JAVA_HOME%" == "" (
    set JAVA=java
    echo JAVA_HOME is not set.  Unexpected results may occur.
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
    set JAVA=%JAVA_HOME%\bin\java
)

rem get java version
SetLocal EnableDelayedExpansion
for /f tokens^=2-5^ delims^=.-_^+^" %%j in ('"%JAVA_HOME%/bin/java.exe" -fullversion 2^>^&1') do (
    set "JAVA_PRODUCT_VERSION=%%j"
    if !JAVA_PRODUCT_VERSION! EQU 1 (
        set "JAVA_PRODUCT_VERSION=%%k"
    )
)
SetLocal DisableDelayedExpansion

rem java 17 support
if %JAVA_PRODUCT_VERSION% == 17 (
    set JAVA_OPTS=%JAVA_OPTS% ^
    --add-opens=java.base/java.lang=ALL-UNNAMED ^
    --add-opens=java.base/java.util=ALL-UNNAMED ^
    --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED ^
    --add-exports=java.base/sun.net.util=ALL-UNNAMED ^
    --add-exports=java.base/sun.security.pkcs=ALL-UNNAMED ^
    --add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED ^
    --add-exports=java.base/sun.security.x509=ALL-UNNAMED ^
    --add-exports=java.base/sun.security.util=ALL-UNNAMED
)

"%JAVA%" ^
    %JAVA_OPTS% ^
    -Dpingfederate.log.dir="%DIRNAME%/log" ^
    -Dpf.server.default.dir="%DIRNAME%/server/default" ^
    -Dlog4j.configurationFile="file:///%DIRNAME%/bin/logfilter.log4j2.xml" ^
    -Drun.properties="%RUN_PROPERTIES%" ^
    -cp "%CLASSPATH%" com.pingidentity.console.logfilter.utility.LogFilterUtility %*

goto :eof