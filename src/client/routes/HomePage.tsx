import { useEffect, useState } from "react";
import { fetchHealth } from "../api/health";

/**
 * 仮のホーム画面。
 * 土台が通っていること(画面の描画と Worker API への到達)だけを確認する。
 */
export function HomePage(): React.ReactElement {
  const [status, setStatus] = useState<"loading" | "ok" | "error">("loading");

  useEffect(() => {
    let active = true;
    fetchHealth()
      .then(() => {
        if (active) setStatus("ok");
      })
      .catch(() => {
        if (active) setStatus("error");
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="mx-auto max-w-2xl p-4">
      <h1 className="text-2xl font-bold">mana-cooks</h1>
      <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
        {status === "loading" && "API の応答を確認中"}
        {status === "ok" && "API に到達できた"}
        {status === "error" && "API に到達できない"}
      </p>
    </main>
  );
}
