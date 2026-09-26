# 開発用。ホスト側に Node の依存を入れないため、すべてコンテナ内で実行する。
FROM node:24-slim AS dev

WORKDIR /app

# workerd(wrangler が使う Workers ランタイム)の実行に必要。
RUN apt-get update \
  && apt-get install -y --no-install-recommends ca-certificates \
  && rm -rf /var/lib/apt/lists/*

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

EXPOSE 5173
CMD ["npm", "run", "dev"]
