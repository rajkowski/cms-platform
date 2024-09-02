# Pull official base image
FROM postgres:15

# Install PostGIS
RUN apt-get update \
    && apt-get install wget -y \
    && apt-get install postgresql-15-postgis-3 -y \
    && apt-get install postgis -y

# Run init.sql
ADD ./docker/db/init.sql /docker-entrypoint-initdb.d/
