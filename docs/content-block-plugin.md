# Content Block Browser Plugin for TinyMCE

## Overview

This plugin adds a new TinyMCE toolbar button that allows content editors to insert references to existing Content Blocks from a searchable browser dialog. When a content block is inserted, it appears in the editor as a visual span tag with the format `${uniqueId:value}`, but is saved as plain text for dynamic rendering.

## Features

- **Content Browser Dialog**: Displays all available content blocks in a searchable list
- **Search Functionality**: Real-time filtering of content blocks by unique ID
- **Visual Representation**: Content block references appear as styled inline elements in the editor
- **Easy Editing**: Double-click a content block reference to replace it with another
- **Easy Removal**: Select and delete a content block reference using backspace or delete keys
- **Automatic Conversion**: 
  - When loading: Plain text `${uniqueId:value}` → Visual span tag
  - When saving: Visual span tag → Plain text `${uniqueId:value}`

## Implementation Details

### Backend Components

1. **ContentBrowserWidget.java**
   - Location: `src/main/java/com/simisinc/platform/presentation/widgets/cms/`
   - Purpose: Loads and displays available content blocks in the browser dialog
   - Uses `LoadContentListCommand` to retrieve content blocks

2. **content-browser.jsp**
   - Location: `src/main/webapp/WEB-INF/jsp/cms/`
   - Purpose: JSP view for the content browser with search and selection UI
   - Features: Real-time search filtering, preview of content

3. **TinyMceCommand.java**
   - Location: `src/main/java/com/simisinc/platform/application/cms/`
   - Enhanced with two new methods:
     - `convertContentBlockTextToSpans()`: Converts `${uniqueId:value}` to span tags when loading into editor
     - `convertContentBlockSpansToText()`: Converts span tags back to `${uniqueId:value}` when saving

4. **Route Configuration**
   - Added `/content-browser` page route in `web-layouts/page/cms-layout.xml`
   - Registered `contentBrowser` widget in `widgets/widget-library.xml`

### Frontend Components

1. **TinyMCE Plugin**
   - Location: `src/main/webapp/javascript/tinymce-6.8.6/plugins/contentblock/`
   - Files:
     - `plugin.js`: Main plugin implementation
     - `index.js`: Module loader export
   
2. **Plugin Features**:
   - Toolbar button with "browse" icon
   - Opens content browser in a modal dialog (850x650px)
   - Listens for `ContentBlockSelected` message from browser
   - Inserts styled span element with `contenteditable="false"`
   - Handles double-click to replace content block
   - Handles keyboard deletion of content blocks

3. **Editor Configuration Updates**:
   - Added `contentblock` to plugins list in:
     - `content-editor-bridge.js` (Visual Content Editor)
     - `content-editor.jsp` (Standalone Content Editor)
     - `blog-editor.jsp` (Blog Editor)
   - Added `contentblock` button to toolbar in all editors

## Usage

### For Content Editors

1. **Insert a Content Block**:
   - Click the "Content Block" button in the TinyMCE toolbar (browse icon)
   - Search for the content block you want to insert
   - Click on a content block to insert it
   - The reference appears as: `${uniqueId:example-content}`

2. **Replace a Content Block**:
   - Double-click on an existing content block reference
   - Select a new content block from the browser

3. **Remove a Content Block**:
   - Click to select the content block reference
   - Press Backspace or Delete key

### How It Works

When you insert a content block reference like `${uniqueId:welcome-message}`, the system:

1. **In the Editor**: Displays it as a styled, non-editable span for visual feedback
2. **When Saving**: Converts it back to plain text `${uniqueId:welcome-message}`
3. **When Rendering**: The existing `ContentHtmlCommand.embedInlineContent()` method dynamically replaces the reference with actual content

This allows content blocks to be:
- Reused across multiple pages
- Updated in one place and reflected everywhere
- Edited independently with their own permissions
- Managed as separate content entities

## Technical Notes

### Content Block Span Format

The visual representation in the editor uses this format:

```html
<span class="content-block-ref" 
      contenteditable="false" 
      data-uniqueid="example-content" 
      style="background-color: #e3f2fd; padding: 2px 6px; border-radius: 3px; 
             border: 1px solid #90caf9; display: inline-block; 
             font-family: monospace; font-size: 0.9em;">
  ${uniqueId:example-content}
</span>
```

### Saved Format

The plain text format saved to the database:

```
${uniqueId:example-content}
```

### Rendering

When a page with content block references is displayed to users, the `ContentHtmlCommand.embedInlineContent()` method:
1. Finds all `${uniqueId:value}` patterns
2. Loads the corresponding content blocks
3. Replaces the references with actual content
4. For editors, adds edit buttons to each embedded content block

## Files Modified/Created

### New Files
- `src/main/java/com/simisinc/platform/presentation/widgets/cms/ContentBrowserWidget.java`
- `src/main/webapp/WEB-INF/jsp/cms/content-browser.jsp`
- `src/main/webapp/javascript/tinymce-6.8.6/plugins/contentblock/plugin.js`
- `src/main/webapp/javascript/tinymce-6.8.6/plugins/contentblock/index.js`

### Modified Files
- `src/main/java/com/simisinc/platform/application/cms/TinyMceCommand.java`
- `src/main/webapp/WEB-INF/web-layouts/page/cms-layout.xml`
- `src/main/webapp/WEB-INF/widgets/widget-library.xml`
- `src/main/webapp/javascript/widgets/editor/content-editor-bridge.js`
- `src/main/webapp/WEB-INF/jsp/cms/content-editor.jsp`
- `src/main/webapp/WEB-INF/jsp/cms/blog-editor.jsp`

## Future Enhancements

Possible improvements:
- Add content block categories/tags for better organization
- Show content preview on hover in the editor
- Add keyboard shortcuts for inserting content blocks
- Support drag-and-drop from content browser
- Show live preview of content block in the browser dialog
- Add recently used content blocks section
