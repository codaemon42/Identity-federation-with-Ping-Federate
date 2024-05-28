#!/usr/bin/env bash

PF_HOME=$(dirname "$0")/..
PF_HOME=$(cd "$PF_HOME" && pwd)
# Set PF_HOME_ESC - this is PF_HOME but with spaces that are replaced with %20
PF_HOME_ESC=${PF_HOME// /%20}

cd "$PF_HOME"

LIB=lib

SERVER=server/default
SERVER_LIB=$SERVER/lib
SERVER_DEPLOY=$SERVER/deploy
SERVER_DATA=$SERVER/data
SERVER_CONF=$SERVER/conf

PF_ENDORSED_DIRS=$LIB/endorsed

CLASSPATH=$SERVER_CONF:$SERVER/data/config-store:$PF_HOME/bin/pf-startup.jar:$SERVER_CONF/generated-hivemodule/

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
  RUN_CONF="$PF_HOME/bin/run.conf"
fi
if [ -r "$RUN_CONF" ]; then
  . "$RUN_CONF"
fi

for jar in $(ls $LIB/*.jar $LIB/*.zip 2>/dev/null); do
  CLASSPATH=$CLASSPATH:$jar
done

for jar in $(ls $SERVER_LIB/*.jar $SERVER_LIB/*.zip 2>/dev/null); do
  CLASSPATH=$CLASSPATH:$jar
done

for jar in $(ls $SERVER_DEPLOY/*.jar $SERVER_DEPLOY/*.zip 2>/dev/null); do
  CLASSPATH=$CLASSPATH:$jar
done

if [ ! -d log ]; then
  mkdir log
fi

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

JAVA_VERSION=$("$JAVA" -version 2>&1 | grep "version" | head -n 1 | cut -d\" -f 2)
JAVA_MAJOR_VERSION=$(echo ${JAVA_VERSION/_/.} | cut -d. -f 1)

ENDORSED_DIRS_FLAG=$()
if [ "$JAVA_MAJOR_VERSION" = "1" ]; then
  ENDORSED_DIRS_FLAG='-Djava.endorsed.dirs="$PF_ENDORSED_DIRS"'
fi

# Check for run.properties (used by PingFederate to configure ports, etc.)
runprops="$PF_HOME/bin/run.properties"
if [ ! -f "$runprops" ]; then
  warn "Missing run.properties; using defaults."
  runprops=""
fi

JAVA_OPTS=""
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
  $ENDORSED_DIRS_FLAG \
  -Dpf.home="$PF_HOME" \
  -Dpf.server.default.dir="$SERVER" \
  -Dpf.server.data.dir="$SERVER_DATA" \
  -Dlog4j.configurationFile="file:///$PF_HOME_ESC/bin/provmgr.log4j2.xml" \
  -Drun.properties="$runprops" \
  com.pingidentity.provisioner.cli.CommandLineTool "$@"