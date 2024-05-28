@echo off

REM @if not "%ECHO%" == ""  echo %ECHO%
if "%OS%" == "Windows_NT"  setlocal

set PF_BIN=.\
set PROGNAME=usercount.bat

if "%OS%" == "Windows_NT" set PF_BIN=%~dp0
if "%OS%" == "Windows_NT" set PROGNAME=%~nx0

REM Set PF_HOME_ESC - this is PF_HOME but with spaces that are replaced with %20
SetLocal EnableDelayedExpansion
set PF_HOME=%PF_BIN%..
set PF_HOME_ESC=!PF_HOME: =%%20!
SetLocal DisableDelayedExpansion


REM Find usercount.jar, or we can't continue
set UCUJAR=%PF_BIN%usercount.jar
if exist "%UCUJAR%" goto FOUND_UCU_JAR
echo Could not locate %UCUJAR%. Please check that you are in the bin directory when running this script.
goto END

:FOUND_UCU_JAR

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

set MINIMUM_JAVA_VERSION=1.7
if %MINIMUM_JAVA_VERSION% GTR %JAVA_PRODUCT_VERSION% goto WRONG_JAVA_PRODUCT_VERSION
goto SETVARS

:WRONG_JAVA_PRODUCT_VERSION
echo JDK/JRE %MINIMUM_JAVA_VERSION% or higher is required to run PingFederate but %JAVA_PRODUCT_VERSION% was detected. Please set the JAVA_HOME environment variable to a JDK/JRE %MINIMUM_JAVA_VERSION% or higher installation directory path.
exit /B 1

:SETVARS
set PF_DEFAULT_DIR=%PF_HOME%\server\default

set UCU_CLASSPATH=%UCUJAR%
set UCU_CLASSPATH=%UCU_CLASSPATH%;%PF_DEFAULT_DIR%\lib\*
set UCU_CLASSPATH=%UCU_CLASSPATH%;%PF_DEFAULT_DIR%\deploy\*
set UCU_CLASSPATH=%UCU_CLASSPATH%;%PF_DEFAULT_DIR%\conf\"

set RUN_PROPERTIES=""
if exist "%PF_BIN%run.properties" (
	set RUN_PROPERTIES=%PF_BIN%run.properties
) ELSE (
	echo Missing %PF_HOME%\bin\run.properties; using defaults.
)

set UCU_LOG_LEVEL=""

"%JAVA%" ^
    %JAVA_OPTS% ^
     -Drun.properties="%RUN_PROPERTIES%" ^
     -Dlog4j.configurationFile="log4j.properties" ^
     -Dpf.server.default.dir="%PF_DEFAULT_DIR%" ^
     -Dpf.home="%PF_HOME%" ^
     -Ducu.log.level=%UCU_LOG_LEVEL% ^
     -cp "%UCU_CLASSPATH%" com.pingidentity.pf.support.ucu.UserCountUtility %*
