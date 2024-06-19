## With Tomcat
## CATALINA_HOME:   /usr/local/tomcat
# ant package
# docker build -f docker/app/Dockerfile -t cms-platform:latest .
# docker image ls
# docker run -p 80:8080 --env-file=.env cms-platform:latest

FROM tomcat:9.0-jdk21
COPY ./target/cms-platform.war /usr/local/tomcat/webapps/ROOT.war
ENV CATALINA_OPTS="-XX:InitialRAMPercentage=10 -XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 CMD curl -I -f --max-time 5 --header "X-Monitor: healthcheck" http://localhost:8080 || exit 1
CMD ["catalina.sh", "run"]
