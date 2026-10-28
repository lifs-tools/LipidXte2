# Node.js runtime: taken from the official image so a security release is a one-line tag bump.
# (Debian bookworm's own `nodejs` package is Node 18, which is end-of-life and no longer patched.)
FROM --platform=$TARGETPLATFORM node:26.12.0-bookworm-slim AS node

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
    mono-complete \
    xvfb


# Node.js 24 LTS (node, npm, corepack) from the official image above
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY --from=node /usr/local/lib/node_modules /usr/local/lib/node_modules
RUN ln -s ../lib/node_modules/npm/bin/npm-cli.js /usr/local/bin/npm && \
    ln -s ../lib/node_modules/npm/bin/npx-cli.js /usr/local/bin/npx && \
    ln -s ../lib/node_modules/corepack/dist/corepack.js /usr/local/bin/corepack && \
    node --version && npm --version

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
COPY LipidXplorer/src /app/LipidXplorer/src
COPY docker-build /app
COPY docker-build/LipidXteSqlite.db /root/
COPY docker-build/pref /root/.massSpec

WORKDIR /app
# Batch uploads and the status database live here (bind-mounted by docker-compose in production).
RUN mkdir -p /app/download
# Yarn version is pinned by "packageManager" in package.json; corepack fetches exactly that version.
# .yarnrc.yml sets enableImmutableInstalls, so the build fails if yarn.lock does not match package.json.
ENV COREPACK_ENABLE_DOWNLOAD_PROMPT=0
RUN corepack enable
# Fail the build if yarn.lock does not match package.json ('focus' alone would silently resolve the difference).
RUN yarn install --immutable --mode=skip-build
# Then keep only runtime dependencies in the image (jest, eslint, supertest are removed again).
RUN yarn workspaces focus --production
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
