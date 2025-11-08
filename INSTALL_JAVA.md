# Installing Java 21 on macOS

You need Java 21 to run the backend. Here are several options:

## Option 1: Install Homebrew (Recommended, then install Java)

Homebrew is the easiest package manager for macOS:

1. **Install Homebrew:**
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
   
   Follow the prompts. After installation, you may need to add Homebrew to your PATH:
   ```bash
   echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zshrc
   eval "$(/opt/homebrew/bin/brew shellenv)"
   ```

2. **Install Java 21:**
   ```bash
   brew install openjdk@21
   ```

3. **Link Java:**
   ```bash
   sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
   ```

4. **Set JAVA_HOME:**
   ```bash
   echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
   echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
   source ~/.zshrc
   ```

5. **Verify:**
   ```bash
   java -version
   # Should show: openjdk version "21.x.x"
   ```

## Option 2: SDKMAN (Java Version Manager)

SDKMAN is great for managing multiple Java versions:

1. **Install SDKMAN:**
   ```bash
   curl -s "https://get.sdkman.io" | bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```

2. **Install Java 21:**
   ```bash
   sdk install java 21.0.1-tem
   ```

3. **Verify:**
   ```bash
   java -version
   ```

## Option 3: Direct Download (Manual Installation)

1. **Download Java 21:**
   - Visit: https://adoptium.net/temurin/releases/?version=21
   - Download: **macOS** → **x64** (or **ARM64** for Apple Silicon) → **JDK** → **.pkg** installer

2. **Install:**
   - Double-click the downloaded `.pkg` file
   - Follow the installation wizard

3. **Set JAVA_HOME:**
   ```bash
   echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
   echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
   source ~/.zshrc
   ```

4. **Verify:**
   ```bash
   java -version
   ```

## Option 4: Using IntelliJ IDEA / IDE

If you're using IntelliJ IDEA or another IDE:
- Most IDEs can download and manage JDKs automatically
- Check your IDE's settings for JDK management
- Download JDK 21 through the IDE

## After Installing Java

1. **Install Maven:**
   ```bash
   # If using Homebrew:
   brew install maven
   
   # Or download from: https://maven.apache.org/download.cgi
   ```

2. **Verify Maven:**
   ```bash
   mvn -version
   ```

3. **Start Docker:**
   ```bash
   # Make sure Docker Desktop is running
   docker compose up -d
   ```

4. **Build and Run Backend:**
   ```bash
   cd backend
   mvn clean install
   mvn spring-boot:run
   ```

## Troubleshooting

### Java not found after installation
```bash
# Check if Java is installed
/usr/libexec/java_home -V

# If Java 21 is listed, set JAVA_HOME:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH

# Add to ~/.zshrc to make permanent:
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v 21)' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
```

### Maven not found
```bash
# Install via Homebrew:
brew install maven

# Or download and extract, then add to PATH:
export PATH=/path/to/maven/bin:$PATH
```

### Check your architecture
```bash
# Check if you're on Apple Silicon (ARM) or Intel:
uname -m
# arm64 = Apple Silicon, x86_64 = Intel
```

Choose the appropriate Java download for your architecture.

