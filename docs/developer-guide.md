# Developer Guide

The CMS is a configurable platform, not a blank application to rebuild for each project. Start with the capabilities already available, configure them for the project, and customize presentation or supplied files only when configuration is not enough. Change framework code only when the required behavior cannot be delivered through those lower-impact options.

This order keeps projects easier to upgrade, reduces duplicated behavior, and preserves improvements for every platform user. A project-specific need is not automatically a reason to add a project-specific feature to the platform.

## Practical Review Questions

- Did we check what the platform already does before proposing code?
- Is this setting or behavior specific to an environment, a site, or the shared platform, and is it stored at the right level?
- Can another site reuse this change without inheriting project-specific assumptions?
- Are authorization, validation, error handling, and data migration covered where relevant?
- Are tests and documentation included, and can the change be upgraded safely?

## Choose the Smallest Change

Use this order when evaluating a requirement:

1. **Configuration:** Can existing settings, content types, page layouts, widgets, roles, or platform features provide the behavior?
2. **Customization:** Can the desired difference be achieved through supported site assets or presentation changes, such as a header or footer layout or CSS?
3. **Platform extension:** If behavior is missing, can it be added as a reusable widget, JSON service, REST service, or other established extension point?
4. **Low-level framework change:** Change core platform behavior only when the requirement cannot be met at the previous levels and the change is appropriate for the shared platform.

Before implementation, write down the user outcome, the platform capabilities considered, why the lower-impact options do not meet the requirement, and how the change will be validated. Keep this rationale with the project work or its design record.

## Configuration

First explore the capabilities available in the CMS administration interface and existing platform features. Common configuration includes:

- Environment variables and site properties
- Navigation sitemap, site index, pages, page templates, layouts, and SEO settings
- Widget selection, placement, and properties
- Data record types, collections, and datasets
- Users, groups, roles, permissions, and SSO settings
- API clients and portal settings
- Existing content features such as blogs, calendars, documents, images, videos, forms, and wiki pages
- E-commerce settings such as products, categories, discounts, shipping rates, and inventory

Prefer configuration when the requirement is about which features are enabled, what content is shown, who can access it, or how existing components are arranged. Keep environment-specific values in configuration rather than embedding them in code. Verify the result using the roles and environments that will use it.

## Customization

Use supported customization for project-specific presentation and assets that do not require new platform behavior. Typical examples include:

- Header and footer layouts
- Site theme and CSS
- Project-specific images, fonts, and other static assets

Keep custom files separate from platform source and use the deployed application's customization location and documented project conventions. Avoid editing generated deployment output or copying platform files wholesale: doing so makes later upgrades harder to reason about. Confirm that a customization works with the platform's responsive layouts, accessibility expectations, and existing UI behavior.

Customization is not a substitute for a missing business rule or data operation. If the requirement changes what the system does, rather than how existing behavior is presented, evaluate an extension point or framework change.

## When Code Is Needed

When configuration and supported customization cannot deliver the required behavior, add code at the narrowest established extension point. The platform is organized into these main areas:

- **Application modules:** Use cases and commands that coordinate application behavior.
- **Domain model and events:** Shared business concepts and meaningful domain changes.
- **Infrastructure:** Persistence, database access, caching, permissions, scheduling, and workflows.
- **Web presentation:** Controllers and widgets for CMS pages, plus JSON services for browser-driven interactions.
- **REST:** Controllers and services intended for external clients.

Prefer an existing module or extension point over a new parallel implementation. A new widget should be reusable and configurable where practical; project-specific data or policy should not be hard-coded into a general platform component. Keep browser-facing JSON services distinct from external REST APIs, and follow the authentication and response conventions of the relevant interface.

For a feature that should be enabled on some sites but not others, use a named property in the `site_properties` table as its feature flag. Read it through the platform's site-property pattern rather than hard-coding the value, and default new behavior to off unless the rollout explicitly calls for otherwise. Test both enabled and disabled paths, and remember that a feature flag does not replace permission checks or make project-specific behavior appropriate for the shared platform.

For changes that affect persistent data, keep the new-install and upgrade paths aligned. Database changes are managed through Flyway scripts; do not rely on manual schema edits. See the [Database Development Process](database-development-process.md).

## Low-Level Platform Changes

A change to core framework behavior has a larger maintenance and upgrade cost than configuration or customization. Make one only when all of the following are true:

- The requirement is not achievable with existing configuration, customization, or an established extension point.
- The behavior belongs in the shared platform, rather than being a one-project policy or presentation preference.
- The change has a clear owner, tests, documentation, and an upgrade and migration strategy where applicable.
- The implementation follows existing architecture and is suitable for upstream reuse.

If a change is useful only to one project, keep it at the project's supported customization or extension boundary where possible. If the framework must change, keep the change small, preserve existing behavior by default, and make project-specific behavior opt-in and configurable. Do not introduce a core change solely to avoid configuring an existing feature.

## Development and Validation

Before changing code, inspect the nearest existing implementation and tests. Follow the source organization described in [Project Structure](project-structure.md), and review [Customization](customization.md) to see which project outcomes may already be supported without code.

For code changes, validate the narrow behavior first, then run the repository checks appropriate to the change. The standard commands are:

```sh
ant compile
ant test
```

Database, API, permission, and workflow changes need tests for their contracts and failure cases, not only a successful path. Presentation changes should be checked at relevant page sizes and with the relevant roles. See [Developer Environment](developer-environment.md) for local setup and deployment workflows.
