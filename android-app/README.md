# X Post Scheduler - Android App

A native Android application for managing and scheduling X (Twitter) post replies with AI-powered content generation.

## 🚀 Features

- **Native Android Experience**: Material Design 3 interface with smooth animations
- **Background Sync**: WorkManager integration for reliable data synchronization
- **GCS Integration**: Seamless sync with Google Cloud Storage for cross-platform data
- **Topic Management**: Dynamic topic selection with cloud synchronization
- **Smart Posting**: WebView-based posting with cookie authentication
- **Circuit Breaker Protection**: Robust error handling and recovery mechanisms

## 📱 Requirements

- **Android**: API 24+ (Android 7.0)
- **Target SDK**: API 34 (Android 14)
- **Storage**: ~80MB RAM typical usage
- **Network**: Internet connection required for GCS sync and posting

## 🛠️ Development Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Java 8+
- Android SDK 34
- Google Cloud Platform account with service account key

### Building the App

1. **Clone the repository**:
   ```bash
   git clone <your-private-repo-url>
   cd android-app
   ```

2. **Configure credentials**:
   - Place your GCS service account key as `app/src/main/assets/key.json`
   - Update `GCSService.java` with your bucket name and project ID

3. **Build the app**:
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**:
   ```bash
   ./gradlew installDebug
   ```

## 📁 Project Structure

```
app/src/main/java/com/zenulabidin/xposter/scheduler/
├── MainActivity.java              # Main app interface
├── XPosterApplication.java        # Application class
├── adapters/
│   └── PostReplyAdapter.java      # RecyclerView adapter for posts
├── models/
│   ├── PostReply.java            # Post data model
│   └── RepliesResponse.java       # GCS response model
└── services/
    └── GCSService.java           # Google Cloud Storage integration
```

## 🔧 Configuration

### GCS Integration
Update the following constants in `GCSService.java`:
```java
private static final String BUCKET_NAME = "your-actual-bucket-name";
private static final String PROJECT_ID = "your-actual-project-id";
```

### Authentication
Place your service account key file in:
```
app/src/main/assets/key.json
```

## 🚨 Security Notes

- **Never commit service account keys** to version control
- The `key.json` file is excluded in `.gitignore`
- Use environment-specific configurations for different builds
- Implement proper key rotation procedures

## 🎯 Usage

1. **Initial Setup**: Configure topics on first launch
2. **Sync Data**: App automatically polls GCS every 15 minutes in background
3. **Review Posts**: View AI-generated replies with scores and categories
4. **Post Management**: 
   - Tap "Post Now" for immediate posting
   - Tap "Discard" to remove unwanted posts
   - Use "Schedule All" for batch processing

## 🔍 Monitoring

The app includes built-in monitoring for:
- **GCS Sync Status**: Connection health and last sync time
- **Processing Locks**: Coordination with other app instances
- **Error Recovery**: Circuit breaker status and recovery attempts

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist
- [ ] Topic selection and sync
- [ ] Post loading and filtering
- [ ] Reply generation display
- [ ] Posting workflow with WebView
- [ ] Background sync functionality
- [ ] Error handling and recovery

## 📊 Performance

### Typical Resource Usage
- **RAM**: ~80MB during normal operation
- **CPU**: Minimal impact with efficient background sync
- **Network**: ~1-5MB per sync cycle depending on data volume
- **Battery**: Optimized WorkManager scheduling for minimal drain

### Background Sync
- **Frequency**: Every 15 minutes when conditions are met
- **Constraints**: Requires network connection
- **Coordination**: Uses distributed locks to prevent conflicts with desktop app

## 🔄 CI/CD Integration

For automated builds and deployment:

### GitHub Actions (Recommended)
```yaml
name: Android CI
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    - name: Build with Gradle
      run: ./gradlew build
    - name: Run tests
      run: ./gradlew test
```

## 🚀 Deployment

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
1. Configure signing in `app/build.gradle`
2. Create keystore for release signing
3. Build release APK:
   ```bash
   ./gradlew assembleRelease
   ```

## 📋 Changelog

### Version 1.0.0
- Initial release with core functionality
- Material Design 3 interface
- GCS integration with coordination layer
- Circuit breaker pattern for reliability
- Background sync with WorkManager

## 🤝 Contributing

1. Follow Android development best practices
2. Maintain Material Design 3 consistency
3. Add unit tests for new features
4. Update documentation for changes
5. Test on multiple Android versions

## 📄 License

This project is licensed under the MIT License - see the main project LICENSE file for details.

## 🆘 Troubleshooting

### Common Issues

**App crashes on startup**
- Verify service account key is properly placed
- Check GCS bucket permissions
- Ensure network connectivity

**Background sync not working**
- Check battery optimization settings
- Verify WorkManager constraints are met
- Review logs for sync errors

**Posting fails**
- Ensure X cookies are valid and current
- Check WebView permissions
- Verify network connectivity to x.com

### Support
For issues and bug reports, please use the GitHub Issues tab in this repository.