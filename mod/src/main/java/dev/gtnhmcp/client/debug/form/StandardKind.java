package dev.gtnhmcp.client.debug.form;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/** The argument types the current tools use. */
public enum StandardKind implements ArgumentKind {

    TEXT {

        public boolean accepts(Type type) {
            return type == String.class;
        }

        public Argument create(Field field, ArgumentForms forms) {
            return new TextArgument(field.name(), field.description(), field.required());
        }
    },

    NUMBER {

        public boolean accepts(Type type) {
            return NUMBERS.contains(type);
        }

        public Argument create(Field field, ArgumentForms forms) {
            return new NumberArgument(field.name(), field.description(), field.required());
        }
    },

    FLAG {

        public boolean accepts(Type type) {
            return type == boolean.class || type == Boolean.class;
        }

        public Argument create(Field field, ArgumentForms forms) {
            return new FlagArgument(field.name(), field.description(), field.required());
        }
    },

    TEXT_LIST {

        public boolean accepts(Type type) {
            return listElement(type) == String.class;
        }

        public Argument create(Field field, ArgumentForms forms) {
            return new TextListArgument(field.name(), field.description(), field.required());
        }
    },

    NUMBER_LIST {

        public boolean accepts(Type type) {
            return NUMBERS.contains(listElement(type));
        }

        public Argument create(Field field, ArgumentForms forms) {
            return new NumberListArgument(field.name(), field.description(), field.required());
        }
    },

    RECORD_LIST {

        public boolean accepts(Type type) {
            return listElement(type) instanceof Class<?>element && element.isRecord();
        }

        public Argument create(Field field, ArgumentForms forms) {
            Type element = listElement(field.type());
            return new RecordListArgument(field.name(), field.description(), field.required(), () -> forms.of(element));
        }
    };

    private static final Set<Type> NUMBERS = Set.of(int.class, long.class, Integer.class, Long.class);

    /** The element type of a {@code List<E>}, or null for anything else. */
    private static Type listElement(Type type) {
        return type instanceof ParameterizedType list && list.getRawType() == List.class
            ? list.getActualTypeArguments()[0]
            : null;
    }
}
