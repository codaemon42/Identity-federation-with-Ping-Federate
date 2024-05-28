#!/usr/bin/env bash
### ====================================================================== ###
##                                                                          ##
## PingFederate Status Check Script                                         ##
##                                                                          ##
### ====================================================================== ###

DIRNAME=`dirname "$0"`

PF_IS_RUNNING_MSG="PingFederate is running"
PF_IS_STOPPED_MSG="PingFederate is stopped"

cygwin=false;
sunos=false;
case "`uname`" in
    CYGWIN*)
        cygwin=true
        ;;

    SunOS*)
        sunos=true
        ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$PF_HOME" ] && PF_HOME=`cygpath --unix "$PF_HOME"`
fi

# Setup PF_HOME
if [ "x$PF_HOME" = "x" ]; then
    # get the full path (without any relative bits)
    PF_HOME=`cd "$DIRNAME/.."; pwd`
fi
export PF_HOME

# Check for currently running instance of PingFederate
RUNFILE="$PF_HOME/bin/pingfederate.pid"

if $sunos ; then
    if [ ! -f "$RUNFILE" ] ; then
        /bin/echo ${PF_IS_STOPPED_MSG}
        exit 0
    fi
else
    if [ ! -e "$RUNFILE" ] ; then
        /bin/echo ${PF_IS_STOPPED_MSG}
        exit 0
    fi
fi

CURRENT_PID=`cat "$RUNFILE"`
if [ -n "$CURRENT_PID" ] ; then
    kill -0 ${CURRENT_PID} 2>/dev/null
    if [ $? -ne 0 ] ; then
        /bin/echo ${PF_IS_STOPPED_MSG}
        exit 0
    else
        /bin/echo ${PF_IS_RUNNING_MSG}
        exit 1
    fi
else
    /bin/echo ${PF_IS_STOPPED_MSG}
    exit 0
fi