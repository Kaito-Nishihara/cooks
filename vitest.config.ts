import { fileURLToPath } from "node:url";
import { defineConfig } from "vitest/config";

/**
 * テストの設定は vite.config.ts から分ける。
 * @cloudflare/vite-plugin は開発サーバーを立てるため、Vitest と同居できない。
 * ドメインロジックと純粋な関数は依存が無いので、Node 環境でそのまま試せる。
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.ts", "src/**/*.test.tsx"],
  },
});
