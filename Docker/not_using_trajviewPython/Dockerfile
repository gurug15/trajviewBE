FROM ubuntu:24.04
COPY trajectory_service.py .
#Depends on this packages
#sudo apt install python3-pip
#sudo pip3 install flask mdtraj numpy --break-system-packages
RUN apt-get update -y
RUN apt-get install python3 python3-pip -y
RUN apt install python3-flask -y
RUN apt install python3-mdtraj -y
RUN apt install python3-numpy -y


CMD ["python3", "trajectory_service.py"]
