// ==================== CONFIGURATION ====================

const WIKI_BASE_URL = 'https://raw.githubusercontent.com/Pepe-tf/Fake-Player-Plugin-Public-/main/wiki/';
const DEFAULT_PAGE = 'Home';

// ==================== STATE ====================

let currentPage = DEFAULT_PAGE;
let wikiContent = {};
let searchIndex = [];

// ==================== THEME ====================

function initTheme() {
    const savedTheme = localStorage.getItem('wiki-theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateHighlightTheme(savedTheme);
}

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme');
    const next = current === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('wiki-theme', next);
    updateHighlightTheme(next);
}

function updateHighlightTheme(theme) {
    const link = document.getElementById('highlight-theme');
    if (link) {
        link.href = theme === 'dark'
            ? 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css'
            : 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css';
    }
}

// ==================== NAVIGATION ====================

function initNavigation() {
    // Sidebar links
    document.querySelectorAll('.sidebar-nav a').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const page = link.getAttribute('data-page');
            if (page) {
                loadPage(page);
                setActivePage(page);
                closeMobileMenu();
            }
        });
    });

    // Mobile menu toggle
    const menuToggle = document.getElementById('menuToggle');
    const sidebar = document.getElementById('sidebar');

    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
        });
    }

    // Close menu when clicking outside
    document.addEventListener('click', (e) => {
        if (sidebar.classList.contains('active') &&
            !sidebar.contains(e.target) &&
            !menuToggle.contains(e.target)) {
            sidebar.classList.remove('active');
        }
    });
}

function closeMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.remove('active');
}

function setActivePage(page) {
    document.querySelectorAll('.sidebar-nav a').forEach(link => {
        link.classList.remove('active');
        if (link.getAttribute('data-page') === page) {
            link.classList.add('active');
        }
    });
    currentPage = page;
    updateURL(page);
}

function updateURL(page) {
    const url = new URL(window.location);
    url.hash = page;
    window.history.pushState({}, '', url);
}

// ==================== PAGE LOADING ====================

async function loadPage(page) {
    const content = document.getElementById('content');

    // Show loading state
    content.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <p>Loading ${page}...</p>
        </div>
    `;

    try {
        // Try to fetch from GitHub
        const url = `${WIKI_BASE_URL}${page}.md`;
        const response = await fetch(url);

        if (!response.ok) {
            throw new Error(`Failed to load ${page}`);
        }

        const markdown = await response.text();

        // Cache the content
        wikiContent[page] = markdown;

        // Render the page
        renderMarkdown(markdown);

        // Update TOC
        generateTOC();

        // Update page navigation
        updatePageNavigation();

        // Scroll to top
        window.scrollTo({ top: 0, behavior: 'smooth' });

    } catch (error) {
        console.error('Error loading page:', error);
        content.innerHTML = `
            <div class="error-state">
                <h1>⚠️ Page Not Found</h1>
                <p>The page <strong>${page}</strong> could not be loaded.</p>
                <p>This might be a temporary network issue or the page doesn't exist.</p>
                <button onclick="loadPage('${DEFAULT_PAGE}')" class="btn-primary">
                    Go to Home Page
                </button>
            </div>
        `;
    }
}

function renderMarkdown(markdown) {
    const content = document.getElementById('content');

    // Configure marked
    marked.setOptions({
        breaks: true,
        gfm: true,
        headerIds: true,
        mangle: false
    });

    // Parse markdown
    let html = marked.parse(markdown);

    // Sanitize HTML
    html = DOMPurify.sanitize(html, {
        ADD_ATTR: ['target', 'rel'],
        ADD_TAGS: ['iframe']
    });

    // Render
    content.innerHTML = html;

    // Append page navigation
    const pageNavHTML = `
        <nav id="pageNav" class="page-navigation" style="display: none;">
            <a href="#" id="prevPage" class="page-nav-btn prev-page">
                <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="m15 18-6-6 6-6"/>
                </svg>
                <span class="nav-text">
                    <span class="nav-label">Previous</span>
                    <span class="nav-title"></span>
                </span>
            </a>
            <a href="#" id="nextPage" class="page-nav-btn next-page">
                <span class="nav-text">
                    <span class="nav-label">Next</span>
                    <span class="nav-title"></span>
                </span>
                <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path d="m9 18 6-6-6-6"/>
                </svg>
            </a>
        </nav>
    `;
    content.insertAdjacentHTML('beforeend', pageNavHTML);

    // Add target="_blank" to external links
    content.querySelectorAll('a[href^="http"]').forEach(link => {
        link.setAttribute('target', '_blank');
        link.setAttribute('rel', 'noopener noreferrer');
    });

    // Highlight code blocks
    content.querySelectorAll('pre code').forEach(block => {
        hljs.highlightElement(block);
    });

    // Add copy button to code blocks
    content.querySelectorAll('pre').forEach(pre => {
        addCopyButton(pre);
    });

    // Process special blockquotes into alert boxes
    content.querySelectorAll('blockquote').forEach(blockquote => {
        const text = blockquote.textContent.trim();
        if (text.startsWith('Note:') || text.startsWith('**Note:**')) {
            blockquote.classList.add('note');
        } else if (text.startsWith('Warning:') || text.startsWith('**Warning:**')) {
            blockquote.classList.add('warning');
        } else if (text.startsWith('Tip:') || text.startsWith('**Tip:**')) {
            blockquote.classList.add('tip');
        }
    });
}

function addCopyButton(pre) {
    const button = document.createElement('button');
    button.className = 'copy-code-btn';
    button.textContent = 'Copy';
    button.style.cssText = `
        position: absolute;
        top: 8px;
        right: 8px;
        padding: 4px 8px;
        font-size: 0.75rem;
        background: var(--bg-secondary);
        border: 1px solid var(--border-color);
        border-radius: 4px;
        cursor: pointer;
        opacity: 0;
        transition: opacity 0.2s;
    `;

    pre.style.position = 'relative';
    pre.appendChild(button);

    pre.addEventListener('mouseenter', () => {
        button.style.opacity = '1';
    });

    pre.addEventListener('mouseleave', () => {
        button.style.opacity = '0';
    });

    button.addEventListener('click', () => {
        const code = pre.querySelector('code').textContent;
        navigator.clipboard.writeText(code).then(() => {
            button.textContent = 'Copied!';
            setTimeout(() => {
                button.textContent = 'Copy';
            }, 2000);
        });
    });
}

// ==================== TABLE OF CONTENTS ====================

function generateTOC() {
    const content = document.getElementById('content');
    const tocNav = document.getElementById('tocNav');
    const headings = content.querySelectorAll('h2, h3, h4');

    if (headings.length === 0) {
        tocNav.innerHTML = '<p class="toc-empty">No headings found</p>';
        return;
    }

    tocNav.innerHTML = '';

    headings.forEach(heading => {
        const level = heading.tagName.toLowerCase();
        const text = heading.textContent;
        const id = heading.id || text.toLowerCase().replace(/[^\w]+/g, '-');

        // Ensure heading has an ID
        heading.id = id;

        const link = document.createElement('a');
        link.href = `#${id}`;
        link.textContent = text;
        link.className = `toc-${level}`;

        link.addEventListener('click', (e) => {
            e.preventDefault();
            heading.scrollIntoView({ behavior: 'smooth', block: 'start' });

            // Update active state
            tocNav.querySelectorAll('a').forEach(a => a.classList.remove('active'));
            link.classList.add('active');
        });

        tocNav.appendChild(link);
    });

    // Highlight TOC on scroll
    initTOCScrollSpy();
}

function initTOCScrollSpy() {
    const headings = document.querySelectorAll('#content h2, #content h3, #content h4');
    const tocLinks = document.querySelectorAll('#tocNav a');

    if (headings.length === 0) return;

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.id;
                tocLinks.forEach(link => {
                    link.classList.remove('active');
                    if (link.getAttribute('href') === `#${id}`) {
                        link.classList.add('active');
                    }
                });
            }
        });
    }, {
        rootMargin: '-100px 0px -66%',
        threshold: 0
    });

    headings.forEach(heading => observer.observe(heading));
}

// ==================== SEARCH ====================

function initSearch() {
    const searchBtn = document.getElementById('searchBtn');
    const searchModal = document.getElementById('searchModal');
    const closeSearch = document.getElementById('closeSearch');
    const searchInput = document.getElementById('searchInput');

    // Open search
    searchBtn.addEventListener('click', openSearch);

    // Close search
    closeSearch.addEventListener('click', closeSearchModal);

    // Close on outside click
    searchModal.addEventListener('click', (e) => {
        if (e.target === searchModal) {
            closeSearchModal();
        }
    });

    // Close on Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && searchModal.classList.contains('active')) {
            closeSearchModal();
        }
    });

    // Ctrl+K to open search
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
            e.preventDefault();
            openSearch();
        }
    });

    // Search input
    let searchTimeout;
    searchInput.addEventListener('input', (e) => {
        clearTimeout(searchTimeout);
        searchTimeout = setTimeout(() => {
            performSearch(e.target.value);
        }, 300);
    });
}

function openSearch() {
    const searchModal = document.getElementById('searchModal');
    const searchInput = document.getElementById('searchInput');
    searchModal.classList.add('active');
    searchInput.focus();
    buildSearchIndex();
}

function closeSearchModal() {
    const searchModal = document.getElementById('searchModal');
    const searchInput = document.getElementById('searchInput');
    const searchResults = document.getElementById('searchResults');
    searchModal.classList.remove('active');
    searchInput.value = '';
    searchResults.innerHTML = '<div class="search-hint">Start typing to search...</div>';
}

async function buildSearchIndex() {
    if (searchIndex.length > 0) return; // Already built

    const pages = [
        'Home', 'Getting-Started', 'FAQ', 'Commands', 'Permissions',
        'Configuration', 'Language', 'Bot-Names', 'Bot-Messages',
        'Bot-Behaviour', 'Skin-System', 'Swap-System', 'Fake-Chat',
        'Placeholders', 'Database', 'Migration'
    ];

    for (const page of pages) {
        try {
            if (!wikiContent[page]) {
                const response = await fetch(`${WIKI_BASE_URL}${page}.md`);
                if (response.ok) {
                    wikiContent[page] = await response.text();
                }
            }

            if (wikiContent[page]) {
                // Index page content
                const lines = wikiContent[page].split('\n');
                lines.forEach((line, index) => {
                    if (line.trim()) {
                        searchIndex.push({
                            page,
                            line: index,
                            content: line.replace(/[#*`]/g, '').trim()
                        });
                    }
                });
            }
        } catch (error) {
            console.error(`Failed to index ${page}:`, error);
        }
    }
}

function performSearch(query) {
    const searchResults = document.getElementById('searchResults');

    if (!query || query.length < 2) {
        searchResults.innerHTML = '<div class="search-hint">Type at least 2 characters to search...</div>';
        return;
    }

    const results = [];
    const queryLower = query.toLowerCase();
    const seen = new Set();

    searchIndex.forEach(item => {
        if (item.content.toLowerCase().includes(queryLower)) {
            const key = `${item.page}:${item.line}`;
            if (!seen.has(key)) {
                seen.add(key);
                results.push(item);
            }
        }
    });

    if (results.length === 0) {
        searchResults.innerHTML = `
            <div class="search-hint">
                No results found for "<strong>${escapeHtml(query)}</strong>"
            </div>
        `;
        return;
    }

    // Group by page
    const grouped = {};
    results.forEach(item => {
        if (!grouped[item.page]) {
            grouped[item.page] = [];
        }
        grouped[item.page].push(item);
    });

    // Render results
    let html = '';
    Object.keys(grouped).slice(0, 10).forEach(page => {
        const items = grouped[page].slice(0, 3);
        items.forEach(item => {
            const excerpt = highlightMatch(item.content, query);
            html += `
                <div class="search-result" onclick="navigateToResult('${page}')">
                    <div class="search-result-title">${formatPageTitle(page)}</div>
                    <div class="search-result-excerpt">${excerpt}</div>
                </div>
            `;
        });
    });

    searchResults.innerHTML = html;
}

function highlightMatch(text, query) {
    const index = text.toLowerCase().indexOf(query.toLowerCase());
    if (index === -1) return escapeHtml(text);

    const start = Math.max(0, index - 40);
    const end = Math.min(text.length, index + query.length + 40);

    let excerpt = text.substring(start, end);
    if (start > 0) excerpt = '...' + excerpt;
    if (end < text.length) excerpt = excerpt + '...';

    const regex = new RegExp(`(${escapeRegex(query)})`, 'gi');
    excerpt = escapeHtml(excerpt).replace(regex, '<mark>$1</mark>');

    return excerpt;
}

function navigateToResult(page) {
    closeSearchModal();
    loadPage(page);
    setActivePage(page);
}

function formatPageTitle(page) {
    return page.replace(/-/g, ' ');
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function escapeRegex(str) {
    return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// ==================== BACK TO TOP ====================

function initBackToTop() {
    const button = document.getElementById('backToTop');

    window.addEventListener('scroll', () => {
        if (window.scrollY > 400) {
            button.classList.add('visible');
        } else {
            button.classList.remove('visible');
        }
    });

    button.addEventListener('click', () => {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
}

// ==================== PAGE NAVIGATION ====================

// Define page order for prev/next navigation
const PAGE_ORDER = [
    'Home',
    'Getting-Started',
    'FAQ',
    'Commands',
    'Permissions',
    'Configuration',
    'Language',
    'Bot-Names',
    'Bot-Messages',
    'Bot-Behaviour',
    'Skin-System',
    'Swap-System',
    'Fake-Chat',
    'Placeholders',
    'Database',
    'Migration'
];

function updatePageNavigation() {
    const pageNav = document.getElementById('pageNav');
    const prevBtn = document.getElementById('prevPage');
    const nextBtn = document.getElementById('nextPage');

    const currentIndex = PAGE_ORDER.indexOf(currentPage);

    if (currentIndex === -1) {
        pageNav.style.display = 'none';
        return;
    }

    // Hide both buttons initially
    prevBtn.style.display = 'none';
    nextBtn.style.display = 'none';

    // Show prev button if not first page
    if (currentIndex > 0) {
        const prevPage = PAGE_ORDER[currentIndex - 1];
        prevBtn.style.display = 'flex';
        prevBtn.href = `#${prevPage}`;
        prevBtn.querySelector('.nav-title').textContent = formatPageTitle(prevPage);
        prevBtn.onclick = (e) => {
            e.preventDefault();
            loadPage(prevPage);
            setActivePage(prevPage);
        };
    }

    // Show next button if not last page
    if (currentIndex < PAGE_ORDER.length - 1) {
        const nextPage = PAGE_ORDER[currentIndex + 1];
        nextBtn.style.display = 'flex';
        nextBtn.href = `#${nextPage}`;
        nextBtn.querySelector('.nav-title').textContent = formatPageTitle(nextPage);
        nextBtn.onclick = (e) => {
            e.preventDefault();
            loadPage(nextPage);
            setActivePage(nextPage);
        };
    }

    // Show navigation if at least one button is visible
    if (prevBtn.style.display !== 'none' || nextBtn.style.display !== 'none') {
        pageNav.style.display = 'flex';
    } else {
        pageNav.style.display = 'none';
    }
}

// ==================== INITIALIZATION ====================

document.addEventListener('DOMContentLoaded', () => {
    // Initialize theme
    initTheme();

    // Theme toggle
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);

    // Initialize navigation
    initNavigation();

    // Initialize search
    initSearch();

    // Initialize back to top
    initBackToTop();

    // Load initial page from URL hash or default
    const hash = window.location.hash.substring(1);
    const initialPage = hash || DEFAULT_PAGE;
    loadPage(initialPage);
    setActivePage(initialPage);

    // Handle browser back/forward
    window.addEventListener('popstate', () => {
        const page = window.location.hash.substring(1) || DEFAULT_PAGE;
        loadPage(page);
        setActivePage(page);
    });
});

