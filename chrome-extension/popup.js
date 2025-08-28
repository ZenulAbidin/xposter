class PopupController {
  constructor() {
    this.elements = {
      status: document.getElementById('status'),
      totalPosts: document.getElementById('totalPosts'),
      postsWithMedia: document.getElementById('postsWithMedia'),
      captureStatus: document.getElementById('captureStatus'),
      exportPosts: document.getElementById('exportPosts'),
      exportCookies: document.getElementById('exportCookies')
    };
    
    this.init();
  }
  
  async init() {
    this.bindEvents();
    await this.updateStats();
    setInterval(() => this.updateStats(), 2000);
  }
  
  bindEvents() {
    this.elements.exportPosts.addEventListener('click', () => {
      this.exportPosts();
    });
    
    this.elements.exportCookies.addEventListener('click', () => {
      this.exportCookies();
    });
  }
  
  async updateStats() {
    try {
      const response = await chrome.runtime.sendMessage({ action: 'getStats' });
      if (response) {
        this.displayStats(response);
      }
    } catch (error) {
      console.error('Failed to get stats:', error);
    }
  }
  
  displayStats(stats) {
    this.elements.totalPosts.textContent = stats.totalPosts;
    this.elements.postsWithMedia.textContent = stats.postsWithMedia;
    
    if (stats.isCapturing) {
      this.elements.captureStatus.textContent = 'Capturing';
      this.elements.status.textContent = 'Capturing posts...';
      this.elements.status.className = 'status capturing';
    } else {
      this.elements.captureStatus.textContent = 'Idle';
      this.elements.status.textContent = 'Ready to capture';
      this.elements.status.className = 'status idle';
    }
    
    this.elements.exportPosts.disabled = stats.totalPosts === 0;
  }
  
  async exportPosts() {
    try {
      this.elements.exportPosts.disabled = true;
      this.elements.exportPosts.textContent = 'Exporting...';
      
      await chrome.runtime.sendMessage({ action: 'exportPosts' });
      
      this.showTemporaryMessage('Posts exported successfully!');
    } catch (error) {
      console.error('Export failed:', error);
      this.showTemporaryMessage('Export failed. Please try again.', true);
    } finally {
      setTimeout(() => {
        this.elements.exportPosts.disabled = false;
        this.elements.exportPosts.textContent = 'Export Posts JSON';
      }, 2000);
    }
  }
  
  async exportCookies() {
    try {
      this.elements.exportCookies.disabled = true;
      this.elements.exportCookies.textContent = 'Exporting...';
      
      await chrome.runtime.sendMessage({ action: 'exportCookies' });
      
      this.showTemporaryMessage('Cookies exported successfully!');
    } catch (error) {
      console.error('Cookie export failed:', error);
      this.showTemporaryMessage('Cookie export failed. Please try again.', true);
    } finally {
      setTimeout(() => {
        this.elements.exportCookies.disabled = false;
        this.elements.exportCookies.textContent = 'Export Cookies JSON';
      }, 2000);
    }
  }
  
  showTemporaryMessage(message, isError = false) {
    const originalContent = this.elements.status.textContent;
    const originalClass = this.elements.status.className;
    
    this.elements.status.textContent = message;
    this.elements.status.className = `status ${isError ? 'error' : 'success'}`;
    
    setTimeout(() => {
      this.elements.status.textContent = originalContent;
      this.elements.status.className = originalClass;
    }, 3000);
  }
}

document.addEventListener('DOMContentLoaded', () => {
  new PopupController();
});