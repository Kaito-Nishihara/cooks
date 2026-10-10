/**
 * Cloudflare Access が発行した識別トークンの検証。
 *
 * Access は Worker のコードに到達する前にリクエストを弾くため、
 * ここへ来る時点で認証は済んでいる。それでもトークンを検証するのは、
 * Access の適用範囲の設定漏れや将来の経路追加で素通りした場合に気づくためである。
 *
 * 検証にかかる CPU を抑えるため、公開鍵は隔離環境(isolate)のメモリへ保持して使い回す。
 */

/** Access がリクエストへ付ける識別トークンのヘッダ名。 */
const ACCESS_JWT_HEADER = "Cf-Access-Jwt-Assertion";

/** 公開鍵を取り直す間隔。鍵の入れ替えに追随できる程度に短くする。 */
const JWKS_TTL_MS = 60 * 60 * 1000;

interface CachedKeys {
  keys: Map<string, CryptoKey>;
  fetchedAt: number;
}

let cache: CachedKeys | undefined;

/** Access の識別情報。 */
export interface AccessIdentity {
  email: string;
}

/** トークンが無い、または検証に通らない。 */
export class AccessVerificationError extends Error {}

interface JwkSet {
  keys: (JsonWebKey & { kid?: string })[];
}

interface AccessJwtPayload {
  email?: string;
  aud?: string | string[];
  exp?: number;
  iss?: string;
}

function decodeSegment(segment: string): unknown {
  // base64url を base64 へ直してから復号する。
  const base64 = segment.replaceAll("-", "+").replaceAll("_", "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  const binary = atob(padded);
  const bytes = Uint8Array.from(binary, (ch) => ch.charCodeAt(0));
  return JSON.parse(new TextDecoder().decode(bytes));
}

function toBytes(segment: string): Uint8Array<ArrayBuffer> {
  const base64 = segment.replaceAll("-", "+").replaceAll("_", "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  const binary = atob(padded);
  const bytes = new Uint8Array(new ArrayBuffer(binary.length));
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

async function loadKeys(teamDomain: string, forceRefresh = false): Promise<Map<string, CryptoKey>> {
  const now = Date.now();
  if (!forceRefresh && cache && now - cache.fetchedAt < JWKS_TTL_MS) {
    return cache.keys;
  }

  const res = await fetch(`https://${teamDomain}/cdn-cgi/access/certs`);
  if (!res.ok) {
    throw new AccessVerificationError(`Access の公開鍵を取得できない: ${res.status}`);
  }

  const jwks = (await res.json()) as JwkSet;
  const keys = new Map<string, CryptoKey>();
  for (const jwk of jwks.keys) {
    if (!jwk.kid) continue;
    const key = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"],
    );
    keys.set(jwk.kid, key);
  }

  cache = { keys, fetchedAt: now };
  return keys;
}

/**
 * リクエストから Access の識別情報を取り出す。
 * トークンが無い場合と検証に通らない場合は AccessVerificationError を投げる。
 */
export async function verifyAccessIdentity(
  request: Request,
  teamDomain: string,
  audience: string,
): Promise<AccessIdentity> {
  const token = request.headers.get(ACCESS_JWT_HEADER);
  if (!token) {
    throw new AccessVerificationError("Access の識別トークンが無い");
  }

  const parts = token.split(".");
  const [headerSegment, payloadSegment, signatureSegment] = parts;
  if (parts.length !== 3 || !headerSegment || !payloadSegment || !signatureSegment) {
    throw new AccessVerificationError("識別トークンの形式が不正");
  }

  const header = decodeSegment(headerSegment) as { kid?: string; alg?: string };
  if (header.alg !== "RS256" || !header.kid) {
    throw new AccessVerificationError("識別トークンの署名方式が想定と異なる");
  }

  let key = (await loadKeys(teamDomain)).get(header.kid);
  if (!key) {
    // Access が鍵を入れ替えた直後は、手元の鍵に新しい kid が無い。
    // キャッシュの期限切れを待つと最長で TTL のあいだログインできなくなるため、取り直す。
    key = (await loadKeys(teamDomain, true)).get(header.kid);
  }
  if (!key) {
    throw new AccessVerificationError("識別トークンに対応する公開鍵が無い");
  }

  const signed = new TextEncoder().encode(`${headerSegment}.${payloadSegment}`);
  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    key,
    toBytes(signatureSegment),
    signed,
  );
  if (!valid) {
    throw new AccessVerificationError("識別トークンの署名が一致しない");
  }

  const payload = decodeSegment(payloadSegment) as AccessJwtPayload;

  if (typeof payload.exp !== "number" || payload.exp * 1000 <= Date.now()) {
    throw new AccessVerificationError("識別トークンの有効期限が切れている");
  }

  // 別のアプリケーション向けに発行されたトークンを受け入れないよう、対象を確認する。
  const audiences = Array.isArray(payload.aud) ? payload.aud : payload.aud ? [payload.aud] : [];
  if (!audiences.includes(audience)) {
    throw new AccessVerificationError("識別トークンの対象が一致しない");
  }

  if (!payload.email) {
    throw new AccessVerificationError("識別トークンにメールアドレスが無い");
  }

  return { email: payload.email.toLowerCase() };
}
