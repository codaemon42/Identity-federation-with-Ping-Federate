@echo off
setlocal

rem Copyright (c) 1999, 2008 Tanuki Software, Inc.
rem http://www.tanukisoftware.com
rem All rights reserved.
rem
rem This software is the proprietary information of Tanuki Software.
rem You shall use it only in accordance with the terms of the
rem license agreement you entered into with Tanuki Software.
rem http://wrapper.tanukisoftware.org/doc/english/licenseOverview.html
rem
rem Java Service Wrapper general NT service install script.
rem Optimized for use with version 3.3.1-st of the Wrapper.
rem

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
if not "%JAVA_HOME%" == "" goto install
echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK to avoid this message.
goto :eof

:install
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set "_REALPATH=%~dp0"

rem Decide on the wrapper binary.
set _WRAPPER_BASE=pingfederate
set "_WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe"
if exist "%_WRAPPER_EXE%" goto conf
set "_WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe"
if exist "%_WRAPPER_EXE%" goto conf
set "_WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%-windows-ia-64.exe"
if exist "%_WRAPPER_EXE%" goto conf
set "_WRAPPER_EXE=%_REALPATH%%_WRAPPER_BASE%.exe"
if exist "%_WRAPPER_EXE%" goto conf
echo Unable to locate a Wrapper executable using any of the following names:
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-32.exe
echo %_REALPATH%%_WRAPPER_BASE%-windows-x86-64.exe
echo %_REALPATH%%_WRAPPER_BASE%.exe
goto :eof

rem
rem Find the PingFederateService.conf
rem
:conf
set "_WRAPPER_CONF=%~f1"
if not "%_WRAPPER_CONF%"=="" goto startup

rem On first install, inspect system memory and CPUs with memoryoptions.bat.  This generates an optimized
rem jvm-memory.options file.  On upgrade, jvm-memory.options will be migrated from the source PingFederate.
rem A call to generate-wrapper-jvm-options.bat is always made, which uses jvm-memory.options to generate all
rem the java options for PingFederate

rem generate jvm-memory.options file if not an upgrade
if not "%PF_UPGRADE%"=="true" call "%_REALPATH%..\..\bin\memoryoptions.bat" || goto :error

rem read jvm-memory.options file and generate wrapper-jvm-options.conf
call "%_REALPATH%../wrapper/generate-wrapper-jvm-options.bat" || goto :error

set _WRAPPER_CONF="%_REALPATH%../wrapper/PingFederateService.conf"

rem
rem Install the Wrapper as an NT service.
rem
:startup
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF%
exit /b

:error
echo Unable to generate wrapper-jvm-options.conf. An error occurred.
exit /b 1

if not errorlevel 1 goto :eof


