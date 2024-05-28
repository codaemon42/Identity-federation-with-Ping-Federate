#!/usr/bin/env bash

DIRNAME=`dirname $0`
PROGNAME=`basename $0`
GREP="grep"

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
linux=false;
sunos=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    Darwin*)
        darwin=true
        ;;
        
    Linux*)
        linux=true
        ;;
        
    SunOS*)
        sunos=true
        ;;        
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$PF_HOME" ] &&
        PF_HOME=`cygpath --unix "$PF_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Setup PF_HOME
if [ "x$PF_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    PF_HOME=`cd $DIRNAME/..; pwd`
fi
export PF_HOME

# Set PF_HOME_ESC - this is PF_HOME but with spaces that are replaced with %20
PF_HOME_ESC=${PF_HOME// /%20}


# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
	    JAVA="$JAVA_HOME/bin/java"
    else
	    JAVA="java"
        warn "JAVA_HOME is not set.  Unexpected results may occur."
        warn "Set JAVA_HOME to the directory of your local JDK or JRE to avoid this message."
    fi
fi

# Setup the classpath
ucujar="$PF_HOME/bin/usercount.jar"
if [ ! -f "$ucujar" ]; then
    die "Missing required file: $ucujar"
fi

PF_DEFAULT_DIR="$PF_HOME/server/default"

UCU_CLASSPATH="$ucujar"
UCU_CLASSPATH="$UCU_CLASSPATH:$PF_DEFAULT_DIR/lib/*"
UCU_CLASSPATH="$UCU_CLASSPATH:$PF_DEFAULT_DIR/deploy/*"
UCU_CLASSPATH="$UCU_CLASSPATH:$PF_DEFAULT_DIR/conf"

JAVA_FULL_VERSION=$("$JAVA" -version 2>&1 | grep "version" | head -n 1 | cut -d\" -f 2)
JAVA_MINOR_VERSION=`/bin/echo ${JAVA_FULL_VERSION} | cut -d "." -f1`

if [ "$JAVA_MINOR_VERSION" -eq "1" ]; then
    JAVA_MINOR_VERSION=`/bin/echo ${JAVA_FULL_VERSION} | cut -d "." -f2`
fi

if [ "$JAVA_MINOR_VERSION" -le "6" ]; then
  /bin/echo "The UCU must be run using Java 1.7 or higher.  Exiting."
    exit 1
fi

JAVA_OPTS=""
# java 17 support
if [[ $JAVA_MINOR_VERSION -eq "17" ]]; then
  JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.lang=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-opens=java.base/java.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.x509=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.net.util=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.pkcs=ALL-UNNAMED"
  JAVA_OPTS="$JAVA_OPTS --add-exports=java.base/sun.security.pkcs10=ALL-UNNAMED"
fi

# Check for run.properties (used by PingFederate to configure ports, etc.)
runprops="$PF_HOME/bin/run.properties"
if [ ! -f "$runprops" ]; then
    warn "Missing run.properties; using defaults."
    runprops=""
fi

# Setup UCU_LOG_LEVEL
if [ "x$UCU_LOG_LEVEL" = "x" ]; then
    UCU_LOG_LEVEL=""
fi


# Execute the JVM
"$JAVA" \
    $JAVA_OPTS \
        -Drun.properties="$runprops" \
		    -Dlog4j.configurationFile="log4j.properties" \
        -Dpf.home="$PF_HOME" \
        -Dpf.server.default.dir="$PF_DEFAULT_DIR" \
        -Ducu.log.level="$UCU_LOG_LEVEL" \
        -classpath "$UCU_CLASSPATH" \
        com.pingidentity.pf.support.ucu.UserCountUtility "$@"
