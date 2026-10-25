package dev.gtnhmcp.client.debug.form;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Builds a form from a tool's input record, the same record its JSON Schema is generated from. */
public final class ArgumentForms {

    private final List<ArgumentKind> kinds;

    public ArgumentForms(List<ArgumentKind> kinds) {
        this.kinds = List.copyOf(kinds);
    }

    public static ArgumentForms standard() {
        return new ArgumentForms(List.of(StandardKind.values()));
    }

    public ArgumentForm of(Type input) {
        List<Argument> arguments = new ArrayList<>();
        if (input instanceof Class<?>record && record.isRecord())
            for (RecordComponent component : record.getRecordComponents()) arguments.add(argument(field(component)));
        return new ArgumentForm(arguments);
    }

    /** Types no kind accepts are edited as text, so a new tool never breaks the screen. */
    private Argument argument(ArgumentKind.Field field) {
        for (ArgumentKind kind : kinds) if (kind.accepts(field.type())) return kind.create(field, this);
        return StandardKind.TEXT.create(field, this);
    }

    /**
     * Jackson's annotations do not target record components, so Java copies them onto the accessor, not the component.
     */
    private static ArgumentKind.Field field(RecordComponent component) {
        JsonProperty property = component.getAccessor()
            .getAnnotation(JsonProperty.class);
        JsonPropertyDescription description = component.getAccessor()
            .getAnnotation(JsonPropertyDescription.class);
        return new ArgumentKind.Field(
            component.getName(),
            description == null ? "" : description.value(),
            property != null && property.required(),
            component.getGenericType());
    }
}
