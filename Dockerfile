# Build layer
FROM mcr.microsoft.com/openjdk/jdk:21-mariner AS builder

ARG ANT_VERSION=1.10.15
ARG ANT_DOWNLOAD_SHA512=d78427aff207592c024ff1552dc04f7b57065a195c42d398fcffe7a0145e8d00cd46786f5aa52e77ab0fdf81334f065eb8011eecd2b48f7228e97ff4cb20d16c
ARG ANT_MIRROR=https://downloads.apache.org

RUN yum install -y tar

# Install Ant
RUN set -o errexit -o nounset \
    && echo "Downloading Ant" \
    && curl -fsL -o ant.tar.gz "${ANT_MIRROR}/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz" \
    && echo "Checking Ant download hash" \
    && echo "${ANT_DOWNLOAD_SHA512} ant.tar.gz" | sha512sum -c - \
    && echo "Extracting Ant" \
    && tar -zvxf ant.tar.gz -C /opt/ \
    && rm ant.tar.gz \
    && mv /opt/apache-ant-${ANT_VERSION} /opt/ant

ENV ANT_HOME=/opt/ant
ENV PATH=${PATH}:${ANT_HOME}/bin
RUN ant -version

# Build the application
COPY build.xml build.xml
COPY ./config config
COPY ./lib lib
COPY ./src src
# COPY ./web-customizations web-customizations
RUN ant package

# Runtime layer
FROM tomcat:9.0-jdk21

COPY --from=builder target/cms-platform.war /usr/local/tomcat/webapps/ROOT.war
ENV CATALINA_OPTS="-XX:InitialRAMPercentage=10 -XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=3 CMD curl -I -f --max-time 5 --header "X-Monitor: healthcheck" http://localhost:8080 || exit 1
CMD ["catalina.sh", "run"]
