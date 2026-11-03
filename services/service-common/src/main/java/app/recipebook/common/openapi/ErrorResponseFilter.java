package app.recipebook.common.openapi;

import app.recipebook.common.error.ErrorResponse;
import io.quarkus.smallrye.openapi.OpenApiFilter;
import java.util.List;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.media.Schema;

/**
 * どの仕様にも、エラーの形 ErrorResponse を載せ、すべての操作のエラーの応答をそれに向ける(4.8、4.11)。
 * エラーは入口で組み立てず、共通の部品が返すので、入口ごとに注釈で書かない。
 */
@OpenApiFilter(stages = OpenApiFilter.RunStage.BUILD)
public class ErrorResponseFilter implements OASFilter {

    static final String SCHEMA_NAME = ErrorResponse.class.getSimpleName();

    private static final String REF = "#/components/schemas/" + SCHEMA_NAME;

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        Components components = openAPI.getComponents();
        components.addSchema(SCHEMA_NAME, errorSchema());

        if (openAPI.getPaths() == null || openAPI.getPaths().getPathItems() == null) {
            return;
        }
        openAPI.getPaths().getPathItems().values().forEach(item ->
                item.getOperations().values().forEach(ErrorResponseFilter::addDefaultError));
    }

    private static void addDefaultError(Operation operation) {
        if (operation.getResponses() == null) {
            operation.setResponses(OASFactory.createAPIResponses());
        }
        operation.getResponses().addAPIResponse("default", OASFactory.createAPIResponse()
                .description("エラー。形は 3 つのサービスで同じである")
                .content(OASFactory.createContent().addMediaType("application/json",
                        OASFactory.createMediaType().schema(OASFactory.createSchema().ref(REF)))));
    }

    /** ErrorResponse の record と同じ項目にする */
    private static Schema errorSchema() {
        return OASFactory.createSchema()
                .type(List.of(Schema.SchemaType.OBJECT))
                .description("エラーの応答の本文")
                .addProperty("code", OASFactory.createSchema().type(List.of(Schema.SchemaType.STRING))
                        .description("エラーの種類"))
                .addProperty("message", OASFactory.createSchema().type(List.of(Schema.SchemaType.STRING))
                        .description("利用者に見せてよい文"))
                .addProperty("details", OASFactory.createSchema()
                        .type(List.of(Schema.SchemaType.OBJECT, Schema.SchemaType.NULL))
                        .additionalPropertiesSchema(OASFactory.createSchema())
                        .description("種類ごとの追加の情報。ないときは null"))
                .required(List.of("code", "message"));
    }
}
