---
id: developer-environment
title: Developer's Local Environment
# prettier-ignore
description: Options for locally developing CMS Platform
---

CMS Platform is meant to be fully developed offline. This allows developers to code, build, test, and run with the least friction when developing.

Developers can use [VS Code](https://code.visualstudio.com) with several included recommended extensions for a truly Open Source environment.

## Developing with Tomcat

The following steps will guide you through the developer tools and environment setup so that your code changes can be compiled and copied automatically and then seen in your web browser.

1. Install [OpenJDK 21+](https://learn.microsoft.com/en-us/java/openjdk/download)
2. Install [Apache Ant 1.10+](https://ant.apache.org) and configure your terminal's path with ANT_HOME/bin
3. Install [Apache Tomcat 9.x](https://tomcat.apache.org/download-90.cgi) into a directory of your choice
4. Install the PostgreSQL database server – natively on MacOS with [Postgres.app](https://postgresapp.com) or with a Docker container like (postgis/postgis:18-3.6-alpine)
5. Set environment variables for `CATALINA_HOME` to tomcat, `CMS_PATH` to a new folder for the CMS user attachments, and `DB_NAME` for the PostgreSQL database; see example below...
6. Clone the CMS Platform repo – `git clone https://github.com/rajkowski/cms-platform.git`
7. In the repo directory execute `ant deploy` – this compiles and updates code and library changes in a working Tomcat exploded webapp directory `./out/exploded/webapps/ROOT`
8. Start Tomcat and the web application using the included `run.sh` script

Minimal Environment Variables:

```ini
CATALINA_HOME=/path/to/tomcat
CATALINA_BASE=/path/to/cms-platform/out/exploded
CMS_PATH=/path/to/files/cms-platform
DB_NAME=cms-platform
CMS_ADMIN_USERNAME=user@example.com
CMS_ADMIN_PASSWORD=test
```

If not specified, the path for file assets and external configuration on Linux is `/opt/cms-platform`; otherwise `$USER_HOME/Web/cms-platform`

Use `ant deploy` to further update the web application with your changes. In debug mode, many Javascript, JSP, and XML files are automatically reloaded. If there are newly compiled Java files, libraries, or database migration scripts, then restart Tomcat.

### Ask AI

Enable the `ai.enabled` and `ai.askai.enabled` site properties and set `ai.askai.control` to `drawer`. Configure the application process with:

```ini
CMS_AI_LLM_OLLAMA_URL=http://localhost:11434
CMS_AI_LLM_OLLAMA_MODEL=mistral:7b
# CMS_AI_LLM_OLLAMA_API_KEY=optional-bearer-token
```

Pull the model on the Ollama server (`ollama pull mistral:7b`). `OLLAMA_URL` is required; `OLLAMA_MODEL` defaults to `mistral:7b`. The server receives each question and up to six permission- and region-filtered search results, so use a trusted Ollama server. Answers are generated independently from the retrieved content and may be incomplete or incorrect. Search falls back to keyword matching when semantic search is unavailable.

### Hybrid Search

Unified search combines PostgreSQL full-text results with semantic matches when the `pgvector` extension and search vector migrations are installed. Provide the Granite small English embedding model's `model.onnx` and `tokenizer.json` in `./models/granite-embedding-small-english-r2`, relative to the application file library directory, and set `CMS_EMBEDDING_MODEL` to their directory. The background scheduler must run cluster jobs to index content; it refreshes search vectors every five minutes. Until indexing completes, or when the model or vector search is unavailable, search uses full-text matching.

To rerank Ask AI search results, set `CMS_RERANK_MODEL` in the application process to a directory containing `model.onnx` and `tokenizer.json` for an ONNX BERT cross-encoder scoring model. This must be a reranking model, not the Granite embedding model. The first 8,000 characters of each candidate's source text are scored in overlapping passages, and up to six highest-scoring nonempty results are sent to the language model. Without the model files, or if scoring fails, Ask AI retains the original search ranking and excludes empty results. In containers, mount the model directory and set the environment variable to its path inside the container.

Set `CMS_RERANK_MODEL` to a reranking model like `./models/granite-embedding-reranker-english-r2`.

## Developer Resources

- [CMS Platform](https://github.com/rajkowski/cms-platform)
- [Java 21 SDK Documentation](https://docs.oracle.com/en/java/javase/21/)
- [MVC Example with Servlets and JSP](https://www.baeldung.com/mvc-servlet-jsp)
- [Servlet 4.0 API](https://tomcat.apache.org/tomcat-9.0-doc/servletapi/index.html)
- [JSP 2.3 API](https://tomcat.apache.org/tomcat-9.0-doc/jspapi/index.html)
- [JSTL 1.2.5 API](https://github.com/javaee/jstl-api)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Domain Driven Design Intro](https://software-architecture-guild.com/guide/architecture/domains/what-is-domain-driven-design/)
- [Foundation for Sites Documentation](https://foundation.zurb.com/sites/docs/)
- [Font Awesome Icons](https://fontawesome.com/icons?d=gallery)
- [Apache Commons JEXL](https://commons.apache.org/proper/commons-jexl/reference/syntax.html)
- [Snyk](https://snyk.io)
