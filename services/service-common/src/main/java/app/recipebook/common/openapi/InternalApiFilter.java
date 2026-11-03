package app.recipebook.common.openapi;

import io.quarkus.smallrye.openapi.OpenApiFilter;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/** サービスの間の仕様(/q/openapi-internal)に、/internal の入口だけを残す。 */
@OpenApiFilter(documentNames = ApiDocuments.INTERNAL, stages = OpenApiFilter.RunStage.BUILD)
public class InternalApiFilter implements OASFilter {

    private final PathPrefix paths = new PathPrefix(ApiDocuments.INTERNAL_PREFIX);

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        paths.keepOnly(openAPI);
    }
}
