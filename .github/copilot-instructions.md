# Copilot Instructions for CMS Platform

## Project Overview

- CMS Platform is an enterprise-grade, open source content management system (CMS) built on a custom Java web framework.
- Major features: dynamic pages, widgets, SSO (OAuth2), static site generation, analytics, e-commerce, CRM, REST API, and more.
- The system is modular, with clear separation between application modules, domain models, infrastructure, and presentation layers.

## Architecture & Key Components

- **Backend:** Java (JDK 17+), custom MVC framework, servlets, JSP, scheduled tasks, workflows, caching, permissions.
- **Frontend:** JSP templates, widgets, static asset management, Foundation for Sites, Font Awesome.
- **API:** REST endpoints are for external clients (not the frontend) with OAuth2 and API key authentication. See `docs/api.md` for usage patterns.
- **Frontend API:** endpoints to use the /json/ AJAX pattern with form data parameters, matching the CMS Platform's convention for dynamic JavaScript services. Only GET, POST, and HEAD methods are supported. Responses are JSON formatted.
- **Data:** PostgreSQL database, external config lists in `config/`, support for CSV/JSON/GeoJSON/RSS datasets.
- **Build System:** Apache Ant (`build.xml`), with tasks for compile, test, package, deploy, and clean deploy.
- **Containerization:** Docker support via `docker-compose.yaml`, with `.env` for environment variables.

## Developer Workflow

- **Build:** Use Ant tasks (`ant compile`, `ant test`, `ant deploy`, `ant package`).
- **Local Dev:** Exploded Tomcat webapp in `out/exploded/ROOT`. Configure Tomcat to point here for hot-reload.
- **Testing:** Unit tests in `src/test/java`, run with `ant test`. Linting via Checkstyle (`checkstyle.xml`).
- **Static Site:** Use the UI to compose content, then publish static copies for SEO/caching.
- **API Auth:** Create app client in Admin, enable API server, use OAuth2 or API key for requests.

## Project Conventions

- **Modules:** Organized under `src/main/java/com/simisinc/platform/application/` (e.g., `cms`, `ecommerce`, `admin`).
- **Config:** External lists (e.g., bot/country/email/ip) in `config/cms/` and `config/e-commerce/`.
- **Libraries:** All dependencies are vendored in `lib/` (no Maven/Gradle by default).
- **Frontend:** JSPs in `src/main/webapp/WEB-INF/jsp/`, static assets in `src/main/webapp/assets/`.
- **Environment:** Use `.env` for Docker, or set env vars for Tomcat deployment.
- **Documentation:** MKDocs format in `docs/`, with architecture diagrams in `docs/diagrams/`.

## Integration Points

- **External Services:** Google Analytics, MapBox, Stripe, USPS, Taxjar, Boxzooka, Snyk.
- **SSO:** OAuth2 endpoint discovery, user groups.
- **API:** REST endpoints for user/server access, see `docs/api.md` for patterns.

## Examples & Patterns

- **Widget Example:** See `src/main/java/com/simisinc/platform/application/cms/widgets/` for custom widgets.
- **API Example:** See `docs/api.md` for authentication and request patterns.
- **Build Example:** `ant webapp` to build and deploy to Tomcat exploded directory.

## References

- [docs/project-structure.md](../docs/project-structure.md)
- [docs/architecture.md](../docs/architecture.md)
- [docs/api.md](../docs/api.md)
- [docs/developer-environment.md](../docs/developer-environment.md)

---
For questions or unclear conventions, review the referenced docs or ask for clarification. Please suggest improvements to this file if you discover new patterns or workflows.
