package app.recipebook.common.openapi;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/** 画面に出す仕様(/q/openapi)に、/api の入口だけを残す。 */
@OpenApiFilter(documentNames = OpenApiFilter.DEFAULT_DOCUMENT_NAME, stages = OpenApiFilter.RunStage.BUILD)
public class PublicApiFilter implements OASFilter {

    private final PathPrefix paths = new PathPrefix(ApiDocuments.PUBLIC_PREFIX);

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        paths.keepOnly(openAPI);
    }
}
