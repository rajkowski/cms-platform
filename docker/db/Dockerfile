# Pull official base image
FROM postgres:18

# Update the system and install PostGIS
RUN apt-get update \
    && apt-get install wget -y \
    && apt-get install postgresql-18-postgis-3 -y \
    && apt-get install postgis -y

# Run init.sql
COPY ./init.sql /docker-entrypoint-initdb.d/
