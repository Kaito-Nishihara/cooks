package app.recipebook.auth.account.presentation;

import app.recipebook.auth.shared.domain.UserId;
import java.util.Arrays;
import java.util.List;

/** 「,」で区切った識別子の並び。検証の前と後で同じ区切り方にするために、ここ 1 か所に置く。 */
final class UserIdListText {

    /** 1 回に並べてよい件数 */
    static final int MAX_COUNT = 100;

    private UserIdListText() {
    }

    /** 空の要素も 1 件に数える。「a,,b」を 3 件とし、空の要素は形の誤りにする。 */
    static List<String> split(String text) {
        return Arrays.asList(text.split(",", -1));
    }

    /** 検証(UserIdList)に通った入力を、識別子に変える。 */
    static List<UserId> parse(String text) {
        return split(text).stream().map(UserId::of).toList();
    }
}
