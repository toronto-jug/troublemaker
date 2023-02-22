FROM debian:latest

RUN apt-get update -y && apt install -y \
    curl \
    iproute2 \
    less \
    openjdk-17-jdk \
    strace \
    sudo \
    tcpdump
RUN curl -Ls https://sh.jbang.dev | bash -s - app setup

ENTRYPOINT /bin/bash
