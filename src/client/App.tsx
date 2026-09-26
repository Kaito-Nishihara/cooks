import { Route, Routes } from "react-router";
import { HomePage } from "./routes/HomePage";

/**
 * 画面の振り分け。
 * ここでのパスはブラウザの URL であり、Worker のルートではない。
 * Worker はどのパスに対しても同じ HTML を返す(wrangler.jsonc の single-page-application)。
 */
export function App(): React.ReactElement {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
    </Routes>
  );
}
