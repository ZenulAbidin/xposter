# X Post Automation System - Setup Guide

## Overview
This system automates engaging with posts on X (Twitter) by capturing posts from your For You page, filtering them based on your interests using AI, generating thoughtful replies, and managing the posting process with human oversight.

## Components
1. **Chrome Extension** - Captures posts from x.com/home
2. **n8n Workflows** - Processes posts using Gemini AI  
3. **Electron Desktop App** - Manages replies and scheduling
4. **Android Mobile App** - Mobile interface for post management

## Prerequisites

### Required Accounts & Services
- **Google Cloud Platform account** with billing enabled
- **Gemini API access** from ai.google.dev
- **n8n instance** (self-hosted or cloud)
- **Chrome browser** for extension
- **Node.js 18+** for Electron app
- **Android Studio** for Android app development

### API Keys & Credentials Needed
1. **Gemini API Key**: Get from https://ai.google.dev/
2. **Google Cloud Service Account Key**: For GCS access
3. **GCS Bucket**: Create a new bucket for data storage

## Step-by-Step Setup

### 1. Google Cloud Setup

#### Create GCS Bucket
```bash
# Using gcloud CLI
gsutil mb gs://your-bucket-name
```

#### Create Service Account
1. Go to Google Cloud Console → IAM & Admin → Service Accounts
2. Create new service account with name "x-poster-service"
3. Grant roles: Storage Object Admin, Storage Object Creator, Storage Object Viewer
4. Generate and download JSON key file as `key.json`

#### Configure Bucket CORS (if needed)
```json
[
  {
    "origin": ["*"],
    "method": ["GET", "PUT", "POST", "DELETE"],
    "responseHeader": ["Content-Type"],
    "maxAgeSeconds": 3600
  }
]
```

### 2. Update Configuration Files

#### Edit `config/config.json`
```json
{
  "gcs": {
    "bucketName": "your-actual-bucket-name",
    "projectId": "your-actual-project-id",
    "keyFilePath": "./key.json"
  },
  "gemini": {
    "apiKey": "your_actual_gemini_api_key"
  }
}
```

### 3. Chrome Extension Setup

1. Copy the service account `key.json` to `chrome-extension/` folder
2. Open Chrome → Extensions → Developer mode ON
3. Click "Load unpacked" and select `chrome-extension/` folder
4. Navigate to x.com/home and scroll to capture posts
5. Use extension popup to export `for_you_posts.json` and `cookies_x.json`

### 4. n8n Workflows Setup

#### Install n8n
```bash
# Local installation
npm install -g n8n

# Or using Docker
docker run -it --rm --name n8n -p 5678:5678 n8nio/n8n
```

#### Import Workflows
1. Open n8n at http://localhost:5678
2. Import `n8n-workflows/main-processing-workflow.json`
3. Import `n8n-workflows/cleanup-workflow.json`

#### Configure Credentials
1. **Google Cloud Storage**: Upload service account JSON
2. **HTTP Request Nodes**: Set Gemini API key in headers
3. Update bucket names in all GCS nodes

#### Activate Workflows
- Enable "Main Processing" workflow (runs every 30 minutes)
- Enable "Cleanup" workflow (runs daily at midnight)

### 5. Electron Desktop App Setup

#### Install Dependencies
```bash
cd electron-app
npm install
```

#### Add Configuration Files
1. Copy `key.json` to `electron-app/key.json`
2. Update `main.js` with your actual project ID and bucket name

#### Run the App
```bash
npm start
```

### 6. Android App Setup

#### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 34
- Java 8+

#### Configuration
1. Copy `key.json` to `android-app/app/src/main/assets/key.json`
2. Update `GCSService.java` with your bucket name and project ID
3. Update `build.gradle` with your signing configuration

#### Build & Install
```bash
cd android-app
./gradlew assembleDebug
# Install APK on device or emulator
```

## Usage Workflow

### Daily Operation
1. **Morning**: Open Chrome extension, visit x.com/home, scroll to capture posts
2. **Export**: Use extension to download posts and cookies JSON files
3. **Upload**: Place files in n8n monitored folder or manually trigger workflow
4. **Processing**: n8n automatically processes posts every 30 minutes
5. **Review**: Open desktop/mobile app to review generated replies
6. **Schedule**: Set posting hour and schedule replies with human confirmation

### Topic Management
1. Open desktop or mobile app
2. Navigate to topic settings
3. Select from predefined topics or add custom ones
4. Topics are synced to GCS for all devices

### Posting Process
1. App polls GCS for new replies
2. Filter posts by score (≥50) and user preferences  
3. Review each reply before posting
4. Schedule posts with 15-45 second random delays
5. Human confirmation required before actual posting

## Monitoring & Maintenance

### Daily Tasks
- Check n8n workflow execution status
- Monitor GCS bucket usage and costs
- Review posting statistics in apps

### Weekly Tasks
- Clean up old processed posts
- Update topic preferences if needed
- Review and adjust AI prompts in n8n workflows

### Monthly Tasks
- Analyze engagement metrics
- Update posting schedule based on performance
- Review and optimize Gemini API usage

## Troubleshooting

### Common Issues

#### "Authentication Failed" in n8n
- Verify service account key is valid
- Check GCS bucket permissions
- Ensure billing is enabled on GCP

#### "No posts captured" in Chrome extension  
- Ensure debugger permissions are granted
- Check if x.com page structure has changed
- Verify extension is active on correct tab

#### "Failed to fetch replies" in desktop/mobile app
- Check internet connection
- Verify GCS bucket accessibility  
- Check service account permissions

#### "Low quality replies" from AI
- Adjust prompts in n8n workflows
- Increase score threshold in filtering
- Update topic selection to be more specific

### Log Locations
- **Chrome Extension**: Browser DevTools → Console
- **n8n**: Workflow execution logs in UI
- **Electron**: App DevTools → Console  
- **Android**: Android Studio → Logcat

## Security Considerations

### API Keys & Credentials
- Never commit service account keys to version control
- Store keys securely and rotate regularly
- Use environment variables where possible

### X Platform Compliance
- Always require human confirmation before posting
- Respect rate limits and posting frequency
- Follow X's Terms of Service and automation policies
- Monitor for any policy changes

### Data Privacy
- Posts and replies are temporarily stored in GCS
- Personal data is processed only for reply generation
- Regular cleanup removes old data automatically

## Cost Optimization

### GCS Storage
- Use lifecycle policies to auto-delete old files
- Monitor bucket usage in GCP console
- Consider using Nearline/Coldline for archival

### Gemini API
- Optimize prompts for token efficiency
- Use appropriate model for task complexity
- Monitor usage in AI Studio dashboard

### n8n Hosting
- Self-host to avoid cloud n8n costs
- Use resource-efficient server sizing
- Monitor workflow execution frequency

## Support & Updates

### Getting Help
- Check troubleshooting section first
- Review component logs for error details
- Test individual components in isolation

### Updates & Maintenance
- Regularly update Chrome extension manifest
- Monitor X platform changes that may affect scraping
- Keep Gemini API model versions current
- Update dependencies in all components

This system is designed for personal use only and should comply with X's Terms of Service. Always ensure human oversight in the posting process.