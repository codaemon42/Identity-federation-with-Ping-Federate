#!/usr/bin/env bash

DIRNAME=`dirname $0`/..
DIRNAME=`(cd $DIRNAME && pwd)`
# Set DIRNAME_ESC - this is DIRNAME but with spaces that are replaced with %20
DIRNAME_ESC=${DIRNAME// /%20}
PROGNAME=`basename $0`
GREP="grep"

# Use the maximum available, or set MAX_FD != -1 to use that
MAX_FD="maximum"

#
# Helper to complain.
#
warn() {
  echo "${PROGNAME}: $*"
}

#
# Helper to fail.
#
die() {
  warn $*
  exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*)
    cygwin=true
    ;;

  Darwin*)
    darwin=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$JBOSS_HOME" ] &&
    JBOSS_HOME=`cygpath --unix "$JBOSS_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$JAVAC_JAR" ] &&
    JAVAC_JAR=`cygpath --unix "$JAVAC_JAR"`
fi

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
  if [ "x$JAVA_HOME" != "x" ]; then
    JAVA="$JAVA_HOME/bin/java"
    echo
  else
    JAVA="java"
    warn "JAVA_HOME is not set.  Please set the JAVA_HOME environment variable to a JDK directory path."
    exit 1
  fi
fi

# check java version
JAVA_VERSION_STRING=`"$JAVA" -version 2>&1 | head -1 | cut -d '"' -f2`
javaSupportedVersion=0

case "$JAVA_VERSION_STRING" in
  1.8*|11|11.*|17|17.*)     # Supported java versions
    javaSupportedVersion=1
    ;;

  *)                        # Unsupported java versions
    javaSupportedVersion=0
    ;;
esac

if [[ $javaSupportedVersion == 0 ]]; then
  echo ""
  echo "!! WARNING !!"
  echo "Java version ${JAVA_VERSION_STRING} is not supported for running PingFederate. Please install Java 8, 11 or 17."
  echo ""
fi

CLASSPATH="$DIRNAME/lib/*"

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
  -Xms128m -Xmx128m \
  $JAVA_OPTS \
  -Dupgrade.home.dir="$DIRNAME" \
  -Dlog.dir="$DIRNAME/log" \
  -Dlog4j.configurationFile="file:///$DIRNAME_ESC/bin/log4j2.xml" \
  -cp "$CLASSPATH" com.pingidentity.pingfederate.migration.UpgradeUtility "$@"
