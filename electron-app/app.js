class XPostSchedulerApp {
    constructor() {
        this.posts = [];
        this.selectedTopics = [];
        this.allTopics = [];
        this.processedIds = this.loadProcessedIds();
        this.lastETag = localStorage.getItem('lastETag');
        this.isProcessing = false;
        
        this.elements = {
            statusText: document.getElementById('status-text'),
            connectionStatus: document.getElementById('connection-status'),
            appVersion: document.getElementById('app-version'),
            postsContainer: document.getElementById('posts-container'),
            totalPosts: document.getElementById('total-posts'),
            readyPosts: document.getElementById('ready-posts'),
            avgScore: document.getElementById('avg-score'),
            selectedTopicsDisplay: document.getElementById('selected-topics-display'),
            scheduleHour: document.getElementById('schedule-hour'),
            fetchBtn: document.getElementById('fetch-btn'),
            scheduleBtn: document.getElementById('schedule-btn'),
            editTopicsBtn: document.getElementById('edit-topics-btn'),
            topicModal: document.getElementById('topic-modal'),
            progressModal: document.getElementById('progress-modal'),
            topicsGrid: document.getElementById('topics-grid'),
            customTopicInput: document.getElementById('custom-topic-input'),
            addTopicBtn: document.getElementById('add-topic-btn'),
            saveTopicsBtn: document.getElementById('save-topics-btn'),
            progressFill: document.getElementById('progress-fill'),
            progressText: document.getElementById('progress-text'),
            currentPostInfo: document.getElementById('current-post-info')
        };

        this.init();
    }

    async init() {
        await this.loadAppVersion();
        await this.loadTopics();
        this.bindEvents();
        this.startPolling();
        this.updateConnectionStatus('connected');
        
        // Show topics modal if no topics selected
        if (this.selectedTopics.length === 0) {
            this.showTopicModal();
        }
    }

    async loadAppVersion() {
        try {
            const version = await window.electronAPI.getAppVersion();
            this.elements.appVersion.textContent = `v${version}`;
        } catch (error) {
            console.error('Failed to load app version:', error);
        }
    }

    async loadTopics() {
        try {
            const topics = await window.electronAPI.fetchTopics();
            this.allTopics = topics;
            this.selectedTopics = JSON.parse(localStorage.getItem('selectedTopics') || '[]');
            
            if (this.selectedTopics.length === 0) {
                this.selectedTopics = topics.slice(0, 5); // Default to first 5 topics
            }
            
            this.updateTopicsDisplay();
        } catch (error) {
            console.error('Failed to load topics:', error);
            this.showToast('Failed to load topics', 'error');
        }
    }

    updateTopicsDisplay() {
        this.elements.selectedTopicsDisplay.innerHTML = this.selectedTopics
            .map(topic => `<span class="topic-tag">${topic}</span>`)
            .join('');
    }

    bindEvents() {
        this.elements.fetchBtn.addEventListener('click', () => this.fetchReplies());
        this.elements.scheduleBtn.addEventListener('click', () => this.scheduleAllPosts());
        this.elements.editTopicsBtn.addEventListener('click', () => this.showTopicModal());
        this.elements.addTopicBtn.addEventListener('click', () => this.addCustomTopic());
        this.elements.saveTopicsBtn.addEventListener('click', () => this.saveTopics());
        
        this.elements.customTopicInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                this.addCustomTopic();
            }
        });

        // Close modals when clicking outside
        this.elements.topicModal.addEventListener('click', (e) => {
            if (e.target === this.elements.topicModal) {
                this.hideTopicModal();
            }
        });

        // Progress updates
        window.electronAPI.onPostProgress(this.handlePostProgress.bind(this));
    }

    startPolling() {
        // Poll every 3 minutes
        setInterval(() => {
            if (!this.isProcessing) {
                this.fetchReplies(true);
            }
        }, 3 * 60 * 1000);
        
        // Initial fetch
        this.fetchReplies(true);
    }

    async fetchReplies(silent = false) {
        if (!silent) {
            this.updateConnectionStatus('connecting');
            this.elements.fetchBtn.disabled = true;
            this.elements.fetchBtn.textContent = 'Fetching...';
        }

        try {
            const { replies, etag, lastUpdated } = await window.electronAPI.fetchReplies();
            
            if (etag && etag === this.lastETag) {
                if (!silent) {
                    this.showToast('No new posts available');
                }
                return;
            }

            this.lastETag = etag;
            localStorage.setItem('lastETag', etag);

            // Filter out already processed posts
            const newPosts = replies.filter(post => !this.processedIds.includes(post.id));
            
            this.posts = newPosts;
            this.renderPosts();
            this.updateStats();
            
            if (!silent) {
                this.showToast(`Loaded ${newPosts.length} new posts`);
            }
            
            this.updateConnectionStatus('connected');
        } catch (error) {
            console.error('Failed to fetch replies:', error);
            this.updateConnectionStatus('error');
            if (!silent) {
                this.showToast('Failed to fetch posts', 'error');
            }
        } finally {
            if (!silent) {
                this.elements.fetchBtn.disabled = false;
                this.elements.fetchBtn.textContent = 'Fetch New Posts';
            }
        }
    }

    renderPosts() {
        if (this.posts.length === 0) {
            this.elements.postsContainer.innerHTML = `
                <div style="text-align: center; padding: 3rem; color: #657786;">
                    <h3>No posts available</h3>
                    <p>Click "Fetch New Posts" to load posts from Google Cloud Storage</p>
                </div>
            `;
            return;
        }

        this.elements.postsContainer.innerHTML = this.posts
            .map(post => this.renderPostCard(post))
            .join('');

        // Bind post action events
        this.bindPostActions();
    }

    renderPostCard(post) {
        const hasMedia = post.media && post.media.length > 0;
        const authorInitial = post.author.charAt(0).toUpperCase();
        
        return `
            <div class="post-card" data-post-id="${post.id}">
                <div class="post-header">
                    <div class="author-avatar">${authorInitial}</div>
                    <div class="author-info">
                        <h4>${post.author}</h4>
                        <span class="handle">@${post.handle}</span>
                    </div>
                </div>
                
                <div class="post-content">${this.truncateText(post.content, 200)}</div>
                
                ${hasMedia ? `
                    <div class="post-media">
                        <div class="media-indicator">📷 Image attached</div>
                    </div>
                ` : ''}
                
                <div class="post-meta">
                    <span>${this.formatDate(post.created_at)}</span>
                    <span class="score-badge">Score: ${post.score}</span>
                </div>
                
                ${post.categories ? `
                    <div class="categories">
                        ${post.categories.map(cat => `<span class="category-tag">${cat}</span>`).join('')}
                    </div>
                ` : ''}
                
                ${post.reply ? `
                    <div class="reply-card">
                        <div class="reply-header">
                            <div class="user-avatar">Y</div>
                            <span>Your Reply:</span>
                        </div>
                        <div class="reply-content">${post.reply}</div>
                    </div>
                ` : ''}
                
                <div class="post-actions">
                    <button class="btn btn-primary post-now-btn" data-post-id="${post.id}">
                        Post Now
                    </button>
                    <button class="btn btn-danger discard-btn" data-post-id="${post.id}">
                        Discard
                    </button>
                    <a href="${post.url}" target="_blank" class="btn btn-secondary">
                        View Original
                    </a>
                </div>
            </div>
        `;
    }

    bindPostActions() {
        // Post now buttons
        document.querySelectorAll('.post-now-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const postId = e.target.dataset.postId;
                this.postSingle(postId);
            });
        });

        // Discard buttons
        document.querySelectorAll('.discard-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const postId = e.target.dataset.postId;
                this.discardPost(postId);
            });
        });
    }

    async postSingle(postId) {
        const post = this.posts.find(p => p.id === postId);
        if (!post) return;

        const btn = document.querySelector(`[data-post-id="${postId}"].post-now-btn`);
        btn.disabled = true;
        btn.textContent = 'Posting...';

        try {
            const result = await window.electronAPI.postReply({
                postUrl: post.url,
                replyText: post.reply
            });

            if (result.success) {
                this.showToast('Reply prepared for posting');
                this.markAsProcessed(postId);
            } else {
                this.showToast(`Failed to post: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('Failed to post reply:', error);
            this.showToast('Failed to post reply', 'error');
        } finally {
            btn.disabled = false;
            btn.textContent = 'Post Now';
        }
    }

    discardPost(postId) {
        this.posts = this.posts.filter(p => p.id !== postId);
        this.markAsProcessed(postId);
        this.renderPosts();
        this.updateStats();
        this.showToast('Post discarded');
    }

    async scheduleAllPosts() {
        if (this.posts.length === 0) {
            this.showToast('No posts to schedule', 'warning');
            return;
        }

        this.isProcessing = true;
        this.elements.scheduleBtn.disabled = true;
        this.elements.scheduleBtn.textContent = 'Scheduling...';
        
        this.showProgressModal();

        try {
            const hour = parseInt(this.elements.scheduleHour.value);
            const postsToSchedule = this.posts.filter(p => p.reply && p.reply.trim());

            const result = await window.electronAPI.schedulePosts({
                hour: hour,
                posts: postsToSchedule
            });

            if (result.success) {
                this.showToast(`Scheduled ${result.processed} posts`);
                // Mark all as processed
                postsToSchedule.forEach(post => this.markAsProcessed(post.id));
                this.posts = [];
                this.renderPosts();
                this.updateStats();
            } else {
                this.showToast(`Scheduling failed: ${result.message}`, 'error');
            }
        } catch (error) {
            console.error('Failed to schedule posts:', error);
            this.showToast('Failed to schedule posts', 'error');
        } finally {
            this.isProcessing = false;
            this.elements.scheduleBtn.disabled = false;
            this.elements.scheduleBtn.textContent = 'Schedule All Posts';
            this.hideProgressModal();
        }
    }

    handlePostProgress(event, data) {
        const { completed, total, currentPost } = data;
        const progress = (completed / total) * 100;
        
        this.elements.progressFill.style.width = `${progress}%`;
        this.elements.progressText.textContent = `Processing ${completed}/${total} posts...`;
        
        if (currentPost) {
            this.elements.currentPostInfo.innerHTML = `
                <h4>Current Post:</h4>
                <p><strong>@${currentPost.handle}:</strong> ${this.truncateText(currentPost.content, 100)}</p>
                <p><strong>Reply:</strong> ${currentPost.reply}</p>
            `;
        }
    }

    showTopicModal() {
        this.renderTopicsGrid();
        this.elements.topicModal.classList.remove('hidden');
    }

    hideTopicModal() {
        this.elements.topicModal.classList.add('hidden');
    }

    showProgressModal() {
        this.elements.progressModal.classList.remove('hidden');
    }

    hideProgressModal() {
        this.elements.progressModal.classList.add('hidden');
    }

    renderTopicsGrid() {
        const allUniqueTopics = [...new Set([...this.allTopics, ...this.selectedTopics])];
        
        this.elements.topicsGrid.innerHTML = allUniqueTopics
            .map(topic => `
                <label class="topic-checkbox">
                    <input type="checkbox" value="${topic}" ${this.selectedTopics.includes(topic) ? 'checked' : ''}>
                    <span>${topic}</span>
                </label>
            `)
            .join('');
    }

    addCustomTopic() {
        const topic = this.elements.customTopicInput.value.trim();
        if (!topic) return;
        
        if (!this.allTopics.includes(topic)) {
            this.allTopics.push(topic);
            this.renderTopicsGrid();
        }
        
        this.elements.customTopicInput.value = '';
    }

    async saveTopics() {
        const checkedBoxes = this.elements.topicsGrid.querySelectorAll('input[type="checkbox"]:checked');
        this.selectedTopics = Array.from(checkedBoxes).map(cb => cb.value);
        
        if (this.selectedTopics.length === 0) {
            this.showToast('Please select at least one topic', 'warning');
            return;
        }

        try {
            localStorage.setItem('selectedTopics', JSON.stringify(this.selectedTopics));
            
            await window.electronAPI.uploadTopics(this.selectedTopics);
            
            this.updateTopicsDisplay();
            this.hideTopicModal();
            this.showToast('Topics saved successfully');
        } catch (error) {
            console.error('Failed to save topics:', error);
            this.showToast('Failed to save topics', 'error');
        }
    }

    updateStats() {
        const totalPosts = this.posts.length;
        const readyPosts = this.posts.filter(p => p.reply && p.reply.trim()).length;
        const avgScore = totalPosts > 0 ? 
            Math.round(this.posts.reduce((sum, p) => sum + (p.score || 0), 0) / totalPosts) : 0;

        this.elements.totalPosts.textContent = totalPosts;
        this.elements.readyPosts.textContent = readyPosts;
        this.elements.avgScore.textContent = avgScore;
    }

    updateConnectionStatus(status) {
        this.elements.connectionStatus.className = `status-dot ${status}`;
        
        const statusTexts = {
            connected: 'Connected',
            connecting: 'Connecting...',
            error: 'Connection Error'
        };
        
        this.elements.statusText.textContent = statusTexts[status] || status;
    }

    markAsProcessed(postId) {
        if (!this.processedIds.includes(postId)) {
            this.processedIds.push(postId);
            localStorage.setItem('processedIds', JSON.stringify(this.processedIds));
        }
    }

    loadProcessedIds() {
        return JSON.parse(localStorage.getItem('processedIds') || '[]');
    }

    showToast(message, type = 'success') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        
        document.getElementById('toast-container').appendChild(toast);
        
        setTimeout(() => {
            toast.remove();
        }, 3000);
    }

    truncateText(text, maxLength) {
        if (text.length <= maxLength) return text;
        return text.substring(0, maxLength) + '...';
    }

    formatDate(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const diffInHours = (now - date) / (1000 * 60 * 60);
        
        if (diffInHours < 1) {
            return `${Math.floor(diffInHours * 60)}m ago`;
        } else if (diffInHours < 24) {
            return `${Math.floor(diffInHours)}h ago`;
        } else {
            return `${Math.floor(diffInHours / 24)}d ago`;
        }
    }
}

// Initialize the app when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new XPostSchedulerApp();
});