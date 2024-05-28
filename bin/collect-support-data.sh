#!/usr/bin/env sh

pf_root=$(cd "$(dirname "$0")/.."; pwd)
csd_config_path="${pf_root}/server/default/conf/collect-support-data"

for jar in "${pf_root}"/bin/csd/lib/*.jar
do
  if [ -f "$jar" ]
  then
    CLASSPATH="${CLASSPATH}:${jar}"
  fi
done

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        if ! [ -n "$JAVA_HOME" ] || ! [ -x "$JAVA_HOME/bin/java" ];  then
            echo "No executable java found in JAVA_HOME, please correct and try running the tool again."
            exit 1
        fi
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
        echo "JAVA_HOME is not set.  Unexpected results may occur."
        echo "Set JAVA_HOME to the directory of your local JDK to avoid this message."
    fi
fi

"$JAVA" -Dcsd.config.path="$csd_config_path" -cp "$CLASSPATH" -Xmx1g com.pingidentity.csd.server.tools.CollectSupportData --productType PingFederate --serverRoot "${pf_root}" "${@}"