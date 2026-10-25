package app.recipebook.common.openapi;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;

/**
 * 決まった始まりのパスだけを仕様に残す。
 * 注釈で入口に 1 つずつ印を付ける方法にしないのは、付け忘れた入口が、違う仕様に混ざらないようにするためである。
 * Quarkus は、OASFilter を直接実装したクラスだけをフィルターとして読むので、継承でなく、この部品を呼んで使う。
 */
final class PathPrefix {

    private final String prefix;

    PathPrefix(String prefix) {
        this.prefix = prefix;
    }

    void keepOnly(OpenAPI openAPI) {
        Paths paths = openAPI.getPaths();
        if (paths == null || paths.getPathItems() == null) {
            return;
        }
        paths.getPathItems().keySet().stream()
                .filter(path -> !path.startsWith(prefix))
                .toList()
                .forEach(paths::removePathItem);
    }
}
