# 📚 FPP Wiki - Frontend Documentation System

A beautiful, modern, and fully-featured wiki system for the Fake Player Plugin documentation.

## ✨ Features

### Core Features
- 📖 **Markdown Rendering** — Full GitHub-flavored Markdown support with syntax highlighting
- 🔍 **Real-time Search** — Fast client-side search with keyword highlighting (Ctrl+K)
- 🎨 **Dark/Light Theme** — Automatic theme switching with persistent preference
- 📱 **Responsive Design** — Works perfectly on desktop, tablet, and mobile
- 🧭 **Smart Navigation** — Sidebar menu + automatic table of contents
- 🔗 **Deep Linking** — Direct links to pages and sections via URL hash
- ⚡ **Fast Loading** — Lazy loading and content caching for instant navigation
- 📋 **Copy Code** — One-click copy buttons on all code blocks

### User Experience
- **Auto-generated TOC** — Right sidebar automatically builds from headings
- **Scroll spy** — Active heading highlighting as you scroll
- **Back to top** — Floating button appears when scrolling down
- **Mobile menu** — Collapsible sidebar for small screens
- **Keyboard shortcuts** — Ctrl+K for search, Escape to close modals
- **Browser history** — Back/forward buttons work correctly
- **External links** — Open in new tabs automatically

### Developer Features
- **GitHub integration** — Loads markdown files directly from GitHub repository
- **Syntax highlighting** — Code blocks use highlight.js with theme matching
- **XSS protection** — DOMPurify sanitization for all rendered content
- **Error handling** — Graceful fallbacks when pages fail to load
- **Print-friendly** — Optimized print styles for documentation

## 🚀 Quick Start

### Development (Local)

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Start the server:**
   ```bash
   npm start
   ```

3. **Open the wiki:**
   ```
   http://localhost:3000/wiki
   ```

### Production (Vercel)

The wiki is automatically deployed with the Express backend. Access it at:
```
https://your-deployment.vercel.app/wiki
```

## 📁 File Structure

```
frontend/
├── wiki.html          # Main HTML structure
├── wiki-styles.css    # Complete styling (light/dark themes)
├── wiki-script.js     # All functionality and interactivity
├── index.js           # Express server (includes wiki route)
└── README-WIKI.md     # This file
```

## 🎯 How It Works

### Content Loading
1. Wiki loads markdown files from GitHub repository
2. Markdown is parsed using [Marked.js](https://marked.js.org/)
3. HTML is sanitized using [DOMPurify](https://github.com/cure53/DOMPurify)
4. Code blocks are highlighted using [highlight.js](https://highlightjs.org/)

### Navigation Flow
```
User clicks sidebar link
    ↓
JavaScript loads markdown from GitHub
    ↓
Markdown → HTML conversion
    ↓
Render in content area
    ↓
Generate table of contents
    ↓
Apply syntax highlighting
    ↓
Update URL hash
```

### Search System
1. **Indexing** — All pages are indexed on first search
2. **Matching** — Case-insensitive substring matching
3. **Grouping** — Results grouped by page
4. **Highlighting** — Query terms highlighted with `<mark>`
5. **Navigation** — Click result to load that page

## 🎨 Customization

### Theme Colors

Edit `wiki-styles.css` CSS variables:

```css
:root {
    --primary-color: #0079FF;      /* Brand color */
    --success-color: #00AF5C;      /* Success state */
    --warning-color: #FF9500;      /* Warning state */
    --danger-color: #FF3B30;       /* Error state */
}
```

### Sidebar Navigation

Edit the sidebar sections in `wiki.html`:

```html
<div class="nav-section">
    <h3>Section Title</h3>
    <ul>
        <li><a href="#PageName" data-page="PageName">🎯 Display Name</a></li>
    </ul>
</div>
```

### Adding Pages

1. Add markdown file to `/wiki/` directory in repository
2. Add link to sidebar navigation in `wiki.html`
3. Add page name to search index in `wiki-script.js`:

```javascript
const pages = [
    'Home', 'Getting-Started', 'YourNewPage', // Add here
    // ...
];
```

## 🔧 Configuration

### GitHub Repository URL

Edit `WIKI_BASE_URL` in `wiki-script.js`:

```javascript
const WIKI_BASE_URL = 'https://raw.githubusercontent.com/USERNAME/REPO/main/wiki/';
```

### Default Page

Edit `DEFAULT_PAGE` in `wiki-script.js`:

```javascript
const DEFAULT_PAGE = 'Home';
```

### Server Port

Edit port in `frontend/index.js` or set environment variable:

```javascript
const port = process.env.PORT || 3000;
```

## 📱 Responsive Breakpoints

| Breakpoint | Behavior |
|------------|----------|
| **1200px+** | Full layout: sidebar + content + TOC |
| **768-1199px** | Sidebar + content (TOC hidden) |
| **< 768px** | Mobile mode: collapsible sidebar |
| **< 480px** | Optimized spacing and typography |

## ⌨️ Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+K` / `Cmd+K` | Open search |
| `Escape` | Close search or modals |
| `Click sidebar link` | Navigate to page |
| `Click TOC link` | Scroll to heading |

## 🎯 Features Breakdown

### 1. Markdown Rendering
- **Headers** — H1 through H6 with auto-generated IDs
- **Links** — Internal and external (auto-target for external)
- **Lists** — Ordered and unordered with nesting
- **Tables** — Full table support with hover effects
- **Code** — Inline code and syntax-highlighted blocks
- **Blockquotes** — Auto-styled as note/warning/tip boxes
- **Images** — Responsive with border-radius
- **Horizontal rules** — Styled dividers

### 2. Search
- **Index building** — First search triggers async indexing
- **Debounced input** — 300ms delay to prevent excessive queries
- **Result grouping** — Max 10 pages, 3 results per page
- **Context preview** — 40 chars before/after match
- **Highlighting** — Yellow highlight on matched terms
- **Keyboard navigation** — Full keyboard accessible

### 3. Table of Contents
- **Auto-generation** — Built from H2, H3, H4 headings
- **Scroll spy** — Active heading updates on scroll
- **Smooth scrolling** — Animated scroll to heading
- **Nested structure** — Indentation shows hierarchy
- **Sticky position** — TOC stays visible while scrolling

### 4. Theme System
- **Persistence** — Theme saved to localStorage
- **System sync** — Can detect OS theme preference
- **Smooth transition** — CSS transitions on all elements
- **Code theme** — Syntax highlighting matches wiki theme
- **Icon toggle** — Sun/moon icons swap visibility

## 🐛 Troubleshooting

### Pages Not Loading

**Issue:** "Page Not Found" error  
**Solution:** Check GitHub URL and ensure markdown files exist

### Search Not Working

**Issue:** Search returns no results  
**Solution:** Wait for indexing to complete (check console)

### Styles Not Applied

**Issue:** Unstyled content  
**Solution:** Ensure `wiki-styles.css` is loaded (check Network tab)

### Mobile Menu Not Opening

**Issue:** Sidebar doesn't slide in  
**Solution:** Check `menuToggle` button and JavaScript console

## 📊 Performance

- **First load:** ~500ms (including markdown fetch)
- **Cached pages:** ~50ms (instant)
- **Search indexing:** ~2-3 seconds (all 16 pages)
- **Theme toggle:** Instant
- **Code highlighting:** ~100ms per page

## 🔒 Security

- **XSS Protection** — DOMPurify sanitizes all HTML
- **CORS** — Enabled for API routes
- **External links** — Auto-added `rel="noopener noreferrer"`
- **No inline scripts** — All JS in external files
- **Content Security** — No arbitrary HTML execution

## 🌐 Browser Support

| Browser | Version |
|---------|---------|
| Chrome | 90+ ✅ |
| Firefox | 88+ ✅ |
| Safari | 14+ ✅ |
| Edge | 90+ ✅ |
| Mobile Chrome | Latest ✅ |
| Mobile Safari | Latest ✅ |

## 📦 Dependencies

### Runtime
- **marked** (11.1.1) — Markdown parser
- **dompurify** (3.0.8) — HTML sanitizer
- **highlight.js** (11.9.0) — Syntax highlighting

### Server
- **express** — Web server
- **cors** — CORS middleware
- **path** / **fs** — File system utilities

All frontend dependencies loaded via CDN (no npm install needed for HTML/CSS/JS).

## 🎓 Usage Examples

### Link to Specific Section

```
https://your-wiki.com/wiki#Commands
```

### Link to Specific Heading

```
https://your-wiki.com/wiki#Commands#admin-commands
```

### Embed in iframe

```html
<iframe src="https://your-wiki.com/wiki" width="100%" height="600"></iframe>
```

### Theme Preference

```javascript
// Set theme programmatically
localStorage.setItem('wiki-theme', 'dark');
```

## 🚀 Deployment

### Vercel (Recommended)

Already configured! Just push to GitHub and Vercel auto-deploys.

### Manual Deployment

```bash
# Build (if needed)
npm install

# Start server
npm start

# Or use PM2 for production
pm2 start index.js --name fpp-wiki
```

## 📝 Markdown Tips

### Create Note Boxes

```markdown
> **Note:** This will be styled as a blue note box
```

### Create Warning Boxes

```markdown
> **Warning:** This will be styled as an orange warning box
```

### Create Tip Boxes

```markdown
> **Tip:** This will be styled as a green tip box
```

### Code Blocks with Language

```markdown
\`\`\`yaml
config:
  enabled: true
\`\`\`
```

## 🎉 Credits

Built with:
- [Marked.js](https://marked.js.org/) — Markdown parsing
- [DOMPurify](https://github.com/cure53/DOMPurify) — XSS protection
- [highlight.js](https://highlightjs.org/) — Syntax highlighting
- [Express](https://expressjs.com/) — Web server

---

**Status:** ✅ Production Ready  
**Version:** 1.0.0  
**Last Updated:** March 26, 2026

For issues or suggestions, visit the [GitHub repository](https://github.com/Pepe-tf/Fake-Player-Plugin-Public-).

