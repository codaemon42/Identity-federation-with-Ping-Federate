@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

if not "%JAVA_HOME%" == "" goto PROCEED

echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK or JRE to avoid this message.
exit /B 1

:PROCEED
set JAVA=%JAVA_HOME%\bin\java

set JAVA_VERSION=
"%JAVA_HOME%/bin/java" -version 2>java_version.txt
for /f "tokens=3" %%g in (java_version.txt) do (
    del java_version.txt
    set JAVA_VERSION=%%g
    goto CHECK_JAVA_VERSION
)

rem grab first 3 characters of version number (ex: 1.6) and compare against required version
:CHECK_JAVA_VERSION
set JAVA_VERSION17=%JAVA_VERSION:~1,2%
if %JAVA_VERSION17% == 17 (
    goto SET_CP
)
set JAVA_VERSION11=%JAVA_VERSION:~1,2%
if %JAVA_VERSION11% == 11 (
    goto SET_CP
)
set JAVA_VERSION8=%JAVA_VERSION:~1,3%
if %JAVA_VERSION8% == 1.8 (
    goto SET_CP
)

:WRONG_JAVA_VERSION
echo JDK 8, 11 or 17 is required to run PingFederate but %JAVA_VERSION% was detected.
echo Please set the JAVA_HOME environment variable to a JDK 8, 11 or 17 installation directory path.

:SET_CP
set DIRNAME=.
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0%
set DIRNAME=%DIRNAME%..

set CLASSPATH="%DIRNAME%/lib/*"

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
    -Xms128m -Xmx128m ^
    %JAVA_OPTS% ^
    -Dupgrade.home.dir="%DIRNAME%" ^
    -Dlog.dir="%DIRNAME%/log" ^
    -Dlog4j.configurationFile="file:///%DIRNAME%/bin/log4j2.xml" ^
    -cp %CLASSPATH% com.pingidentity.pingfederate.migration.UpgradeUtility %*
:END