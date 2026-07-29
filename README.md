# CMS Platform

[![Java CI](https://github.com/rajkowski/cms-platform/actions/workflows/ant.yml/badge.svg)](https://github.com/rajkowski/cms-platform/actions/workflows/ant.yml)

## What is CMS Platform?

CMS Platform comes out-of-the-box with modules, advanced security, easy setup, and powerful developer features. Use and configure what's there, and customize what's not. The flexible Open Source license lets you move beyond the technology to focus on delivering a quality website.

**New for 2026** – Build your web pages, styles, and SEO, with a comprehensive visual webpage editor.

## What's improved in CMS Platform?

- It's well-maintained
- You can publish your website as a completely static website
  - Use CMS Platform's UI across your team to compose web changes including content, navigation, layout, Javascript, CSS, etc.
  - Then publish static copies (without draft content) with features like:
    - SEO naming conventions
    - Redirects
    - Caching
    - PDF downloads
    - Streaming videos
- There's a refined Enterprise SSO integration with OAuth2 endpoint discovery and user groups
- The site and deployment properties can be completely configured through environment variables

## Documentation

The latest CMS Platform documentation is available at <https://github.com/rajkowski/cms-platform/blob/main/docs/index.md> including technical documentation and diagrams.

The CHANGELOG is at <https://github.com/rajkowski/cms-platform/blob/main/docs/CHANGELOG.md>.

Documentation is in MKDocs format intended for use in platforms which use MKDocs like [Spotify Backstage](https://backstage.io).

## License

```text
Copyright 2026

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Overview

Need a website or web portal? CMS Platform can be used from Day 1:

- Once installed the administrator signs in and can quickly create a sitemap. Working on their own, or with others, the pages of the site are added.
- Each web page can have shared elements and styles, as well as their own elements and styles.
- A designer can work on the site's global style and layout, then target page-by-page improvements, while content authors fill out the web page content.
- Content authors work with the page elements which include searchable text, images, and videos. There are many dynamic elements which can be selected, including slideshows, news feeds, calendar events, blog posts, and more.
- For more complex components, a developer can work both online and off to enable the functionality or create it.

## Features

- **CMS**: Visual Webpage Editor, Site Map, Web Pages (Templates, UI Designer, SEO, Searchable) with Content and Images, HTML Editor, CSS Editor, Blogs, Form Data, Calendars, Folders and Files, Mailing Lists, Videos, Wikis, Search, Site Alerts, Form Pop-Ups, Sticky Header and Buttons, Responsive, Bot Detection, Static Site Generator (SSG)
- **Analytics**: Tracking for Sessions, Hits, Geolocation, Content, Searches, Referrals; Charts; xAPI; Pixels
- **Data Integration**: Datasets (CSV, TSV, JSON, GeoJSON, and RSS sources), Collections (Profiles, Geolocation, Multiple Categories, Relationships, Custom Fields, Indexed, Searchable), Data Sources
- **Collaboration**: Users (Register, Validation, Login, Invite), User Groups, Collection Membership and Permissions, Chat
- **E-commerce**: Products, SKUs, Categories, Customers, Orders, Account Management, Shipping Methods, Carriers, Tracking Numbers, Pricing Rules (Constraints, Discounts, and Promos)
- **CRM**: Forms, Leads & Customers, Orders
- **Settings**: Theme, Site SEO, Social Media, Mail Server, Maps, Captcha, Analytics, E-commerce, Mailing Lists
- **Integration**: Google Analytics, Map Box, Open Street Map, Square, Stripe, Taxjar, USPS, Boxzooka
- **Security**: OAuth, Firewall (Integration and Blocked IP lists), Spam Filter, Geo Filter, Rate Limiting, Snyk scanning
- **API**: Rest API
- **Platform**: Micro Widgets, Connection Pool, Cache, Scheduler, Workflow, Expression Engine, Upgrades, Migrations, Record Paging

## Run the Published Container Images

You can use the `docker-compose.yaml` file in this repo to pull the latest images and run them locally:
<https://github.com/rajkowski/cms-platform/blob/main/docker-compose.yaml>

Create a file called `.env` with the following values, edit as needed:

```dotenv
CMS_ADMIN_USERNAME=<Your_Admin_Username>
CMS_ADMIN_PASSWORD=<Your_Admin_Password>
CMS_FORCE_SSL=false
CMS_NODE_TYPE=main
DB_SERVER_NAME=db
DB_SSL=false
DB_NAME=cms-platform
DB_USER=postgres
DB_PASSWORD=postgres_password
```

Run:

```bash
docker compose up
```

Login: <http://localhost/login>

## Release Process

An optimized web application archive (.war), with production settings, is released to this project's GitHub releases, ready for installation and which automatically upgrades previously installed versions. Always have a backup of your database and the file library path.

The latest CMS Platform release is at <https://github.com/rajkowski/cms-platform/releases>.

Release notes include a list of changes for review.

Download the .war and follow your choice of deployment options.

To log into a new site, add "/login" to the URL. Later, turn on the login setting to reveal a login button for your website.
