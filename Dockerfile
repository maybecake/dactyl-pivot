FROM ubuntu:22.04

# Avoid interactive prompts during package installation
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    curl \
    wget \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install Leiningen
RUN curl -s https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/local/bin/lein \
    && chmod +x /usr/local/bin/lein \
    && lein

# Set up working directory
WORKDIR /app

# Create things directory with proper permissions
RUN mkdir -p /app/things && chmod 777 /app/things

# Copy project files
COPY . /app

# Command to run when container starts
CMD ["bash"] 