#!/usr/bin/env bash
### ====================================================================== ###
##                                                                          ##
##  PingFederate Shutdown Script                                            ##
##                                                                          ##
### ====================================================================== ###

DIRNAME=`dirname "$0"`

# OS specific support (must be 'true' or 'false').
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
    [ -n "$PF_HOME" ] &&
        PF_HOME=`cygpath --unix "$PF_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Setup PF_HOME
if [ "x$PF_HOME" = "x" ]; then
    PF_HOME=`cd "$DIRNAME/.."; pwd`
fi

# Check for currently running instance of PingFederate
RUNFILE="$PF_HOME/bin/pingfederate.pid"

if $sunos ; then
	if [ ! -f "$RUNFILE" ] ; then
		/bin/echo "PingFederate is not currently running. Exiting."
		exit 0
	fi
else
	if [ ! -e "$RUNFILE" ] ; then
		/bin/echo "PingFederate is not currently running. Exiting."
		exit 0
	fi
fi

CURRENT_PID=`cat "$RUNFILE"`

if [ -n "$CURRENT_PID" ] ; then 
    kill -0 $CURRENT_PID 2>/dev/null
    if [ $? -ne 0 ] ; then
		/bin/echo "PingFederate is not currently running. Exiting."
		exit 0
    fi
    kill $CURRENT_PID 2>/dev/null
    if [ $? -eq 0 ] ; then
		if $sunos ; then
			/bin/echo "The PingFederate server is shutting down . . ."
			ps -p $CURRENT_PID 1>/dev/null 2>/dev/null
			
			while [ $? -eq 0 ]
			do
				/bin/echo  "."
				sleep 1
				ps -p $CURRENT_PID 1>/dev/null 2>/dev/null
			done
		else
			/bin/echo -n "The PingFederate server is shutting down . . ."
			ps $CURRENT_PID 1>/dev/null 2>/dev/null
			
			while [ $? -eq 0 ]
			do
				/bin/echo  -n " ."
				sleep 1
				ps $CURRENT_PID 1>/dev/null 2>/dev/null
			done
		fi
		
		/bin/echo " done."
		exit 0
	else
		/bin/echo "Unable to shutdown currently running PingFederate server. Exiting."
		exit 1
    fi
else
	/bin/echo "PingFederate is not currently running. Exiting."
	exit 0
fi
