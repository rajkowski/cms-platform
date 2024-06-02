---
id: developer-environment
title: Developer's Local Environment
# prettier-ignore
description: Options for locally developing CMS Platform
---

CMS Platform is meant to be fully developed offline. This allows developers to code, build, test, and run with the least friction when developing.

Developers can use [Visual Studio Code](https://code.visualstudio.com) with several recommended extensions for a truly Open Source environment. Developers can also use other IDEs like [IntelliJ IDEA](https://www.jetbrains.com/idea/). Settings for each are included in the SimIS CMS source code, however VS Code development is primarily maintained.

## Using VS Code

The following steps will guide you through the developer tools and environment setup so that your code changes can be compiled and copied automatically and then seen in your web browser.

1. Install [OpenJDK 17+](https://learn.microsoft.com/en-us/java/openjdk/download)
2. Install [Apache Ant 1.10+](https://ant.apache.org) and configure your terminal's path with ANT_HOME/bin
3. Install [Apache Tomcat 9.x](https://tomcat.apache.org/download-90.cgi) into a directory of your choice
4. Install the PostgreSQL database server – natively on MacOS with [Postgres.app](https://postgresapp.com) or with a Docker container like (postgis/postgis:14-3.4)
5. Clone the CMS Platform repo – `git clone https://github.com/rajkowski/cms-platform.git`
6. In the repo directory execute `ant webapp` – this tests your environment and updates code and library changes in a working Tomcat exploded webapp directory `./out/exploded/ROOT`
7. Open SimIS CMS in VS Code and accept the recommended extensions
8. Manually setup the VS Code Community Server Connector with Apache Tomcat, setup a deployment, and choose to edit the server with your system's settings:

```json
  "mapProperty.launch.env": {
    "CMS_PATH": "/Users/matt/Web/cms-platform",
    "DB_NAME": "cms-platform"
  },
  "deployables": {
    "/Users/matt/Source/cms-platform/out/exploded/ROOT": {
      "label": "/Users/matt/Source/cms-platform/out/exploded/ROOT",
      "path": "/Users/matt/Source/cms-platform/out/exploded/ROOT",
      "options": {}
    }
  }
```

If not specified, the path for file assets and external configuration on Linux is `/opt/cms-platform`; otherwise `$USER_HOME/Web/cms-platform`

## Developer Resources

- [CMS Platform](https://github.com/rajkowski/cms-platform)
- [Java 17 SDK Documentation](https://docs.oracle.com/en/java/javase/17/)
- [MVC Example with Servlets and JSP](https://www.baeldung.com/mvc-servlet-jsp)
- [Servlet 4.0 API](https://tomcat.apache.org/tomcat-9.0-doc/servletapi/index.html)
- [JSP 2.3 API](https://tomcat.apache.org/tomcat-9.0-doc/jspapi/index.html)
- [JSTL 1.2.5 API](https://github.com/javaee/jstl-api)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Domain Driven Design Intro](https://airbrake.io/blog/software-design/domain-driven-design)
- [Foundation for Sites Documentation](https://foundation.zurb.com/sites/docs/)
- [Font Awesome Icons](https://fontawesome.com/icons?d=gallery)
- [Apache Commons JEXL](https://commons.apache.org/proper/commons-jexl/reference/syntax.html)
- [Snyk](https://snyk.io)
