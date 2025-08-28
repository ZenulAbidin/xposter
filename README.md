# X Post Automation System

A comprehensive system for automated engagement with X (Twitter) posts using AI-powered content analysis and reply generation.

## ⚠️ Important Disclaimer

This system is designed for **personal use only** and must comply with X's Terms of Service. It requires human confirmation before posting replies and is intended to assist with content engagement, not replace human judgment.

## 🎯 Features

- **Smart Post Capture**: Chrome extension captures posts from your For You page
- **AI-Powered Filtering**: Uses Gemini AI to score posts based on your interests
- **Intelligent Reply Generation**: Creates contextual, engaging replies (50-150 characters)
- **Multi-Platform Support**: Desktop (Electron) and mobile (Android) management apps
- **Human Oversight**: All posts require manual confirmation before publishing
- **Topic Management**: Customizable interest categories with sync across devices
- **Automated Workflows**: n8n handles background processing and cleanup

## 🏗️ System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Chrome Ext.    │───▶│  n8n Workflows  │───▶│  GCS Storage    │
│  (Capture)      │    │  (Process AI)   │    │  (Sync Data)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────┐    ┌─────────────────┐
                       │  Electron App   │◀───┤  Android App    │
                       │  (Desktop)      │    │  (Mobile)       │
                       └─────────────────┘    └─────────────────┘
```

## 📦 Components

### 1. Chrome Extension
- Captures GraphQL requests from x.com/home
- Extracts post data, media, and engagement metrics
- Filters posts by age (<2 days) and liked status
- Exports JSON files for processing

### 2. n8n Workflows
- **Main Processing**: Analyzes posts using Gemini AI, scores relevance, generates replies
- **Cleanup**: Removes stale posts older than 2 days
- Handles image processing and multimodal AI analysis

### 3. Electron Desktop App
- Modern desktop interface for post management
- Real-time polling of Google Cloud Storage
- Topic selection and sync capabilities
- Batch scheduling with randomized delays

### 4. Android Mobile App
- Native mobile interface with Material Design
- Background sync using WorkManager
- WebView integration for posting with cookie management
- Offline support with local data caching

## 🚀 Quick Start

### Prerequisites
- Google Cloud Platform account with billing enabled
- Gemini API access from ai.google.dev
- n8n instance (local or cloud)
- Node.js 18+ for Electron app
- Android Studio for mobile app

### Basic Setup
1. **Configure Google Cloud**
   ```bash
   # Create GCS bucket
   gsutil mb gs://your-bucket-name
   
   # Create service account with Storage permissions
   gcloud iam service-accounts create x-poster-service
   ```

2. **Update Configuration**
   - Edit `config/config.json` with your API keys and bucket details
   - Place service account key as `key.json` in component directories

3. **Install Chrome Extension**
   - Load unpacked extension from `chrome-extension/` folder
   - Navigate to x.com/home and start capturing posts

4. **Setup n8n Workflows**
   - Import workflows from `n8n-workflows/` directory
   - Configure Google Cloud and Gemini API credentials

5. **Run Desktop App**
   ```bash
   cd electron-app
   npm install
   npm start
   ```

See [SETUP.md](SETUP.md) for detailed installation instructions.

## 📱 Usage

### Daily Workflow
1. **Capture**: Use Chrome extension to capture posts while browsing X
2. **Process**: n8n automatically analyzes posts and generates replies every 30 minutes  
3. **Review**: Open desktop or mobile app to review AI-generated replies
4. **Schedule**: Set posting times and confirm replies before publishing

### Topic Management
- Select from 15+ predefined topics (AI, Crypto, Tech, etc.)
- Add custom topics based on your interests
- Topics sync across all devices via Google Cloud Storage

### Smart Filtering
- Posts scored 0-100 based on topic relevance
- Only posts scoring ≥50 are processed for replies
- Automatic filtering of liked posts and content >2 days old

## 🔧 Configuration

### Key Settings in `config/config.json`
```json
{
  "posting": {
    "minDelaySeconds": 15,
    "maxDelaySeconds": 45,
    "maxPostsPerDay": 400,
    "scoreThreshold": 50
  },
  "filtering": {
    "maxPostAgeHours": 48,
    "minReplyLength": 50,
    "maxReplyLength": 150
  }
}
```

### Environment Variables
```bash
export GCP_PROJECT_ID="your-project-id"
export GCS_BUCKET_NAME="your-bucket-name"  
export GEMINI_API_KEY="your-api-key"
```

## 🔐 Security & Compliance

### X Platform Compliance
- ✅ Human confirmation required for all posts
- ✅ Respects rate limits with randomized delays
- ✅ No fully automated posting without user consent
- ✅ Transparent about AI-generated content

### Data Security  
- 🔒 Service account keys never committed to version control
- 🔒 Temporary storage in GCS with automatic cleanup
- 🔒 Local processing of sensitive authentication data
- 🔒 Regular rotation of API keys and credentials

## 📊 Monitoring & Analytics

### Built-in Metrics
- Posts captured per session
- AI scoring accuracy and distribution  
- Reply engagement rates
- Topic performance analysis

### Cost Monitoring
- GCS storage usage and costs
- Gemini API token consumption
- n8n execution statistics

## 🛠️ Development

### Project Structure
```
xposter/
├── chrome-extension/     # Chrome extension for post capture
├── n8n-workflows/       # AI processing workflows  
├── electron-app/        # Desktop management app
├── android-app/         # Mobile management app
├── config/             # Shared configuration
└── docs/               # Documentation
```

### Contributing
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📈 Performance

### Typical Processing Times
- **Post Capture**: ~2-3 seconds per 20 posts
- **AI Analysis**: ~5-10 seconds per post with images
- **Reply Generation**: ~3-5 seconds per reply
- **End-to-End**: ~15 minutes from capture to ready replies

### Resource Usage
- **Chrome Extension**: Minimal CPU impact
- **n8n Workflows**: ~100MB RAM per execution
- **Electron App**: ~150MB RAM typical usage
- **Android App**: ~80MB RAM with background sync

## 🆘 Troubleshooting

### Common Issues

**"No posts captured"**
- Verify extension has debugger permissions
- Check if x.com page structure changed
- Ensure you're on the correct /home timeline

**"AI processing failed"**
- Verify Gemini API key is valid and has quota
- Check n8n workflow credentials configuration
- Monitor GCP billing and quota limits

**"Authentication errors"**
- Regenerate service account key
- Verify GCS bucket permissions
- Check project ID and bucket name configuration

See [SETUP.md](SETUP.md) for detailed troubleshooting steps.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚖️ Legal & Ethical Use

This tool is designed for personal productivity and engagement enhancement. Users are responsible for:

- Complying with X's Terms of Service and automation policies
- Ensuring all posted content meets platform guidelines  
- Maintaining authentic engagement and avoiding spam behavior
- Respecting intellectual property and attribution requirements

**Use responsibly and maintain the human element in your social media interactions.**

---

**Built with ❤️ for the creator economy and thoughtful social media engagement.**