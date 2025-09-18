# Java Versions Summary & Setup Guide

## 🔍 **Current Java Versions on Your System:**

### **System Default Java:**
```
java version "25" 2025-09-16 LTS
Java(TM) SE Runtime Environment (build 25+37-LTS-3491)
Java HotSpot(TM) 64-Bit Server VM (build 25+37-LTS-3491, mixed mode, sharing)
```
- **Location:** `C:\Program Files\Java\jdk-25\`
- **Status:** ❌ Too new for Android Gradle Plugin
- **Compatibility:** Not supported by AGP 8.7.2

### **Android Studio's Java:**
```
openjdk version "21.0.7" 2025-04-15
OpenJDK Runtime Environment (build 21.0.7+-13880790-b1038.58)
OpenJDK 64-Bit Server VM (build 21.0.7+-13880790-b1038.58, mixed mode)
```
- **Location:** `C:\Program Files\Android\Android Studio\jbr\`
- **Status:** ✅ Compatible with Android Gradle Plugin
- **Compatibility:** Supported by AGP 8.7.2

## 🚀 **Setup Commands for Different Projects:**

### **Android Development (Current Working Setup):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
java -version
# Should show: openjdk version "21.0.7"
```

### **Backend Development (Recommended):**
```powershell
# Install Java 17 first, then:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
java -version
# Should show: java version "17.x.x"
```

## 📋 **Project Configuration Files:**

### **Android Project (build.gradle.kts):**
```kotlin
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlinOptions {
    jvmTarget = "21"
}
```

### **Backend Project (pom.xml):**
```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
</properties>
```

## 🔧 **Quick Setup Scripts:**

### **Android Development Script:**
```powershell
# Set JAVA_HOME for Android
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
echo "JAVA_HOME set to: $env:JAVA_HOME"
java -version
```

### **Backend Development Script:**
```powershell
# Set JAVA_HOME for Backend (when Java 17 is installed)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
echo "JAVA_HOME set to: $env:JAVA_HOME"
java -version
```

## ⚠️ **Important Notes:**

1. **Java 25** is too new for Android development
2. **Java 21** (Android Studio) works for Android projects
3. **Java 17** is recommended for backend/Spring Boot projects
4. Always set `JAVA_HOME` explicitly to avoid version conflicts
5. Clear Gradle cache if you encounter build issues: `Remove-Item -Path "$env:USERPROFILE\.gradle" -Recurse -Force`

## 🎯 **Next Steps for Backend Project:**

1. Install Java 17 LTS
2. Set `JAVA_HOME` to Java 17
3. Configure Maven/Gradle to use Java 17
4. Test build with `mvn clean install` or `./gradlew build`

---
**Last Updated:** $(Get-Date)
**Working Android Build:** ✅ SUCCESS
**Java Version Used:** OpenJDK 21.0.7 (Android Studio)
