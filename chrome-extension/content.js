function initializeCapture() {
  if (window.location.pathname === '/home') {
    chrome.runtime.sendMessage({ action: 'startCapture' });
    console.log('X Post Capture: Started monitoring For You page');
    
    let scrollCount = 0;
    const maxScrolls = 10;
    
    const autoScroll = () => {
      if (scrollCount < maxScrolls) {
        window.scrollBy(0, window.innerHeight);
        scrollCount++;
        setTimeout(autoScroll, 2000);
      }
    };
    
    setTimeout(autoScroll, 3000);
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initializeCapture);
} else {
  initializeCapture();
}

window.addEventListener('beforeunload', () => {
  chrome.runtime.sendMessage({ action: 'stopCapture' });
});