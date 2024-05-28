@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0

set PF_HOME=%DIRNAME%..
cd %PF_HOME%

set LIB=lib
set BIN=bin

set SERVER=server\default
set SERVER_LIB=%SERVER%\lib
set SERVER_DEPLOY=%SERVER%\deploy
set SERVER_CONF=%SERVER%\conf

set PF_ENDORSED_DIRS=%LIB%\endorsed

set CLASSPATH=%SERVER_CONF%;%SERVER%\data\config-store;%SERVER_CONF%\generated-hivemodule\"

REM Read an optional running configuration file
@if ["%RUN_CONF%"] == [""] (
    set RUN_CONF=%dsp0%conf.bat
)
@if exist "%RUN_CONF%" (
    call "%RUN_CONF%"
)

set CLASSPATH=%CLASSPATH%;%LIB%\*;%SERVER_LIB%\*;%SERVER_DEPLOY%\*;%BIN%\pf-startup.jar

if not exist log mkdir log

if "%JAVA_HOME%" == "" (
    set JAVA=java
    echo JAVA_HOME is not set.  Unexpected results may occur.
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
    set JAVA=%JAVA_HOME%\bin\java
)

set JAVA_VERSION=
"%JAVA%" -version 2>java_version.txt
for /f "tokens=3" %%g in (java_version.txt) do (
    del java_version.txt
    set JAVA_VERSION=%%g
    goto CHECK_JAVA_VERSION
)

:CHECK_JAVA_VERSION
set JAVA_VERSION11=%JAVA_VERSION:~1,2%

set PF_ENDORSED_DIRS_FLAG="-Djava.endorsed.dirs=%PF_ENDORSED_DIRS%"

if %JAVA_VERSION11% == 11 (
    set "PF_ENDORSED_DIRS_FLAG="
)

set "RUN_PROPERTIES="
set "RUN_PROPERTIES_FILE=%~dsp0%run.properties"
if exist %RUN_PROPERTIES_FILE% (
    set "RUN_PROPERTIES=%RUN_PROPERTIES_FILE%"
) else (
    echo "Missing %RUN_PROPERTIES_FILE%, using defaults."
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
    set "PF_ENDORSED_DIRS_FLAG="
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
    %PF_ENDORSED_DIRS_FLAG% ^
    -Dpf.home="%PF_HOME%" ^
    -Dpf.server.default.dir="%SERVER%" ^
    -Dlog4j.configurationFile="file:///%PF_HOME%/bin/provmgr.log4j2.xml" ^
    -Drun.properties="%RUN_PROPERTIES%" ^
    -cp "%CLASSPATH%" com.pingidentity.provisioner.cli.CommandLineTool %*

goto :eof