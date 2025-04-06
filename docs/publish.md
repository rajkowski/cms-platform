# Publish

## Local Docker Container Image

```bash
docker build -t cms-platform:latest .
```

## Github Docker Container Image Registry

One-time setup to use docker buildx for multi-platform architecture builds:

```bash
docker buildx create --use
```

Build and push to Github container image registry:

```bash
export BUILD_TAG=20250405.10000
export MINOR_TAG=20250405
export MAJOR_TAG=1
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --push \
  -t ghcr.io/rajkowski/cms-platform:${BUILD_TAG} \
  -t ghcr.io/rajkowski/cms-platform:${MINOR_TAG} \
  -t ghcr.io/rajkowski/cms-platform:${MAJOR_TAG} \
  .
```

## PostgreSQL Image with PostGIS

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  --push \
  -t ghcr.io/rajkowski/cms-platform-db:15 \
  ./docker/db/
```
