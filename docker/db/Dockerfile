# Pull official base image
FROM postgres:18

# Update the system and install extensions
RUN apt-get update \
    && apt-get install -y wget \
    && apt-get install -y postgresql-18-postgis-3 \
    && apt-get install -y postgis \
    && apt-get install -y postgresql-18-pgvector

# Run init.sql
COPY ./init.sql /docker-entrypoint-initdb.d/
