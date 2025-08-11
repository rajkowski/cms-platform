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
