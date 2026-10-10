/**
 * wrangler types が生成する Env に、変数と秘密値の型を足す。
 * worker-configuration.d.ts は生成物のため直接編集しない。
 */
declare global {
  interface Env {
    /** Cloudflare Access のチーム ドメイン。"example.cloudflareaccess.com" の形。 */
    ACCESS_TEAM_DOMAIN?: string;
    /** Access アプリケーションの Application Audience (AUD) タグ。 */
    ACCESS_AUD?: string;
    /**
     * ローカル開発でログインする利用者のメールアドレス。
     * これが設定されている間は Access の検証を行わない。本番では設定しない。
     */
    DEV_USER_EMAIL?: string;
  }
}

export {};
