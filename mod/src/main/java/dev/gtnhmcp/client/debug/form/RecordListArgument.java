package dev.gtnhmcp.client.debug.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** A list of nested records, e.g. plan_chain's targets: one sub-form per row. */
public final class RecordListArgument extends Argument {

    private final Supplier<ArgumentForm> rowFactory;
    private final List<ArgumentForm> rows = new ArrayList<>();

    public RecordListArgument(String name, String description, boolean required, Supplier<ArgumentForm> rowFactory) {
        super(name, description, required);
        this.rowFactory = rowFactory;
        add();
    }

    public List<ArgumentForm> rows() {
        return Collections.unmodifiableList(rows);
    }

    public ArgumentForm add() {
        ArgumentForm row = rowFactory.get();
        rows.add(row);
        return row;
    }

    public void remove(ArgumentForm row) {
        rows.remove(row);
    }

    /** Rows left completely empty are skipped. */
    public Object value() {
        List<Map<String, Object>> values = rows.stream()
            .filter(row -> !row.isEmpty())
            .map(ArgumentForm::values)
            .toList();
        return values.isEmpty() ? null : values;
    }
}
