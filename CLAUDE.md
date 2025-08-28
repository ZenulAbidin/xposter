CLAUDE.md
Project Overview
This project automates engaging with posts on X (formerly Twitter) from the user's For You page. The system captures recent posts (less than 2 days old, not liked by the user), filters them based on dynamic user-selected topics for relevance (scoring >=50 using AI classification), generates short thoughtful replies (50-150 characters, incorporating image descriptions if present), and allows manual review/discarding before scheduled or manual posting with randomized delays (15-45 seconds between posts) to avoid detection. Target volume: 300-400 posts per day for increased engagement.
Key principles:

Personal use only; comply with X's terms (no full automation of posting; user taps to confirm).
Use Google Cloud Storage (GCS) for syncing data (replies.json, topics.json, cookies_x.json) with polling (every 1-5 minutes) to avoid duplicates and costs.
AI: Use Gemini 2.5 Flash Lite for multimodal classification/scoring and reply generation (handles text and images via base64).
Dynamic topics: User selects/deselects from predefined list (e.g., AI, Crypto, Bitcoin, Ethereum, Motivation, Machine Learning, Blockchain, Self-Improvement, Tech Innovation, Programming, Science, Gaming, Environment, Finance, Health & Wellness) plus custom additions; saved locally and uploaded to GCS for sync.
De-duplication: Use ETag/last-modified for file changes; track processed post IDs locally.
Filtering: Skip liked posts, posts older than 2 days.
Costs: Optimize for free tiers; no WebSocket or always-on services.

Shared Configurations:

GCS Bucket: your-bucket-name (replace with actual).
Gemini API Key: your_gemini_api_key (from ai.google.dev; enable billing if needed).
Google Cloud Project ID: your-project-id.
Service Account Key: For GCS access (key.json or equivalent).
Predefined Topics: Array as listed above.

Component 1: Chrome Extension (Request Capture)

Purpose: Capture GraphQL requests on x.com/home as user scrolls, extract post data (ID, content, author, handle, created_at, media URLs, skip if favorited/liked), filter <2 days old, export for_you_posts.json and cookies_x.json.
Files:

manifest.json: Manifest V3, permissions: ["debugger", "storage", "cookies", "downloads", "tabs"], host_permissions for x.com, background service worker, content script for /home, popup.
background.js: Attach debugger, enable Network, listen for HomeTimeline responses, parse JSON for posts/media/liked, store in array, export on message.
content.js: Send startCapture on /home load.
popup.html/js: Buttons to export posts JSON and cookies JSON (use chrome.cookies.getAll for domain 'x.com').


Usage: Load unpacked, scroll on For You page, export files, place in n8n-monitored folder.

Component 2: n8n Workflows

Setup: Self-hosted n8n (npm/Docker), with Google Cloud credentials for GCS and HTTP for Gemini.
Workflow 1: Main Processing (replies generation)

Trigger: Cron (every 30 min) or file watch if possible.
Nodes:

GCS Download: topics.json (array of selected topics).
Read File: for_you_posts.json (array of posts).
Code: Prepare loop items with topics.
Loop Over Items.
If: Has photo media (media.length >0 and type='photo').
HTTP GET: Fetch first image URL as binary.
Set: Base64 encode image, mime 'image/jpeg'.
HTTP POST: Gemini scoring/classification (model: gemini-2.5-flash-lite, multimodal if image, prompt: classify/score 0-100 based on topics, output format: Categories: [list]. Score: N.).
Code: Parse score/categories from response.
If: Score >=50.
HTTP POST: Gemini reply generation (similar, multimodal, prompt for 50-150 char reply).
Set/Aggregate/Write File: Add score/categories/reply/image_desc, aggregate array with last_updated timestamp.
GCS Upload: replies.json.




Workflow 2: Cleanup Stale Replies

Trigger: Cron (daily at 00:00 UTC).
GCS Get: replies.json (binary).
Binary to JSON.
Code: Filter entries where created_at > now - 2 days (use Date).
If: Filtered length >0.

True: JSON to Binary, GCS Update replies.json.
False: GCS Delete replies.json.





Component 3: Electron Desktop App

Purpose: Poll GCS for replies.json (1-5 min interval), detect changes via ETag, filter new/unprocessed posts, display X-style post/reply cards (with user profile placeholder), allow discard, schedule auto-posting at user-set hour (0-23) with 15-45s random delays, embed BrowserView for injection/click simulation (user confirms if needed), load cookies for auto-login without clearing.
Dependencies: electron, @google-cloud/storage.
Files:

main.js: Create window, IPC for fetch/schedule/load-cookies, GCS setup, BrowserWindow/View for posting (set cookies, load URL, inject text, optional auto-click post button).
index.html: UI with schedule hour input, fetch/schedule buttons, posts-container div; styles for post-card (content, author), reply-card (profile img/name/handle, reply text), discard button.
preload.js: Optional for security.
app.js (in <script>): Poll logic (setInterval, getMetadata for ETag, download if changed, filter !processedIds, update UI, markAsProcessed after post; localStorage for ETag/IDs).


Topic UI: On load if no local topics, show checkboxes for predefined + custom input/add, save to localStorage, upload to GCS via storage.create.

Component 4: Android App

Purpose: Similar to Electron: Poll GCS (WorkManager for background), change detection via ETag, filter new, display posts/replies, discard, process with 15s+ delays, WebView for loading post URLs, inject reply text, auto-login via cookies, topic selection UI.
Dependencies: com.google.cloud:google-cloud-storage, androidx.work.
MainActivity.java: GCS init, SharedPreferences for ETag/processedIds/topics, Handler/Runnable for polling (get ETag, download if changed, filter, update UI), WebView setup (JS enabled, onPageFinished for injection), Thread.sleep for delays.
Topic Selection: Startup LinearLayout with CheckBoxes for predefined, EditText+Button for custom, save to prefs, upload to GCS.
XML: activity_main.xml with WebView, optional list for posts.
Background: Use WorkManager for persistent polling.

Additional Requirements

Handle images: Only first photo; base64 in Gemini prompts.
Replies: 1-2 sentences, on-topic, engaging.
De-dupe: Across apps via local storage; reset if needed.
Errors: Retry polling with backoff, log.
Testing: Small batches, anonymized data.

