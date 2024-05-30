# Declare builder using amd64 base image
FROM --platform=linux/amd64 python:3.12.3-slim as builder-amd64
# Declare builder using arm64 base image
FROM --platform=linux/arm64 arm64v8/python:3.12.3-slim as builder-arm64

ARG TARGETARCH

# Declaring env
ENV NODE_ENV production

# Select builder stage based on TARGETARCH ARG
FROM builder-${TARGETARCH}

RUN apt update && \
    apt install -y  \
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
    apt install -y  \
    build-essential  \
    python-dev-is-python3  \
    nodejs \
    npm  \
    mono-complete \
    xvfb


RUN wget https://cdn.azul.com/zulu/bin/zulu8.78.0.19-ca-fx-jdk8.0.412-linux_${TARGETARCH}.deb && \
    apt-get -yq install -f ./zulu8.78.0.19-ca-fx-jdk8.0.412-linux_${TARGETARCH}.deb && \
    rm -rf /var/lib/apt/lists/*


#RUN apt install -y dirmngr ca-certificates gnupg && \
#    gpg --homedir /tmp --no-default-keyring --keyring /usr/share/keyrings/mono-official-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF && \
#    echo "deb [signed-by=/usr/share/keyrings/mono-official-archive-keyring.gpg] https://download.mono-project.com/repo/debian stable-buster main" | sudo tee /etc/apt/sources.list.d/mono-official-stable.list && \
#    sudo apt update && \
#    apt install -y mono-complete && \
#    rm -rf /var/lib/apt/lists/*


RUN python3 -m pip install \
    numpy \
    ply \
    configparser \
    numba \
    lxml \
    pandas \
    fisher-py

VOLUME /tmp
COPY LipidXplorer/src /app/LipidXplorer/src
COPY docker-build /app
COPY docker-build/LipidXteSqlite.db /root/
COPY docker-build/pref /root/.massSpec

WORKDIR /app
RUN npm install -g corepack
RUN corepack enable
RUN yarn set version stable
RUN yarn

#CMD ["yarn", "dev"]

# Exposing server port
#EXPOSE 8090
