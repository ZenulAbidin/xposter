class XPostCapture {
  constructor() {
    this.posts = [];
    this.isCapturing = false;
    this.debuggeeId = null;
  }

  async startCapture(tabId) {
    if (this.isCapturing) return;
    
    this.debuggeeId = { tabId };
    this.isCapturing = true;
    
    try {
      await chrome.debugger.attach(this.debuggeeId, "1.0");
      await chrome.debugger.sendCommand(this.debuggeeId, "Network.enable");
      
      chrome.debugger.onEvent.addListener(this.handleNetworkEvent.bind(this));
      console.log('Started capturing X posts...');
    } catch (error) {
      console.error('Failed to start capture:', error);
      this.isCapturing = false;
    }
  }

  async stopCapture() {
    if (!this.isCapturing || !this.debuggeeId) return;
    
    try {
      chrome.debugger.onEvent.removeListener(this.handleNetworkEvent.bind(this));
      await chrome.debugger.detach(this.debuggeeId);
    } catch (error) {
      console.error('Error stopping capture:', error);
    }
    
    this.isCapturing = false;
    this.debuggeeId = null;
  }

  handleNetworkEvent(debuggeeId, message, params) {
    if (message === 'Network.responseReceived') {
      const { response } = params;
      if (this.isHomeTimelineRequest(response.url)) {
        chrome.debugger.sendCommand(debuggeeId, "Network.getResponseBody", {
          requestId: params.requestId
        }, (result) => {
          if (result && result.body) {
            this.parseHomeTimelineResponse(result.body);
          }
        });
      }
    }
  }

  isHomeTimelineRequest(url) {
    return url.includes('HomeTimeline') || 
           url.includes('UserTweets') ||
           (url.includes('graphql') && url.includes('HomeTimeline'));
  }

  parseHomeTimelineResponse(responseBody) {
    try {
      const data = JSON.parse(responseBody);
      const entries = this.extractTimelineEntries(data);
      
      entries.forEach(entry => {
        const post = this.extractPostData(entry);
        if (post && this.isValidPost(post)) {
          this.addPost(post);
        }
      });
    } catch (error) {
      console.error('Error parsing response:', error);
    }
  }

  extractTimelineEntries(data) {
    const entries = [];
    
    const findEntries = (obj) => {
      if (Array.isArray(obj)) {
        obj.forEach(findEntries);
      } else if (obj && typeof obj === 'object') {
        if (obj.entryId && obj.content) {
          entries.push(obj);
        }
        Object.values(obj).forEach(findEntries);
      }
    };
    
    findEntries(data);
    return entries;
  }

  extractPostData(entry) {
    try {
      const tweetResult = entry.content?.itemContent?.tweet_results?.result;
      if (!tweetResult || !tweetResult.legacy) return null;

      const legacy = tweetResult.legacy;
      const user = tweetResult.core?.user_results?.result?.legacy;
      
      if (!user) return null;

      const media = this.extractMedia(legacy.entities?.media || []);
      const createdAt = new Date(legacy.created_at);
      const twoDaysAgo = new Date(Date.now() - 2 * 24 * 60 * 60 * 1000);

      if (createdAt < twoDaysAgo) return null;
      if (legacy.favorited) return null;

      return {
        id: legacy.id_str,
        content: legacy.full_text || legacy.text || '',
        author: user.name,
        handle: user.screen_name,
        created_at: legacy.created_at,
        media: media,
        likes: legacy.favorite_count || 0,
        retweets: legacy.retweet_count || 0,
        replies: legacy.reply_count || 0,
        url: `https://x.com/${user.screen_name}/status/${legacy.id_str}`
      };
    } catch (error) {
      console.error('Error extracting post data:', error);
      return null;
    }
  }

  extractMedia(mediaEntities) {
    return mediaEntities
      .filter(media => media.type === 'photo')
      .map(media => ({
        type: 'photo',
        url: media.media_url_https,
        alt_text: media.alt_text || ''
      }));
  }

  isValidPost(post) {
    return post.id && 
           post.content && 
           post.author && 
           post.handle &&
           !this.posts.find(p => p.id === post.id);
  }

  addPost(post) {
    this.posts.push(post);
    console.log(`Captured post: ${post.id} by @${post.handle}`);
  }

  async exportPosts() {
    const data = JSON.stringify(this.posts, null, 2);
    
    // First download locally for backup
    const blob = new Blob([data], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    
    try {
      await chrome.downloads.download({
        url: url,
        filename: 'for_you_posts.json',
        saveAs: true
      });
      
      // Also upload to GCS for n8n processing
      await this.uploadToGCS('for_you_posts.json', data);
      console.log(`Exported ${this.posts.length} posts to local and GCS`);
    } catch (error) {
      console.error('Export failed:', error);
    }
  }

  async uploadToGCS(filename, data) {
    try {
      // Note: This requires service account key to be configured
      // For now, user must manually upload the downloaded file to GCS
      // TODO: Implement proper GCS upload with authentication
      console.log('Manual GCS upload required for:', filename);
    } catch (error) {
      console.error('GCS upload failed:', error);
    }
  }

  async exportCookies() {
    try {
      const cookies = await chrome.cookies.getAll({ domain: '.x.com' });
      const twitterCookies = await chrome.cookies.getAll({ domain: '.twitter.com' });
      
      const allCookies = [...cookies, ...twitterCookies];
      const data = JSON.stringify(allCookies, null, 2);
      const blob = new Blob([data], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      
      await chrome.downloads.download({
        url: url,
        filename: 'cookies_x.json',
        saveAs: true
      });
      console.log(`Exported ${allCookies.length} cookies`);
    } catch (error) {
      console.error('Cookie export failed:', error);
    }
  }

  getStats() {
    return {
      totalPosts: this.posts.length,
      isCapturing: this.isCapturing,
      postsWithMedia: this.posts.filter(p => p.media.length > 0).length
    };
  }
}

const postCapture = new XPostCapture();

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  switch (request.action) {
    case 'startCapture':
      postCapture.startCapture(sender.tab.id);
      break;
    case 'stopCapture':
      postCapture.stopCapture();
      break;
    case 'exportPosts':
      postCapture.exportPosts();
      break;
    case 'exportCookies':
      postCapture.exportCookies();
      break;
    case 'getStats':
      sendResponse(postCapture.getStats());
      break;
  }
});

chrome.tabs.onUpdated.addListener((tabId, changeInfo, tab) => {
  if (changeInfo.status === 'complete' && postCapture.isCapturing) {
    if (!tab.url || (!tab.url.includes('x.com/home') && !tab.url.includes('twitter.com/home'))) {
      postCapture.stopCapture();
    }
  }
});

chrome.debugger.onDetach.addListener((source, reason) => {
  if (reason === 'target_closed' || reason === 'canceled_by_user') {
    postCapture.isCapturing = false;
  }
});