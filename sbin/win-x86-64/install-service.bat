@echo off
rem Copyright (C) 2020 Ping Identity Corporation
rem All rights reserved.

if not "%JAVA_HOME%" == "" goto TEST_VERSION
echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto :eof

:TEST_VERSION
set JAVA_VERSION=
"%JAVA_HOME%/bin/java" -version 2>java_version.txt
for /f "tokens=3" %%g in (java_version.txt) do (
    del java_version.txt
    set JAVA_VERSION=%%g
    set JAVA=%JAVA_HOME%/bin/java
    goto CHECK_JAVA_VERSION
)

rem grab first 3 characters of version number (ex: 1.6) and compare against required version
:CHECK_JAVA_VERSION
set JAVA_VERSION17=%JAVA_VERSION:~1,2%
if %JAVA_VERSION17% == 17 (
    goto install
)
set JAVA_VERSION11=%JAVA_VERSION:~1,2%
if %JAVA_VERSION11% == 11 (
    goto install
)
set JAVA_VERSION8=%JAVA_VERSION:~1,3%
if %JAVA_VERSION8% == 1.8 (
    goto install
)

:WRONG_JAVA_VERSION
echo JDK 8, 11 or 17 is required to run PingFederate but %JAVA_VERSION% was detected. Please set the JAVA_HOME environment variable to a JDK 8, 11 or 17 installation directory path.
goto install

:install
copy ..\wrapper\InstallPingFederateService.bat InstallPingFederateServiceTmp.bat > out.txt
call InstallPingFederateServiceTmp.bat
del  InstallPingFederateServiceTmp.bat
del  out.txt
