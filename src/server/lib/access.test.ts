import { describe, expect, it, vi, beforeAll, beforeEach, afterEach } from "vitest";
import { AccessVerificationError, verifyAccessIdentity } from "./access";

const TEAM_DOMAIN = "example.cloudflareaccess.com";
const AUD = "aud-tag";
const KID = "test-key";

/** 検証に使う鍵の組。テストの間だけ使い捨てる。 */
async function generateKeyPair(): Promise<CryptoKeyPair> {
  return crypto.subtle.generateKey(
    {
      name: "RSASSA-PKCS1-v1_5",
      modulusLength: 2048,
      publicExponent: new Uint8Array([1, 0, 1]),
      hash: "SHA-256",
    },
    true,
    ["sign", "verify"],
  );
}

function base64Url(bytes: Uint8Array): string {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replaceAll("+", "-").replaceAll("/", "_").replaceAll("=", "");
}

function encodeJson(value: unknown): string {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)));
}

/** Access が発行するものと同じ形の識別トークンを組み立てる。 */
async function makeToken(
  privateKey: CryptoKey,
  payload: Record<string, unknown>,
  header: Record<string, unknown> = { alg: "RS256", kid: KID },
): Promise<string> {
  const signingInput = `${encodeJson(header)}.${encodeJson(payload)}`;
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    new TextEncoder().encode(signingInput),
  );
  return `${signingInput}.${base64Url(new Uint8Array(signature))}`;
}

function requestWith(token?: string): Request {
  const headers = new Headers();
  if (token) headers.set("Cf-Access-Jwt-Assertion", token);
  return new Request("https://example.com/api/me", { headers });
}

const validPayload = () => ({
  email: "Owner@Example.com",
  aud: [AUD],
  exp: Math.floor(Date.now() / 1000) + 600,
});

describe("verifyAccessIdentity", () => {
  let keyPair: CryptoKeyPair;
  let publicJwk: JsonWebKey;

  // 鍵はテスト全体で一組にする。verifyAccessIdentity が公開鍵をメモリへ保持するため、
  // テストごとに鍵を替えると実際には起きない不一致を見ることになる。
  beforeAll(async () => {
    keyPair = await generateKeyPair();
    publicJwk = await crypto.subtle.exportKey("jwk", keyPair.publicKey);
  });

  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => Response.json({ keys: [{ ...publicJwk, kid: KID }] })),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("正しいトークンからメールアドレスを小文字で取り出す", async () => {
    const token = await makeToken(keyPair.privateKey, validPayload());
    const identity = await verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD);
    expect(identity.email).toBe("owner@example.com");
  });

  it("トークンが無ければ失敗する", async () => {
    await expect(verifyAccessIdentity(requestWith(), TEAM_DOMAIN, AUD)).rejects.toThrow(
      AccessVerificationError,
    );
  });

  it("有効期限が切れたトークンを受け入れない", async () => {
    const token = await makeToken(keyPair.privateKey, {
      ...validPayload(),
      exp: Math.floor(Date.now() / 1000) - 1,
    });
    await expect(verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD)).rejects.toThrow(
      /有効期限/,
    );
  });

  it("別のアプリケーション向けのトークンを受け入れない", async () => {
    const token = await makeToken(keyPair.privateKey, { ...validPayload(), aud: ["other-aud"] });
    await expect(verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD)).rejects.toThrow(
      /対象が一致しない/,
    );
  });

  it("別の鍵で署名されたトークンを受け入れない", async () => {
    const attacker = await generateKeyPair();
    const token = await makeToken(attacker.privateKey, validPayload());
    await expect(verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD)).rejects.toThrow(
      /署名が一致しない/,
    );
  });

  it("署名方式が RS256 でないトークンを受け入れない", async () => {
    // alg を none に差し替えて署名の検証を回避しようとする改竄を想定する。
    const token = await makeToken(keyPair.privateKey, validPayload(), { alg: "none", kid: KID });
    await expect(verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD)).rejects.toThrow(
      /署名方式/,
    );
  });

  it("鍵が入れ替わったら取り直して検証する", async () => {
    // 先に現行の鍵で一度検証し、公開鍵をメモリへ載せる。
    await verifyAccessIdentity(
      requestWith(await makeToken(keyPair.privateKey, validPayload())),
      TEAM_DOMAIN,
      AUD,
    );

    // Access が鍵を入れ替えた状況を作る。手元のキャッシュには新しい kid が無い。
    const rotated = await generateKeyPair();
    const rotatedJwk = await crypto.subtle.exportKey("jwk", rotated.publicKey);
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => Response.json({ keys: [{ ...rotatedJwk, kid: "rotated-key" }] })),
    );

    const token = await makeToken(rotated.privateKey, validPayload(), {
      alg: "RS256",
      kid: "rotated-key",
    });
    const identity = await verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD);
    expect(identity.email).toBe("owner@example.com");
  });

  it("メールアドレスを含まないトークンを受け入れない", async () => {
    const payload = validPayload();
    const token = await makeToken(keyPair.privateKey, {
      aud: payload.aud,
      exp: payload.exp,
    });
    await expect(verifyAccessIdentity(requestWith(token), TEAM_DOMAIN, AUD)).rejects.toThrow(
      /メールアドレス/,
    );
  });
});
