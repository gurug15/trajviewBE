FROM ubuntu:24.04

# Install system dependencies + JDK + GROMACS in one layer
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    software-properties-common \
    gromacs \
    && add-apt-repository ppa:openjdk-r/ppa \
    && apt-get update \
    && apt-get install -y --no-install-recommends openjdk-21-jdk \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

EXPOSE 8080

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} trajview.jar

RUN mkdir /analysis

ENTRYPOINT ["java", "-jar", "/trajview.jar"]
