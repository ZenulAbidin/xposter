const { app, BrowserWindow, ipcMain, BrowserView } = require('electron');
const { Storage } = require('@google-cloud/storage');
const path = require('path');
const fs = require('fs');
const { circuitBreakers } = require('./circuit-breaker');

class XPostScheduler {
  constructor() {
    this.mainWindow = null;
    this.postingView = null;
    this.storage = null;
    this.bucket = null;
    this.isProcessing = false;
    this.scheduledHour = 9; // Default to 9 AM
  }

  async initialize() {
    try {
      this.storage = new Storage({
        keyFilename: path.join(__dirname, 'key.json'), // Service account key
        projectId: 'your-project-id'
      });
      this.bucket = this.storage.bucket('your-bucket-name');
      console.log('GCS initialized successfully');
    } catch (error) {
      console.error('Failed to initialize GCS:', error);
    }
  }

  createWindow() {
    this.mainWindow = new BrowserWindow({
      width: 1200,
      height: 800,
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true,
        preload: path.join(__dirname, 'preload.js')
      },
      icon: path.join(__dirname, 'assets', 'icon.png'),
      titleBarStyle: 'default'
    });

    this.mainWindow.loadFile('index.html');

    if (process.argv.includes('--dev')) {
      this.mainWindow.webContents.openDevTools();
    }

    this.mainWindow.on('closed', () => {
      this.mainWindow = null;
      if (this.postingView) {
        this.postingView.destroy();
        this.postingView = null;
      }
    });
  }

  createPostingView() {
    if (this.postingView) return this.postingView;

    this.postingView = new BrowserView({
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true,
        partition: 'persist:x-session'
      }
    });

    this.mainWindow.setBrowserView(this.postingView);
    this.postingView.setBounds({ x: 0, y: 0, width: 0, height: 0 }); // Hidden initially
    
    return this.postingView;
  }

  async fetchRepliesFromGCS() {
    return await circuitBreakers.gcs.call(async () => {
      // Check for processing lock to avoid race conditions
      const lockFile = this.bucket.file('processing.lock');
      const [lockExists] = await lockFile.exists();
      
      if (lockExists) {
        const [lockMetadata] = await lockFile.getMetadata();
        const lockAge = Date.now() - Date.parse(lockMetadata.timeCreated);
        
        // Clear stale locks older than 5 minutes
        if (lockAge > 300000) {
          try {
            await lockFile.delete();
            console.log('Cleared stale processing lock');
          } catch (deleteError) {
            console.warn('Failed to clear stale lock:', deleteError);
          }
        } else {
          console.log('Another instance is processing, skipping fetch');
          return { replies: [], etag: null, locked: true };
        }
      }

      const file = this.bucket.file('replies.json');
      const [exists] = await file.exists();
      
      if (!exists) {
        return { replies: [], etag: null };
      }

      const [metadata] = await file.getMetadata();
      const [contents] = await file.download();
      const data = JSON.parse(contents.toString());

      return {
        replies: data.replies || [],
        etag: metadata.etag,
        lastUpdated: data.last_updated
      };
    }, 'GCS-FetchReplies');
  }

  async createProcessingLock() {
    try {
      const lockFile = this.bucket.file('processing.lock');
      const lockData = {
        instance: 'electron-app',
        timestamp: new Date().toISOString(),
        pid: process.pid
      };
      await lockFile.save(JSON.stringify(lockData));
      console.log('Created processing lock');
      return true;
    } catch (error) {
      console.error('Failed to create processing lock:', error);
      return false;
    }
  }

  async releaseProcessingLock() {
    try {
      const lockFile = this.bucket.file('processing.lock');
      await lockFile.delete();
      console.log('Released processing lock');
    } catch (error) {
      console.warn('Failed to release processing lock:', error);
    }
  }

  async fetchTopicsFromGCS() {
    try {
      const file = this.bucket.file('topics.json');
      const [exists] = await file.exists();
      
      if (!exists) {
        return this.getDefaultTopics();
      }

      const [contents] = await file.download();
      const data = JSON.parse(contents.toString());
      return data.topics || this.getDefaultTopics();
    } catch (error) {
      console.error('Failed to fetch topics from GCS:', error);
      return this.getDefaultTopics();
    }
  }

  getDefaultTopics() {
    return [
      'AI', 'Crypto', 'Bitcoin', 'Ethereum', 'Motivation', 
      'Machine Learning', 'Blockchain', 'Self-Improvement', 
      'Tech Innovation', 'Programming', 'Science', 'Gaming', 
      'Environment', 'Finance', 'Health & Wellness'
    ];
  }

  async uploadTopicsToGCS(topics) {
    try {
      const file = this.bucket.file('topics.json');
      const data = JSON.stringify({ topics, last_updated: new Date().toISOString() }, null, 2);
      await file.save(data, { metadata: { contentType: 'application/json' } });
      console.log('Topics uploaded to GCS');
      return true;
    } catch (error) {
      console.error('Failed to upload topics to GCS:', error);
      return false;
    }
  }

  async loadCookiesForPosting() {
    try {
      const cookiesPath = path.join(__dirname, 'cookies_x.json');
      if (!fs.existsSync(cookiesPath)) {
        throw new Error('Cookies file not found');
      }

      const cookiesData = fs.readFileSync(cookiesPath, 'utf8');
      const cookies = JSON.parse(cookiesData);

      if (!this.postingView) {
        this.createPostingView();
      }

      for (const cookie of cookies) {
        await this.postingView.webContents.session.cookies.set({
          url: `https://${cookie.domain}`,
          name: cookie.name,
          value: cookie.value,
          domain: cookie.domain,
          path: cookie.path,
          secure: cookie.secure,
          httpOnly: cookie.httpOnly,
          expirationDate: cookie.expirationDate
        });
      }

      console.log('Cookies loaded successfully');
      return true;
    } catch (error) {
      console.error('Failed to load cookies:', error);
      return false;
    }
  }

  async postReply(postUrl, replyText) {
    return await circuitBreakers.posting.call(async () => {
      if (!this.postingView) {
        await this.loadCookiesForPosting();
      }

      return new Promise((resolve, reject) => {
        // Set timeout for the entire posting operation
        const operationTimeout = setTimeout(() => {
          reject(new Error('Post operation timeout after 45 seconds'));
        }, 45000);

        this.postingView.webContents.loadURL(postUrl);

        this.postingView.webContents.once('did-finish-load', async () => {
          try {
            await new Promise(resolve => setTimeout(resolve, 3000)); // Wait for page load

            const injectionScript = `
              (function() {
                try {
                  const replyButton = document.querySelector('[data-testid="reply"]');
                  if (replyButton) {
                    replyButton.click();
                    setTimeout(() => {
                      const textArea = document.querySelector('[data-testid="tweetTextarea_0"]');
                      if (textArea) {
                        textArea.focus();
                        textArea.textContent = "${replyText.replace(/"/g, '\\"')}";
                        textArea.dispatchEvent(new Event('input', { bubbles: true }));
                        
                        setTimeout(() => {
                          const postButton = document.querySelector('[data-testid="tweetButtonInline"]');
                          if (postButton && !postButton.disabled) {
                            return 'ready_to_post';
                          }
                          return 'post_button_not_ready';
                        }, 1000);
                      } else {
                        return 'textarea_not_found';
                      }
                    }, 1000);
                  } else {
                    return 'reply_button_not_found';
                  }
                } catch (error) {
                  return 'injection_error: ' + error.message;
                }
              })();
            `;

            const result = await this.postingView.webContents.executeJavaScript(injectionScript);
            
            clearTimeout(operationTimeout);
            
            if (result === 'ready_to_post') {
              resolve({ success: true, message: 'Reply prepared, user can confirm posting' });
            } else {
              resolve({ success: false, message: result });
            }
          } catch (error) {
            clearTimeout(operationTimeout);
            reject(error);
          }
        });

        // Handle page load failures
        this.postingView.webContents.once('did-fail-load', (event, errorCode, errorDescription) => {
          clearTimeout(operationTimeout);
          reject(new Error(`Page load failed: ${errorDescription}`));
        });
      });
    }, 'X-Posting');
  }

  setupIPC() {
    ipcMain.handle('fetch-replies', async () => {
      return await this.fetchRepliesFromGCS();
    });

    ipcMain.handle('fetch-topics', async () => {
      return await this.fetchTopicsFromGCS();
    });

    ipcMain.handle('upload-topics', async (event, topics) => {
      return await this.uploadTopicsToGCS(topics);
    });

    ipcMain.handle('schedule-posts', async (event, { hour, posts }) => {
      this.scheduledHour = hour;
      return await this.scheduleAutoPosts(posts);
    });

    ipcMain.handle('post-reply', async (event, { postUrl, replyText }) => {
      return await this.postReply(postUrl, replyText);
    });

    ipcMain.handle('load-cookies', async () => {
      return await this.loadCookiesForPosting();
    });

    ipcMain.handle('get-app-version', () => {
      return app.getVersion();
    });

    ipcMain.handle('get-circuit-breaker-status', () => {
      return {
        gcs: circuitBreakers.gcs.getState(),
        posting: circuitBreakers.posting.getState()
      };
    });

    ipcMain.handle('reset-circuit-breaker', (event, type) => {
      if (circuitBreakers[type]) {
        circuitBreakers[type].reset();
        return true;
      }
      return false;
    });
  }

  async scheduleAutoPosts(posts) {
    if (this.isProcessing) {
      return { success: false, message: 'Already processing posts' };
    }

    this.isProcessing = true;
    const results = [];

    try {
      for (let i = 0; i < posts.length; i++) {
        const post = posts[i];
        const delay = Math.random() * (45 - 15) + 15; // 15-45 seconds
        
        console.log(`Processing post ${i + 1}/${posts.length} with ${delay}s delay`);
        
        if (i > 0) {
          await new Promise(resolve => setTimeout(resolve, delay * 1000));
        }

        const result = await this.postReply(post.url, post.reply);
        results.push({ postId: post.id, result });

        // Send progress update to renderer
        if (this.mainWindow) {
          this.mainWindow.webContents.send('post-progress', {
            completed: i + 1,
            total: posts.length,
            currentPost: post
          });
        }
      }

      return { success: true, results, processed: results.length };
    } catch (error) {
      console.error('Error during auto-posting:', error);
      return { success: false, message: error.message, results };
    } finally {
      this.isProcessing = false;
    }
  }
}

const scheduler = new XPostScheduler();

app.whenReady().then(async () => {
  await scheduler.initialize();
  scheduler.createWindow();
  scheduler.setupIPC();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      scheduler.createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', () => {
  scheduler.isProcessing = false;
});