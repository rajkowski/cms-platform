# Pull official base image
FROM postgres:15

# Update the system and install PostGIS
RUN apt-get update \
    && apt-get install wget -y \
    && apt-get install postgresql-15-postgis-3 -y \
    && apt-get install postgis -y

# Run init.sql
COPY ./init.sql /docker-entrypoint-initdb.d/
