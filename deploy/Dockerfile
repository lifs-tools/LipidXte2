FROM --platform=$TARGETPLATFORM python:3.12.3-slim as builder

ARG TARGETARCH

# Declaring env
ENV NODE_ENV production

# Select builder stage based on TARGETARCH ARG
FROM builder

ARG TARGETARCH

RUN apt update && \
    apt install -y \
    sudo \
    ssh \
    git \
    gpg \
    wget curl

RUN apt-key adv     --keyserver hkp://keyserver.ubuntu.com:80  \
       --recv-keys 0xB1998361219BD9C9 && \
    curl -O https://cdn.azul.com/zulu/bin/zulu-repo_1.0.0-2_all.deb && \
    apt install ./zulu-repo_1.0.0-2_all.deb

RUN apt update && \
    apt install -y \
    build-essential  \
    python-dev-is-python3  \
    nodejs \
    npm  \
    mono-complete \
    xvfb


RUN wget https://cdn.azul.com/zulu/bin/zulu8.78.0.19-ca-fx-jdk8.0.412-linux_${TARGETARCH}.deb && \
    apt-get -yq install -f ./zulu8.78.0.19-ca-fx-jdk8.0.412-linux_${TARGETARCH}.deb && \
    rm -rf ./zulu8.78.0.19-ca-fx-jdk8.0.412-linux_${TARGETARCH}.deb && \
    apt-get remove -y gnupg && \
    apt-get clean autoclean && \
    apt-get autoremove --yes && \
    rm -rf /var/lib/apt/lists/*


#RUN apt install -y dirmngr ca-certificates gnupg && \
#    gpg --homedir /tmp --no-default-keyring --keyring /usr/share/keyrings/mono-official-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF && \
#    echo "deb [signed-by=/usr/share/keyrings/mono-official-archive-keyring.gpg] https://download.mono-project.com/repo/debian stable-buster main" | sudo tee /etc/apt/sources.list.d/mono-official-stable.list && \
#    sudo apt update && \
#    apt install -y mono-complete && \
#    rm -rf /var/lib/apt/lists/*


RUN python3 -m pip install --no-cache-dir \
    numpy \
    ply \
    configparser \
    numba \
    lxml \
    pandas \
    fisher-py

VOLUME /tmp
COPY preprocessing/src /app/preprocessing/src
COPY server /app
COPY server/LipidXteSqlite.db /root/
COPY server/pref /root/.massSpec

WORKDIR /app
RUN npm install -g corepack
RUN corepack enable
RUN yarn set version stable
RUN yarn
RUN yarn cache clean --all

# Development mode
CMD ["yarn", "dev"]

EXPOSE 8090

# Production mode
#CMD ["yarn", "start"]
#
## Exposing server port
#EXPOSE 80
#EXPOSE 443

# Run this docker with root for using 443 port
# docker build -t lipidserver:latest .
# docker run -d -v /local/moon/LipidXteServer/certs:/app/certs -v /local/moon/LipidXteServer/download:/app/download -v /local/moon/LipidXteServer/web:/app/web -p 0.0.0.0:80:80 -p 0.0.0.0:443:443 lipidserver:latest
