@echo off
if not "%JAVA_HOME%" == "" goto ADD_JAVA
set JAVA=java
echo JAVA_HOME is not set.  Unexpected results may occur.
echo Set JAVA_HOME to the directory of your local JDK or JRE to avoid this message.
goto SETVARS

:ADD_JAVA

set JAVA=%JAVA_HOME%\bin\java

:SET_VARS

setlocal
set BIN_DIRNAME=%~dp0
set PF_HOME=%BIN_DIRNAME%..
set CSD_CONFIG_PATH=%PF_HOME%\server\default\conf\collect-support-data
set CSD_LIB_DIR=%PF_HOME%\bin\csd\lib
set CLASSPATH=%CSD_LIB_DIR%\*

"%JAVA%" -Dcsd.config.path="%CSD_CONFIG_PATH%" -cp "%CLASSPATH%" -Xmx1g com.pingidentity.csd.server.tools.CollectSupportData --productType PingFederate --serverRoot "%PF_HOME%" %*