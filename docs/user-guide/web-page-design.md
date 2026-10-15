# Web Page Design User Guide

Users must have the appropriate role(s) to make changes to the website:

- **System Administrator**: This role allows the user to modify the web page layouts using the web-based XML Layout Editor.
- **Content Manager**: This role allows the user to interactively edit the content of the website. This user does not see the web-based XML editor.

## Web Pages

The application includes several page templates which are made up of XML. Using the Layout Editor, the XML is 100% customizable, and pages can be created from any combination of widgets.

Each web page is independently configured and designed. The layout editor is comprised of well-formed XML to design the page. Within the design, Widgets are placed using XML snippets (see examples below).

### Page Attributes

Pages are made up of sections and columns. Each section can be thought of as a container of columns, and those columns are responsive depending on the setting attributes and the end-user's display size.

Using HTML class attributes, specify how many grid columns (out of 12) is intended for the size of the display. A value of 12 means the column will stretch the full length of the display, and additional columns then would wrap to the next row on that display. You can specify the expected behavior for small displays (the default for all displays), and then override the value for medium and large displays.

In the following example, small screens will have columns wrapped/stacked on each other because the full width of "12" is assigned; however for medium and larger screens, the columns will be arranged next to each other with the "medium-3" taking up about 1/4 of the width, and "medium-9" taking of the rest. Since the values add to 12 the columns will not wrap like they do on the smaller display. A column "cell" attribute must be specified for the proper display behavior.

```xml
<page>
  <section>
    <column class="small-12 medium-3 cell">
      <widget name="tableOfContents">
        <uniqueId>tab-1-item-2</uniqueId>
      </widget>
    </column>
    <column class="small-12 medium-9 cell">
      <widget name="content">
        <uniqueId>tab-1-item-2-content</uniqueId>
      </widget>
    </column>
  </section>
</page>
```

There are several default classes provided to change the page output, margins, and paddings.

`page class="full-page"` will expand the page to the width of the display.

### Additional Attributes

There are several default classes provided to change the section output, margins, and paddings.

`section id="" class="grid-x grid-margin-x platform-no-margin align-middle align-center" hr="true"`

There are several default classes provided to change the column output, margins, and paddings.

`column id="" class="small-12 cell text-center callout radius round" hr="true"`

There are several default classes provided to change the widget output, margins, and paddings.

`widget id="" name="" hr="true"`

When `hr="true"` is specified, then an HTML `<HR>` element is appended before a following section, column, and/or widget (if the additional section/column/wiget would be displayed).

## Configurable Header

The header uses the same section/column/widget XML configuration mechanism as other layouts. System administrators can open `/admin/web-container-designer?name=header.default&returnPage=/` to edit its complete definition. When no published override exists, the editor loads the bundled definition without creating a database record. Save publishes an override for the current workspace and invalidates its header cache, `header.default`

Theme Properties includes an optional **Header XML Definition** field (`theme.header.name`). Leave it empty to retain the current default: `header.default`. Enter a named header to select it explicitly. Checkout, iframe/suppressed routes, existing administrative page-shell rules, and static export keep their existing behavior.

### Header and Placement Settings

Document order controls the displayed structure; `num` remains a coordinate/identity setting, not a sort instruction.

| Setting | Scope | Accepted Values and Default |
| --- | --- | --- |
| `name`, `title` | Header | Required named definition and optional authoring label |
| `id` | Header/section/column/widget | Unique identifier without whitespace; root defaults to `platform-menu` |
| `class`, `style` | Header/section/column/widget | Styling hooks; use scoped classes, CSS variables, spacing, alignment, and sizing rules |
| `role`, `group` | Header/section/column/widget | Existing comma-separated visibility restrictions; no permission grants |
| `enabled` | Header/section/column/widget | `true` or `false`, default `true`; disabled parents do not execute children |
| `sticky` | Header | `true` by default; `false` disables root stickiness |
| `scrollBehavior` | Header | `hide-on-down` by default or `none`; upward scrolling reveals the header |
| `scrollThreshold` | Header | Integer 0-10000, default 100 pixels |
| `visibility` | Section/column/widget | `all` by default, `small` for small only, `medium` for medium and larger, `large` for large and larger |
| `container` | Section | `none` by default, `grid` for an inner grid container, `header` for the flex container |
| `containerClass` | Section | Optional inner-container classes; defaults to `grid-container` or `header-container` for those container modes |
| `wrapper` | Column | `default` or `none`; `none` requires absent ID/class/style/sticky settings |
| `wrapper` | Widget | `auto` or `none`; auto adds a wrapper when placement settings require one; none requires those settings absent |
| `sticky` | Column/widget | Existing local sticky behavior, default false; separate from the root's scroll behavior |

Empty sections/columns are omitted. Use one main navigation, live search, account menu, region selector, and mobile toggle per header. Their scoped hooks initialize once.

### Header Widgets and Preferences

| Widget | Settings | Data and Default Behavior |
| --- | --- | --- |
| `systemAlert` | `showEditButton=true`, `linkLabel=Details` | Existing announcement properties and optional link; absent text produces no band, editing remains admin-only |
| `logo` | `homeLink=/`, existing `maxWidth`, `maxHeight`, `logoClass`, `text` | Site branding; mixed, white, then standard dark-logo fallback; missing logo displays icon/site name |
| `mainMenu` | `omitFirstHomeItem=true`, optional `submenuIcon`, `submenuIconClass`, `class`, highlight settings | Existing permission-filtered navigation, active state and child menus; permission checking cannot be disabled |
| `searchControl` | `placeholder=Search...`, `minChars=3`, `debounceMs=300` | GET `/search` and existing live HTML results; minChars 1-100 and debounceMs 0-5000 |
| `menu` | flat `links` entries | Independently configurable ordinary actions; automatic legacy admin-menu types are not used |
| `headerControl` | `type=region`, `appearance`, `askAI`, or `mobileMenu`; optional `label`, `icon` | One separately placeable control; Region/globe, appearance moon/sun, and mobile bars/times defaults |
| `headerAccountMenu` | Flat `links` entries, `title=User Menu` | Authenticated user name and configured action groups; no guest or empty menu |
| `cart` | `emptyMessage=Your cart is empty`, `cartLink=/cart`, `checkoutLink=/checkout` | Read-only current cart display; requires enabled site cart; no ordinary-page mutations or redirects |

Ask AI and Contact are ordinary `menu` entries. Guest Login matches the original header default; an optional site-property `rule` can be configured deliberately. Guest appearance is a guest-only control and authenticated appearance is an account action, so the supplied definition has one visible appearance control.

Ordinary link entries accept `name`, `link`, `icon`, `class`, `icon-only`, `role`, `group`, and `rule`. Account entries additionally use `action`, `section`, `sectionLabel`, and `enabled`. Flat entry attributes cannot contain `|`, the existing preference encoding delimiter. Group labels must agree for the same group key, and groups with no eligible actions disappear.

### Customization Example

Start with the complete bundled definition in the editor; do not replace it with a partial example. Keep sections/columns and action order in XML rather than copying the entire header into a content widget. Separate single-entry `menu` widgets allow ordinary actions to be reordered around controls such as the cart.

This replacement action list demonstrates changed order, labels, destinations, and icons:

```xml
<widget name="menu">
  <links>
    <link name="Contact first" link="/contact-us" icon="fa-envelope" />
    <link name="Assistant" link="/ask-ai" icon="fa-brain" />
  </links>
</widget>
```

Remove a control or set its widget `enabled="false"` to hide it. Set the header's `scrollBehavior="none"` to keep it from hiding, change a search widget's `debounceMs`, or change a section's `containerClass`/style for presentation. Save and reload to verify the effect. File-only edits use the existing application reload/restart lifecycle; publishing editor changes uses the existing workspace cache invalidation.

### Account Tools and Safety

Account actions use fixed server-owned eligibility ceilings. XML can hide, reorder, relabel, regroup, or safely redirect an action, but cannot grant its permission. Destination authorization remains required even when a control is hidden.

| Actions | Maximum Eligible Roles |
| --- | --- |
| pageInfo, pageAnalytics, pageDesign, files, images, hierarchy, siteAnalytics | Admin or content-manager |
| instructions | Admin, community-manager, content-manager, or data-manager |
| users | Admin or community-manager |
| announcement, siteCss, setup | Admin |
| appearance, account, logout, ordinary link | Signed-in users, further constrained by configured visibility |

Page tools bind the current page or documented master-page fallback and encode query values. Configuration does not execute arbitrary destination expressions. Internal paths receive the context path once; HTTP/HTTPS external destinations keep their host. JavaScript/data/protocol-relative/malformed destinations are rejected. Appearance actions accept no destination override.

Malformed definitions, structural conflicts, duplicate controls/IDs, unsafe destinations, and inconsistent groups are rejected with diagnostics. Invalid scalar options use their documented defaults; unknown optional preferences are warned about and ignored. A rejected publication does not replace a valid definition. Reload failures retain the workspace's last validated definition or use the route-appropriate bundled fallback without recursively retrying the invalid override.

## Widgets

There are approximately 50 widgets which may be configured and combined on web pages. Some widgets use a "uniqueId" will corresponds to using the backend database to reference the content. By using the same "uniqueId" on multiple pages, the content can be edited once, and each page will reuse the same content source-of-truth.

### Breadcrumbs

```xml
<widget name="breadcrumbs">
  <links>
    <link name="Previous Page Title" value="/link" />
    <link name="Page Title" value="" />
  </links>
</widget>
```

### Content

Placing the widget with the database content editor (recommended):

```xml
<widget name="content">
  <uniqueId>content-unique-id</uniqueId>
</widget>
```

Placing the widget and specifying HTML to inject:

```xml
<widget name="content">
  <html>&lt;![CDATA[ Any HTML content ]]&gt;</html>
</widget>
```

### Content Accordian

The accordian can have multiple levels of expanding sections.

Placing the widget with the database content editor (recommended):

```xml
<widget name="contentAccordion">
  <uniqueId>content-unique-id</uniqueId>
</widget>
```

Placing the widget and specifying HTML to inject:

```xml
<widget name="contentAccordion">
  <html>&lt;![CDATA[ Any HTML content ]]&gt;</html>
</widget>
```

### Content Cards

### Content Gallery

### Content Reveal

### Content Slider

### Content Tabs

### Button

### Link

### Card

### Menu

### Progress Card

### Statistic Card

### Logo

### Copyright

### Email Subscribe

### Form

### Blog Post List

### Blog Post

### Blog Post Name

### Product Browser

### Product Name

### Product Image

### Product Description

### Add to Cart

### Cart

### Remote Content

### Remote Course List

These require Moodle Token and URL to be configured.

```xml
<widget name="remoteCourseList" group="instructors">
  <title>Instructor's Courses</title>
  <showWhenEmpty>true</showWhenEmpty>
  <role>teacher</role>
  <useItemLink>false</useItemLink>
  <showLaunchLink>true</showLaunchLink>
  <launchLabel>View</launchLabel>
  <showParticipants>true</showParticipants>
  <noRecordsFoundMessage>No classes were found</noRecordsFoundMessage>
  <showAddCourseButton>true</showAddCourseButton>
  <courseButtonText>Add a course</courseButtonText>
</widget>
```

```xml
<widget name="remoteCourseList">
  <title>My Enrolled Courses</title>
  <showWhenEmpty>true</showWhenEmpty>
  <useItemLink>false</useItemLink>
  <showLaunchLink>true</showLaunchLink>
  <launchLabel>View</launchLabel>
</widget>
```

### Search Form

### Table of Contents (TOC) Widget

This is a reusable component which dynamically adapts to the page it is displayed on.

Set `uniqueId` to the same value on each page you would like the shared table of contents to appear.

Once placed on a page, a User with the Content Editor Role can choose to edit the values and links displayed to the user. When the link matches the current page's URI, then users of the site will see that TOC link highlighted.

Placing the widget with the database configuration editor (recommended):

```xml
<widget name="tableOfContents">
  <uniqueId>example-toc</uniqueId>
</widget>
```

Placing the widget and manually specifying the TOC entries:

```xml
<widget name="tableOfContents">
  <links>
    <link name="The first page" value="/the-first-page" />
    <link name="Another page &amp; information" value="/another-page-and-information" />
  </links>
</widget>
```

Once the widget is placed and saved on a page, the Content Editor can interactively make changes.

Editing the widget with the database configuration editor:

| Order | Name                       | Web Page Link (/page)         |
|:-----:|----------------------------|-------------------------------|
| 1     | The first page             | /the-first-page               |
| 2     | Another page & information | /another-page-and-information |

- Order: Configures the order in which the TOC entries are displayed
- Name: The value shown in the widget to the user
- Web Page Link: This is the link the user selects to navigate to another page

When specifying a Web Page Link, standard [URI restrictions](https://www.rfc-editor.org/rfc/rfc3986#page-12) apply.

Do not use these URI reserved characters with web page links: `:`  `/` `?` `#` `[` `]` `@` `!` `$` `&` `'` `(` `)` `*` `+` `,` `;` `=`

### Album Gallery

### Photo Gallery

### File List

### File List by Folder

### File List by Year

### File Drop Zone

### Calendar

An example calendar component configured to show the CMS calendar called "all-learners" as well as combining the user's Moodle Events if the integration is enabled.

```xml
<widget name="calendar">
  <view>small</view>
  <default>month</default>
  <calendarUniqueId>all-learners</calendarUniqueId>
  <showEvents>true</showEvents>
  <showHolidays>true</showHolidays>
  <showMoodleEvents>true</showMoodleEvents>
</widget>
```

### Upcoming Calendar Events

### Calendar Event Details

### Map

### Instagram Photos

A business account must be setup at Instagram, and the API token must be configured in the CMS Platform Social Settings.

### Social Media Links

### Wiki

### Web Page Content Search Results

### Web Page Title Search Results

### Blog Post Search Results

### Calendar Search Results

### Items Search Results
