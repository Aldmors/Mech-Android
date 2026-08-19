FROM ubuntu:24.04

RUN apt-get update && apt-get install -y --no-install-recommends \
    openjdk-21-jdk-headless \
    curl unzip git \
    && rm -rf /var/lib/apt/lists/*

ENV ANDROID_HOME=/opt/android-sdk
RUN mkdir -p $ANDROID_HOME/cmdline-tools \
    && curl -fsSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o /tmp/cmdline-tools.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools $ANDROID_HOME/cmdline-tools/latest \
    && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
        "platform-tools" "platforms;android-35" "build-tools;35.0.0" "build-tools;34.0.0" \
    && rm -rf /tmp/*

ENV PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
