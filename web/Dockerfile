FROM node:24-slim AS build
WORKDIR /src
RUN corepack enable
COPY package.json pnpm-lock.yaml ./
RUN COREPACK_ENABLE_DOWNLOAD_PROMPT=0 pnpm install --frozen-lockfile
COPY . .
RUN COREPACK_ENABLE_DOWNLOAD_PROMPT=0 pnpm build-only

FROM caddy:2
COPY --from=build /src/dist /srv
