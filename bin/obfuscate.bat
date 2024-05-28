@echo off

@if not "%ECHO%" == ""  echo %ECHO%
@if "%OS%" == "Windows_NT"  setlocal

set /a argCount = 0
for %%a in (%*) do set /a argCount +=1

@if %argCount% EQU 1 (
   if "%~1" == "-l" (
      echo obfuscate.bat -l: missing password
      echo Try 'obfuscate.bat --help' for more information.
      exit /b
   )
)

@if "%~1"=="--help" (
    echo Usage: obfuscate.bat [-l password] [password]
    echo Prompts for a password, obfuscates it, then prints the result.
    echo To avoid prompting, a PASSWORD argument may be specified. However, this is generally less secure.
    echo If PASSWORD contains special characters, it should be enclosed in double quotes.
    echo.
    echo   -l, Obfuscate using the legacy AES algorithm.
    exit /b
)

@if %argCount% GEQ 3 (
   echo Validation Error: Invalid Arguments.
   echo Try 'obfuscate.bat --help' for more information.
   exit /b
)

@if %argCount% EQU 2 (
   if not "%~1" == "-l" (
      echo Validation Error: Invalid Arguments.
      echo Try 'obfuscate.bat --help' for more information.
      exit /b
   )
)

set DIRNAME=.\
if "%OS%" == "Windows_NT" set DIRNAME=%~dp0
set DIRNAME=%DIRNAME%..

set DEFAULT_DIR=%DIRNAME%\server\default
set DEFAULT_CONF_DIR=%DEFAULT_DIR%\conf

set CLASSPATH=%DEFAULT_DIR%\lib\*;.
set CLASSPATH=%CLASSPATH%;%DIRNAME%\lib\*
set CLASSPATH=%CLASSPATH%;%DIRNAME%\bin\pf-startup.jar
set CLASSPATH=%CLASSPATH%;%DEFAULT_DIR%\deploy\*
set CLASSPATH=%CLASSPATH%;%DEFAULT_CONF_DIR%\
set CLASSPATH=%CLASSPATH%;%DEFAULT_CONF_DIR%\generated-hivemodule\"

REM Read an optional running configuration file
@if ["%RUN_CONF%"] == [""] (
    set RUN_CONF=%dsp0%conf.bat
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

@if ["%JAVA_HOME%"] == [""] (
    set JAVA=java
    echo JAVA_HOME is not set.  Unexpected results may occur.
    echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
) else (
    set JAVA=%JAVA_HOME%\bin\java
)

REM If password contains special characters (e.g. '&'), an extra pair of quotes is required.
@if "%~2" == "" (
    set ARGS=^"%1^"
) else (
    set ARGS=%1 ^"%2^"
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
    -Dlog4j.configurationFile="file:///%DIRNAME%\bin\obfuscate.log4j2.xml" ^
    -Drun.properties="%RUN_PROPERTIES%" ^
    -Dpf.server.default.dir="%DEFAULT_DIR%" ^
    -cp "%CLASSPATH%" com.pingidentity.crypto.PasswordEncoder %ARGS%

goto :eof