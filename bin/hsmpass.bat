@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0

set ROOT=%DIRNAME%..
set ROOT_LIB=%ROOT%\lib
set DEFAULT_DIR=%ROOT%\server\default
set DEFAULT_CONF_DIR=%DEFAULT_DIR%\conf

REM Read an optional running configuration file
@if ["%RUN_CONF%"] == [""] (
    set "RUN_CONF=%~dsp0%conf.bat"
)
@if exist "%RUN_CONF%" (
    call "%RUN_CONF%"
)

set PASSWORD_FILE=%ROOT%\server\default\data\hsmpasswd.txt

set CLASSPATH=%ROOT_LIB%\*;%DIRNAME%\*;%DEFAULT_DIR%\lib\*;%DEFAULT_CONF_DIR%\;%DEFAULT_CONF_DIR%\generated-hivemodule\"

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
    -Dlog4j.configurationFile="file:///%DIRNAME%\hsmpass.log4j2.xml" ^
    -Dpf.server.default.dir="%DEFAULT_DIR%" ^
    -Dpassword.file="%PASSWORD_FILE%" ^
    -classpath "%CLASSPATH%" com.pingidentity.console.PasswordChanger HSM %*

goto :eof