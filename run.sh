: "${CATALINA_HOME:?CATALINA_HOME must be set to a Tomcat directory before running this script}"

CATALINA_BASE="${CATALINA_BASE:-$PWD/out/exploded}"
export CATALINA_BASE

sh "$CATALINA_HOME/bin/catalina.sh" run
