# Copilot Instructions for CMS Platform

## Project Overview

- CMS Platform is an enterprise-grade, open source content management system (CMS) built on a custom Java web framework.
- Major features: dynamic pages, widgets, SSO (OAuth2), static site generation, analytics, e-commerce, CRM, REST API, and more.
- The system is modular with Domain-Driven Design (DDD): clear separation between application layer (commands/use cases), domain models, infrastructure (data access, external services), and presentation (widgets, JSP, REST).

## Code Style & Conventions

- **Max Line Length:** 300 characters (enforced by `checkstyle.xml`)
- **Naming:** Commands use `{Action}{Entity}Command` format (e.g., `SaveImageCommand`, `DeleteGroupCommand`). Methods use verb prefixes: `load*`/`find*`, `save*`, `delete*`, `query*`/`get*`.
- **Structure:** All Classes/Methods have Apache license headers. Packages use UPPER_SNAKE_CASE for constants, camelCase for variables, PascalCase for classes.
- **Linting:** `ant test` runs Checkstyle; fix violations before committing. Key rules: switch statements must have `default`, `@Override` annotations required for overridden methods.
- **Logging:** Use Apache Commons Logging: `private static Log LOG = LogFactory.getLog(ClassName.class);` then `LOG.debug()`, `LOG.warn()`, `LOG.error()`.

## Architecture & Key Components

- **Backend:** Java (JDK 17+), custom MVC framework, servlets, JSP, scheduled tasks, workflows, caching, permissions.
- **Application Layer:** Commands are static, action-based classes at `src/main/java/com/simisinc/platform/application/{module}/commands/`. Each command encapsulates a use case, validates input, returns domain objects or throws `DataException`.
- **Data Access:** Custom lightweight ORM using `DB` utility class (fluent API) with `Specification` query objects. Repositories in `infrastructure/` layer; use `@Repository` pattern with `-1L` as "not set" sentinel.
- **Frontend:** JSP templates in `src/main/webapp/WEB-INF/jsp/`. Widgets are reusable components with permission checks, preference-driven rendering, and JSP execution. Widget base class: `com.simisinc.platform.presentation.widgets.AbstractWidget`.
- **API:** REST endpoints are for external clients (not the frontend) with OAuth2 and API key authentication. Responses use `ServiceResponse` objects with `status`, `meta`, `data`, `error` fields. See [docs/api.md](../docs/api.md) for patterns.
- **Frontend AJAX:** Use `/json/` pattern with form data parameters for dynamic JavaScript services. Only GET, POST, and HEAD methods supported. Responses are JSON formatted and use `ServiceContext` for request metadata.
- **Data:** PostgreSQL database with Flyway migrations. External config lists in `config/cms/` and `config/e-commerce/` (e.g., `bot-list.csv`, `country-ignore-list.csv`). Site properties cached via Caffeine.
- **Build System:** Apache Ant (`build.xml`) with schema files; tasks include compile, test, package, deploy, clean deploy. Produces exploded webapp in `out/exploded/ROOT` for hot-reload development.
- **Containerization:** Docker support via `docker-compose.yaml`, with `.env` for environment variables.

## Build & Test Commands

- **Compile:** `ant compile` - Builds classes to `target/classes/`
- **Test:** `ant test` - Runs JUnit 5 tests in `src/test/java/` and checks Checkstyle rules
- **Deploy:** `ant deploy` (or `ant webapp`) - Builds exploded webapp to `out/exploded/ROOT/` for hot-reload in Tomcat
- **Clean Deploy:** `ant clean webapp` - Cleans and rebuilds from scratch
- **Package:** `ant package` - Creates WAR file to `target/cms-platform.war`

## Development Workflow

- **Local Dev:** Configure Tomcat to point `ROOT` context to `out/exploded/ROOT` for hot-reload during development. JSPs are pre-compiled during deploy.
- **Testing:** Unit tests use JUnit 5 + Mockito. Test base classes available for widget testing. Aim for comprehensive code coverage.
- **Database:** Migrations use Flyway. Queries use the `DB` utility class with fluent API and `Specification` objects. Always use parameterized queries to prevent SQL injection.
- **Permissions:** Check user roles/permissions before data operations. Use permission helper methods from context.
- **Static Site:** Use the UI to compose content, then publish static copies for SEO/caching.
- **API Auth:** Create app client in Admin, enable API server, use OAuth2 or API key for requests.

## Project Conventions

- **Modules:** Organized under `src/main/java/com/simisinc/platform/application/{module}` with subfolders for `commands/`, `models/`, `beans/`, `widgets/`. Examples: `cms/`, `ecommerce/`, `admin/`, `analytics/`, `oauth/`, `workflow/`.
- **Commands:** Command classes encapsulate business logic as static methods, validate inputs, return domain objects, throw `DataException` on errors. Example: `SaveImageCommand.saveImage(Image imageBean)` returns an `Image` or throws `DataException`.
- **Repositories:** Access data through repository pattern in `infrastructure/` layer. Use `Specification` objects for complex queries. Responses include optional collections, use fluent builder pattern for queries.
- **Error Handling:** Throw `DataException` with user-friendly messages. Generic framework exceptions for system-level errors. Avoid exposing internal stack traces to API clients.
- **Config:** External lists in `config/cms/` and `config/e-commerce/` (CSV format). Site properties cached. Load config via `SiteProperties` or config managers.  
- **Libraries:** All dependencies are vendored in `lib/` organized by use (compile, test, build, war). No Maven/Gradle by default; versions are critical for security.
- **Caching:** Caffeine cache for site properties, credentials, user groups. Cache keys in `APP_CACHE`, `USER_CREDENTIALS_CACHE`.
- **Frontend:** JSPs in `src/main/webapp/WEB-INF/jsp/`, static assets in `src/main/webapp/assets/`. Use Foundation for Sites and Font Awesome for UI.
- **Environment:** Version format: `YYYYMMDD.10000`. Set env vars or use `.env` for Docker deployment. Database connection via properties file.
- **Documentation:** MKDocs format in `docs/`, with architecture diagrams in `docs/diagrams/`. Update docs when changing APIs or architecture.

## Integration Points

- **External Services:** Google Analytics, MapBox, Stripe, USPS, Taxjar, Boxzooka, Snyk. Each integration typically has a service class in its module.
- **SSO:** OAuth2 endpoint discovery for user authentication, group mapping. See `application/oauth/` for OAuth2 command implementations.
- **API:** REST endpoints for external clients at `src/main/java/com/simisinc/platform/rest/`. Use `ServiceContext` for request metadata, `ServiceResponse` for responses with status/meta/data/error.
- **Database:** PostgreSQL with Flyway migrations. Schema updates via migration files, not direct DDL.

## Security Patterns

- **Input Validation:** Validate inputs in commands before processing. Use parameterized queries via `DB` utility to prevent SQL injection.
- **Authorization:** Check user roles/groups before sensitive operations. Use permission helper methods from `ServiceContext`.
- **Rate Limiting:** Implement rate limiting for API endpoints to prevent abuse.
- **Error Messages:** Return generic error messages to clients; log detailed errors server-side for debugging.
- **XSS Prevention:** JSP taglibs handle context-aware escaping. Never output unescaped user input in templates.

## Common Patterns

### Widget Development
Widgets are reusable UI components. Extend `AbstractWidget`, implement `getHTML()` method, check permissions early, load data, render JSP. Example: `ContentWidget`, `FormWidget`, `MenuWidget` in `src/main/java/com/simisinc/platform/application/cms/widgets/`.

### API Response Format
```json
{
  "status": "ok",
  "meta": {"page": 1, "total": 50},
  "data": [...],
  "error": null
}
```

### Command Structure
```java
public class SaveImageCommand {
  public static Image saveImage(Image imageBean) throws DataException {
    // 1. Validate inputs
    // 2. Load existing or create new
    // 3. Update properties
    // 4. Persist and return
  }
}
```

### Database Query Pattern
Using `Specification` with fluent builder: `ImageSpecification spec = new ImageSpecification().setCollectionId(123).setApprovedOnly(true);` then `ImageRepository.findAll(spec)`.

## References

- [docs/project-structure.md](../docs/project-structure.md)
- [docs/architecture.md](../docs/architecture.md)
- [docs/api.md](../docs/api.md)
- [docs/developer-environment.md](../docs/developer-environment.md)
- [workspace/AI_AGENT_PATTERNS_GUIDE.md](../../workspace/AI_AGENT_PATTERNS_GUIDE.md) - Detailed technical patterns for AI agents

---
For questions on conventions, review referenced docs or the detailed patterns guide. Suggest improvements to this file as you discover workflows unique to your features.
