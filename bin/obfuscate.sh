#!/usr/bin/env bash

if [ $# -eq 1 ]; then
  if [ $1 = "-l" ]; then
    echo "$0 -l: missing password"
    echo "Try '$0 --help' for more information."
    exit 1
  fi
fi

if [ "$1" = "--help" ]; then
  echo 'Usage:' $(basename $0) '[-l PASSWORD] [PASSWORD]'
  echo 'Prompts for a password, obfuscates it, then prints the result.'
  echo 'To avoid prompting, a PASSWORD argument may be specified. However, this is generally less secure.'
  echo 'If PASSWORD contains special characters, it should be enclosed in single quotes.'
  echo
  echo '  -l, Obfuscate using the legacy AES algorithm.'
  exit 1
fi

if [ $# -ge 3 ]; then
  echo "Validation Error: Invalid arguments."
  echo "Try '$0 --help' for more information."
  exit 1
fi

if [ $# -eq 2 ]; then
  if [ $1 != "-l" ]; then
    echo "Validation Error: Invalid arguments."
    echo "Try '$0 --help' for more information."
    exit 1
  fi
fi

DIRNAME=$(dirname "$0")/..
DIRNAME=$(cd "$DIRNAME" && pwd)
# Set DIRNAME_ESC - this is DIRNAME but with spaces that are replaced with %20
DIRNAME_ESC=${DIRNAME// /%20}
DEFAULT_DIR="$DIRNAME/server/default"
DEFAULT_CONF_DIR="$DEFAULT_DIR/conf"

# Read an optional running configuration file
if [ "x$RUN_CONF" = "x" ]; then
  RUN_CONF="$DIRNAME/bin/run.conf"
fi
if [ -r "$RUN_CONF" ]; then
  . "$RUN_CONF"
fi

# Check for run.properties
runprops="$DIRNAME/bin/run.properties"
if [ ! -f "$runprops" ]; then
  warn "Missing run.properties; using defaults."
  runprops=""
fi

CLASSPATH="$CLASSPATH:$DIRNAME/bin/pf-startup.jar"
CLASSPATH="$CLASSPATH:$DEFAULT_DIR/lib/*"
CLASSPATH="$CLASSPATH:$DIRNAME/lib/*"
CLASSPATH="$CLASSPATH:$DEFAULT_DIR/deploy/*"
CLASSPATH="$CLASSPATH:$DEFAULT_CONF_DIR/"
CLASSPATH="$CLASSPATH:$DEFAULT_CONF_DIR/generated-hivemodule/"

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
  -Dlog4j.configurationFile="file:///$DIRNAME_ESC/bin/obfuscate.log4j2.xml" \
  -Drun.properties="$runprops" \
  -Dpf.server.default.dir="$DEFAULT_DIR" \
  -cp "$CLASSPATH" \
  com.pingidentity.crypto.PasswordEncoder "$@"
