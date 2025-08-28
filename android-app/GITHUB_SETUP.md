# GitHub Repository Setup Guide

## Step-by-Step Instructions

### 1. Create Private GitHub Repository

1. **Go to GitHub.com** and sign in to your account
2. **Click the "+" icon** in the top right corner
3. **Select "New repository"**
4. **Configure the repository**:
   - **Repository name**: `xposter-android`
   - **Description**: `Native Android app for X post scheduling and management`
   - **Visibility**: ✅ **Private**
   - **Initialize**: ❌ Don't add README, .gitignore, or license (we have them)
5. **Click "Create repository"**

### 2. Initialize Git Repository Locally

Navigate to the android-app directory and initialize Git:

```bash
cd /home/zenulabidin/Documents/xposter/android-app

# Initialize Git repository
git init

# Add all files to staging
git add .

# Create initial commit
git commit -m "Initial commit: X Post Scheduler Android App

- Native Android app with Material Design 3
- GCS integration for data synchronization  
- Circuit breaker pattern for reliability
- WorkManager background sync
- Coordinated polling with desktop app
- WebView-based posting with cookie auth"
```

### 3. Connect to GitHub Repository

Replace `YOUR_GITHUB_USERNAME` with your actual GitHub username:

```bash
# Add remote repository
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/xposter-android.git

# Set main branch
git branch -M main

# Push to GitHub
git push -u origin main
```

### 4. Configure Repository Settings

#### A. Branch Protection (Recommended)
1. Go to your repository on GitHub
2. **Settings** → **Branches**
3. **Add rule** for `main` branch:
   - ✅ Require pull request reviews before merging
   - ✅ Require status checks to pass before merging
   - ✅ Require branches to be up to date before merging

#### B. Security Settings
1. **Settings** → **Security & analysis**
2. Enable:
   - ✅ Dependency graph
   - ✅ Dependabot alerts
   - ✅ Dependabot security updates

#### C. Secrets for CI/CD (If using GitHub Actions)
1. **Settings** → **Secrets and variables** → **Actions**
2. Add repository secrets:
   - `ANDROID_SIGNING_KEY`: Base64 encoded keystore
   - `ANDROID_KEY_ALIAS`: Keystore alias
   - `ANDROID_KEY_PASSWORD`: Key password
   - `ANDROID_STORE_PASSWORD`: Keystore password

### 5. Set Up GitHub Actions (Optional)

Create `.github/workflows/android-ci.yml`:

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
    - name: Checkout code
      uses: actions/checkout@v4
      
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
        
    - name: Cache Gradle packages
      uses: actions/cache@v3
      with:
        path: |
          ~/.gradle/caches
          ~/.gradle/wrapper
        key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
        restore-keys: |
          ${{ runner.os }}-gradle-
          
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
      
    - name: Run tests
      run: ./gradlew test
      
    - name: Build debug APK
      run: ./gradlew assembleDebug
      
    - name: Upload APK artifact
      uses: actions/upload-artifact@v3
      with:
        name: debug-apk
        path: app/build/outputs/apk/debug/*.apk
```

### 6. Repository Structure Verification

Your repository should now have this structure:
```
xposter-android/
├── .github/
│   └── workflows/
│       └── android-ci.yml
├── app/
│   ├── src/main/java/com/zenulabidin/xposter/scheduler/
│   ├── src/main/res/
│   └── build.gradle
├── gradle/
├── .gitignore
├── README.md
├── GITHUB_SETUP.md
├── build.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
└── settings.gradle
```

### 7. Collaborate and Maintain

#### Development Workflow
1. **Create feature branches** for new development:
   ```bash
   git checkout -b feature/topic-management-ui
   ```

2. **Make changes and commit**:
   ```bash
   git add .
   git commit -m "Add topic management UI with Material Design 3"
   ```

3. **Push and create pull request**:
   ```bash
   git push -u origin feature/topic-management-ui
   ```

4. **Create PR** on GitHub for code review

#### Release Management
1. **Tag releases** for version tracking:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. **Create GitHub Releases** for distribution

### 8. Team Access (If Needed)

1. **Settings** → **Manage access**
2. **Invite a collaborator**
3. Set appropriate permissions:
   - **Read**: View code and issues
   - **Triage**: Manage issues and pull requests  
   - **Write**: Push commits and create branches
   - **Maintain**: Manage repository settings
   - **Admin**: Full administrative access

### 9. Issue Templates (Optional)

Create `.github/ISSUE_TEMPLATE/` with templates:

**Bug Report** (`.github/ISSUE_TEMPLATE/bug_report.md`):
```markdown
---
name: Bug report
about: Create a report to help us improve
title: ''
labels: bug
assignees: ''
---

**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior.

**Expected behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Device info:**
- Device: [e.g. Samsung Galaxy S21]
- OS: [e.g. Android 12]
- App Version: [e.g. 1.0.0]
```

### 10. Verification Commands

After setup, verify everything works:

```bash
# Clone the repository to test
git clone https://github.com/YOUR_GITHUB_USERNAME/xposter-android.git test-clone
cd test-clone

# Verify build works
./gradlew build

# Clean up test
cd ..
rm -rf test-clone
```

## Repository URL

After completing the setup, your private repository will be available at:
```
https://github.com/YOUR_GITHUB_USERNAME/xposter-android
```

## Next Steps

1. **Invite collaborators** if working with a team
2. **Set up development environment** on all development machines
3. **Configure CI/CD pipeline** for automated testing and builds
4. **Create first release** when ready for testing
5. **Set up issue tracking** for bug reports and feature requests

Your Android app is now properly set up with a private GitHub repository with enterprise-grade development practices!