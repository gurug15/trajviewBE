FROM ubuntu:24.04
COPY trajectory_service.py .

#Conda Gromacs installation
RUN apt-get update && apt-get install -y wget \
    && wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh \
    && bash Miniconda3-latest-Linux-x86_64.sh -b -p /opt/conda \
    && rm Miniconda3-latest-Linux-x86_64.sh \
    && /opt/conda/bin/conda init bash

#JDK installation in ubuntu
RUN apt-get update && apt-get install -y \
    wget \
    software-properties-common

# Add the OpenJDK PPA (if needed)
RUN add-apt-repository ppa:openjdk-r/ppa

# Update package list again and install OpenJDK 21
RUN apt-get install -y \
    openjdk-21-jdk

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

EXPOSE 8080
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} trajview.jar
RUN mkdir /analysis
ENTRYPOINT ["java","-jar","/trajview.jar"]

#Python
RUN apt-get install python3 python3-pip -y
RUN pip3 install flask mdtraj numpy --break-system-packages

# Install GROMACS via Conda
RUN /opt/conda/bin/conda install -c bioconda gromacs \
    && /opt/conda/bin/conda install -c conda-forge gromacs=2024

#CMD ["python3", "trajectory_service.py"]
