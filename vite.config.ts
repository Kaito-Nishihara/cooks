import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { cloudflare } from "@cloudflare/vite-plugin";

export default defineConfig({
  plugins: [react(), tailwindcss(), cloudflare()],
  resolve: {
    // tsconfig の paths と同じ別名を解決する。
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  // public/ の中身は静的アセットとして配信される。
  // マスタデータ(成分表等)は public/data/ に置き、ブラウザが直接取得する。
  build: {
    // バンドルが膨らむと Worker の起動時間が CPU 10ms に食い込むため、
    // 想定外の肥大化に気づけるよう警告のしきい値を低めにする。
    chunkSizeWarningLimit: 500,
  },
});
