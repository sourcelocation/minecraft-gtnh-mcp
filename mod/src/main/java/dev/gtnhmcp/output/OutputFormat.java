package dev.gtnhmcp.output;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.OptionPreset;
import com.github.victools.jsonschema.generator.SchemaGenerationContext;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import com.github.victools.jsonschema.generator.SchemaKeyword;
import com.github.victools.jsonschema.generator.SchemaVersion;
import com.github.victools.jsonschema.module.jackson.JacksonOption;
import com.github.victools.jsonschema.module.jackson.JacksonSchemaModule;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * How tool output reads. Tools return plain records; this alone decides their JSON and schemas: {@link Label}led types
 * become one string, and nulls and empty values are omitted to save tokens. The SDK keeps its own, standard mapper.
 */
public final class OutputFormat {

    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final List<Label<?>> labels;
    private final JsonMapper mapper;
    private final SchemaGenerator schemas;

    public OutputFormat() {
        ItemLabel item = new ItemLabel();
        StackLabel stack = new StackLabel(item);
        this.labels = List.of(item, stack, new IngredientLabel(stack));
        SimpleModule module = new SimpleModule("labels");
        for (Label<?> label : labels) module.addSerializer(label);
        this.mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(
                inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_EMPTY)
                    .withContentInclusion(JsonInclude.Include.NON_EMPTY))
            .addModule(module)
            .build();
        SchemaGeneratorConfigBuilder config = new SchemaGeneratorConfigBuilder(
            mapper,
            SchemaVersion.DRAFT_2020_12,
            OptionPreset.PLAIN_JSON).without(Option.SCHEMA_VERSION_INDICATOR)
                .with(Option.MAP_VALUES_AS_ADDITIONAL_PROPERTIES)
                .with(new JacksonSchemaModule(JacksonOption.RESPECT_JSONPROPERTY_REQUIRED));
        config.forTypesInGeneral()
            .withCustomDefinitionProvider(this::labelSchema);
        this.schemas = new SchemaGenerator(config.build());
    }

    /** Reads tool input and writes tool output. */
    public JsonMapper mapper() {
        return mapper;
    }

    public Map<String, Object> schema(Type type) {
        return mapper.convertValue(schemas.generateSchema(type), MAP);
    }

    private CustomDefinition labelSchema(ResolvedType type, SchemaGenerationContext context) {
        return labels.stream()
            .filter(
                label -> label.handledType()
                    .equals(type.getErasedType()))
            .findFirst()
            .map(
                label -> new CustomDefinition(
                    context.getGeneratorConfig()
                        .createObjectNode()
                        .put(
                            context.getKeyword(SchemaKeyword.TAG_TYPE),
                            context.getKeyword(SchemaKeyword.TAG_TYPE_STRING))
                        .put(context.getKeyword(SchemaKeyword.TAG_DESCRIPTION), label.describe()),
                    CustomDefinition.INLINE_DEFINITION,
                    CustomDefinition.INCLUDING_ATTRIBUTES))
            .orElse(null);
    }
}
