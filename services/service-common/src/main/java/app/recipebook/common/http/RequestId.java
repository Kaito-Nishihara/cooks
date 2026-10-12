package app.recipebook.common.http;

import java.util.UUID;
import java.util.regex.Pattern;

/** 要求の識別子(「共通設計: サービスの共通構成」の 4.5)。 */
public final class RequestId {

    /** 英数字とハイフンだけの 64 文字以内。ログに任意の文字列を書き込まれるのを防ぐ */
    private static final Pattern ACCEPTABLE = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private RequestId() {
    }

    /** 届いた値が決まった形ならそれを使い、なければ、または形が合わなければ、新しく作る。 */
    public static String resolve(String received) {
        if (received != null && ACCEPTABLE.matcher(received).matches()) {
            return received;
        }
        return UUID.randomUUID().toString();
    }
}
