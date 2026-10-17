package app.recipebook.auth.account.presentation;

import app.recipebook.auth.account.application.UserSummary;
import java.util.List;

/**
 * 表示名の一覧の形(「機能設計: 登録とログイン」の 4.8)。
 * 一覧の形(「共通設計: API の共通仕様」の 4.3)に合わせて nextCursor を置くが、続きはないので、いつも null である。
 */
public record UserListResponse(List<Item> items, String nextCursor) {

    public static UserListResponse of(List<UserSummary> users) {
        return new UserListResponse(users.stream().map(Item::from).toList(), null);
    }

    /** 利用者 1 人。項目は、この 2 つだけである。 */
    public record Item(String id, String displayName) {

        static Item from(UserSummary user) {
            return new Item(user.id(), user.displayName());
        }
    }
}
