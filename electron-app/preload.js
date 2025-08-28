const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  fetchReplies: () => ipcRenderer.invoke('fetch-replies'),
  fetchTopics: () => ipcRenderer.invoke('fetch-topics'),
  uploadTopics: (topics) => ipcRenderer.invoke('upload-topics', topics),
  schedulePosts: (options) => ipcRenderer.invoke('schedule-posts', options),
  postReply: (options) => ipcRenderer.invoke('post-reply', options),
  loadCookies: () => ipcRenderer.invoke('load-cookies'),
  getAppVersion: () => ipcRenderer.invoke('get-app-version'),
  
  // Event listeners
  onPostProgress: (callback) => ipcRenderer.on('post-progress', callback),
  removeAllListeners: () => ipcRenderer.removeAllListeners()
});