# Content Block Browser Plugin - Usage Guide

## Quick Start

The Content Block Browser Plugin adds a new toolbar button to TinyMCE editors that allows you to insert references to existing content blocks.

## Step-by-Step Usage

### 1. Open the Content Browser

Click the **Content Block** button (browse icon) in the TinyMCE toolbar:

```
Toolbar: [ link | image | media | table | contentblock | undo | redo | ... ]
                                            ↑
                                      Click here
```

### 2. Browse and Search Content Blocks

The Content Browser dialog opens showing all available content blocks:

```
┌─────────────────────────────────────────────────────────┐
│  Content Browser                                    [X] │
├─────────────────────────────────────────────────────────┤
│  Search: [_________________________]                    │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │ welcome-message     ${uniqueId:welcome-message}│   │
│  │ Welcome to our site! This is the welcome...    │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │ footer-info         ${uniqueId:footer-info}    │   │
│  │ Company information and contact details...     │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │ pricing-table       ${uniqueId:pricing-table}  │   │
│  │ Our pricing options with features and...       │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Features:**
- **Real-time Search**: Type to filter content blocks by unique ID
- **Preview**: See a preview of each content block
- **Reference Format**: Shows the exact format that will be inserted

### 3. Insert a Content Block

Click on any content block to insert it into your content:

**Before insertion:**
```
This is my page content. [cursor here] More content follows.
```

**After insertion:**
```
This is my page content. ${uniqueId:welcome-message} More content follows.
                         └──────────────┬──────────────┘
                            Inserted reference
```

**How it appears in the editor:**

The reference is displayed as a styled, non-editable element for easy identification:

```
┌──────────────────────────────────┐
│ ${uniqueId:welcome-message}      │  ← Visual indicator
└──────────────────────────────────┘
  • Blue background
  • Monospace font
  • Non-editable
  • Border styling
```

### 4. Manage Content Block References

#### Replace a Content Block
- **Double-click** on the content block reference
- The Content Browser opens again
- Select a different content block

#### Remove a Content Block
- Click to select the content block reference
- Press **Backspace** or **Delete** key

### 5. Save and Render

**When you save:**
The visual span element is automatically converted to plain text:
```
${uniqueId:welcome-message}
```

**When the page is viewed:**
The system dynamically replaces the reference with the actual content:
```
${uniqueId:welcome-message}
         ↓
Welcome to our site! We're glad you're here.
```

## Use Cases

### 1. Reusable Content
Insert the same content block across multiple pages:
- Company information
- Contact details
- Terms and conditions
- Welcome messages

**Benefit**: Update once, reflected everywhere

### 2. Structured Content
Break complex pages into manageable sections:
```
${uniqueId:header-section}
${uniqueId:feature-list}
${uniqueId:testimonials}
${uniqueId:call-to-action}
${uniqueId:footer-section}
```

### 3. Collaborative Editing
Different team members can manage different content blocks:
- Marketing team: Call-to-action blocks
- Legal team: Terms and policy blocks
- Support team: FAQ blocks

### 4. Dynamic Content
Content blocks can contain:
- Rich HTML formatting
- Images and media
- Links and buttons
- Tables and lists
- Other embedded content

## Technical Details

### Storage Format
Content is always stored as plain text in the database:
```
${uniqueId:content-block-name}
```

### Editor Format
In the TinyMCE editor, it's displayed as:
```html
<span class="content-block-ref" 
      contenteditable="false" 
      data-uniqueid="content-block-name" 
      style="...styling...">
  ${uniqueId:content-block-name}
</span>
```

### Rendering Process
1. User requests page
2. System finds all `${uniqueId:*}` patterns
3. Loads corresponding content blocks from database
4. Replaces references with actual content
5. Delivers complete page to user

## Best Practices

### Naming Content Blocks
Use descriptive, hierarchical names:
```
✓ Good:
  - homepage-hero
  - about-team-section
  - product-features-list

✗ Avoid:
  - content1
  - test
  - abc123
```

### Content Organization
Group related content blocks:
```
header-*
  header-navigation
  header-announcement
  
footer-*
  footer-links
  footer-social
  footer-copyright
  
features-*
  features-enterprise
  features-startup
  features-comparison
```

### Nesting Strategy
Keep nesting levels reasonable:
```
✓ Good: Page → Content Block → Text/Images
✗ Avoid: Page → Content Block → Content Block → Content Block (too deep)
```

## Troubleshooting

### Content Block Not Found
If a referenced content block doesn't exist:
- **For Editors**: Shows "Add Content Here" button
- **For Users**: Reference is ignored (no content shown)

### Search Not Working
- Check that content blocks have unique IDs
- Ensure content blocks are published (not just drafts)
- Try refreshing the browser

### Reference Not Converting
If you see the raw `${uniqueId:*}` text on the page:
- Ensure the content block exists
- Check that you have proper permissions
- Verify the unique ID is spelled correctly

## Permissions

- **Insert References**: Requires `admin` or `content-manager` role
- **View Content Browser**: Requires `admin` or `content-manager` role
- **Edit Content Blocks**: Per-content-block permissions apply
- **View Rendered Content**: All users can see rendered content blocks

## Tips and Tricks

1. **Quick Preview**: Hover over the content block reference to see tooltip (future enhancement)

2. **Recent Blocks**: Frequently used content blocks appear first (future enhancement)

3. **Keyboard Shortcuts**: Use Ctrl+K to open Content Browser (future enhancement)

4. **Draft Preview**: Editors see draft versions of content blocks if available

5. **Inline Editing**: Click the edit button on embedded content to edit the block directly
