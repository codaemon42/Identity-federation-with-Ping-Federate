#!/usr/bin/env bash

DIRNAME=$(dirname "$0")/..
DIRNAME=$( (cd "$DIRNAME" && pwd))
# Set DIRNAME_ESC - this is DIRNAME but with spaces that are replaced with %20
DIRNAME_ESC=${DIRNAME// /%20}

BIN=$DIRNAME/bin
# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
  RUN_CONF="$BIN/run.conf"
fi
if [ -r "$RUN_CONF" ]; then
  . "$RUN_CONF"
fi

# Check for run.properties
runprops="$BIN/run.properties"
if [ ! -f "$runprops" ]; then
  warn "Missing run.properties; using defaults."
  runprops=""
fi

SERVER_DEFAULT=$DIRNAME/server/default

CLASSPATH="$BIN/pf-consoleutils.jar:$BIN/pf-startup.jar:$SERVER_DEFAULT/lib/*:$SERVER_DEFAULT/conf/generated-hivemodule/"

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
  if [ "x$JAVA_HOME" != "x" ]; then
    JAVA="$JAVA_HOME/bin/java"
  else
    JAVA="java"
    echo "JAVA_HOME is not set.  Unexpected results may occur."
    echo "Set JAVA_HOME to the directory of your local JDK to avoid this message."
  fi
fi

JAVA_OPTS=""
JAVA_VERSION=$("$JAVA" -version 2>&1 | grep "version" | head -n 1 | cut -d\" -f 2)
JAVA_MAJOR_VERSION=$(echo ${JAVA_VERSION/_/.} | cut -d. -f 1)
# java 17 support
if [[ $JAVA_MAJOR_VERSION -eq "17" ]]; then
  JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.x509=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.net.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.pkcs=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED"
fi

"$JAVA" \
  $JAVA_OPTS \
  -cp "$CLASSPATH" \
  -Dpingfederate.log.dir="$DIRNAME/log" \
  -Dpf.server.default.dir="$DIRNAME/server/default" \
  -Dlog4j.configurationFile="file:///$DIRNAME_ESC/bin/logfilter.log4j2.xml" \
  -Drun.properties="$runprops" \
  com.pingidentity.console.logfilter.utility.LogFilterUtility "$@"
