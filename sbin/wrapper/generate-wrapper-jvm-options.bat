@echo off
REM This script reads from the bin\jvm-memory.options file and generates
REM the sbin\wrapper\wrapper-jvm-options.conf used by the Tanuki service
REM wrapper to set all the JVM options

setlocal enabledelayedexpansion

set "pfHome=%~dp0..\.."
set "jvmMemoryOptions=%pfHome%\bin\jvm-memory.options"
set "wrapperOptions=%pfHome%\sbin\wrapper\wrapper-jvm-options.conf"

REM ********* File Template *********
REM Each line must end with a "^" and be separated by a space
set file=^
#encoding=UTF-8
REM ******* End File Template *******

REM check for existence of jvm-memory.options and generate a new one if it doesn't exist
if not exist "%jvmMemoryOptions%" call "%pfHome%\bin\memoryoptions.bat"

REM set garbage collection log file
set GC_FILE=..\..\log\jvm-garbage-collection.log

REM set garbage collection log file rotation count
set GC_FILE_COUNT=3

REM set individual garbage collection log file size
set GC_FILE_SIZE=1m

REM set garbage collection log jvm options based on java product version
if NOT EXIST "..\..\log" (
    md "..\..\log"
)
for /f tokens^=2-5^ delims^=.-_^+^" %%j in ('"%JAVA_HOME%/bin/java.exe" -fullversion 2^>^&1') do (
    set "JAVA_PRODUCT_VERSION=%%j"
    if !JAVA_PRODUCT_VERSION! EQU 1 (
        set "JAVA_PRODUCT_VERSION=%%k"
    )
)
if !JAVA_PRODUCT_VERSION! LEQ 8 (
    set GC_OPTION=-XX:+PrintGCDetails -Xloggc:^"%GC_FILE%^" -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=%GC_FILE_COUNT% -XX:GCLogFileSize=%GC_FILE_SIZE%
) else (
    set GC_OPTION=-Xlog:gc*:file=^"%GC_FILE%^":uptime,time,level,tags:filecount=%GC_FILE_COUNT%,filesize=%GC_FILE_SIZE%
)

REM read the jvm-memory.options file and add as JVM options
for /f "usebackq tokens=* eol=#" %%o in ("%jvmMemoryOptions%") do (
    REM ********* File Template *********
    REM line spacing is important
    set file=!file!^

%%o
REM ******* End File Template *******
)

REM java 17 support
if !JAVA_PRODUCT_VERSION! == 17 (
    set ADD_OPEN_LANG=--add-opens=java.base/java.lang=ALL-UNNAMED
    set ADD_OPEN_UTIL=--add-opens=java.base/java.util=ALL-UNNAMED
    set ADD_EXPORTS_X509=--add-exports=java.base/sun.security.x509=ALL-UNNAMED
    set ADD_EXPORTS_UTIL=--add-exports=java.base/sun.security.util=ALL-UNNAMED
    set ADD_EXPORTS_NAMING=--add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED
    set ADD_EXPORTS_SUN_NET_UTIL=--add-exports=java.base/sun.net.util=ALL-UNNAMED
    set ADD_EXPORTS_PKCS=--add-exports=java.base/sun.security.pkcs=ALL-UNNAMED
    set ADD_EXPORTS_PKCS10=--add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED
    REM ********* File Template *********
    REM line spacing is important
    set file=!file!^

!ADD_OPEN_LANG!^

!ADD_OPEN_UTIL!^

!ADD_EXPORTS_X509!^

!ADD_EXPORTS_UTIL!^

!ADD_EXPORTS_NAMING!^

!ADD_EXPORTS_SUN_NET_UTIL!^

!ADD_EXPORTS_PKCS!^

!ADD_EXPORTS_PKCS10!
)

REM ********* File Template *********
REM line spacing is important
set file=!file!^

^
# Comment out to disable java garbage collection logs^

%GC_OPTION%^

^

# Comment out to disable java crash logs^

-XX:ErrorFile=..\..\log\java_error%%p.log^

^

# Uncomment the following to enable Memory Dumps^

# -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=..\..\log^

^

# Uncomment the following to enable debugging on the service^

# -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n^

^

# Add any other arguments that should be passed to the JVM here.

REM ******* End File Template *******

echo !file! > "%wrapperOptions%" || GOTO :ERROR
echo Generated new wrapper-jvm-options.conf
EXIT /B 

:ERROR
echo Unable to generate wrapper-jvm-options.conf. An error occurred.
EXIT /B 1