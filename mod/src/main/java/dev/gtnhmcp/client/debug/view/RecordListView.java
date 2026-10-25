package dev.gtnhmcp.client.debug.view;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import dev.gtnhmcp.client.debug.form.ArgumentForm;
import dev.gtnhmcp.client.debug.form.RecordListArgument;

/** One sub-form per row, each with a remove button, and an add button below. */
final class RecordListView implements ArgumentView<RecordListArgument> {

    private static final int GAP = 2;

    public Class<RecordListArgument> type() {
        return RecordListArgument.class;
    }

    public IWidget editor(RecordListArgument argument, ArgumentViews views) {
        return new Rows(argument, views).column;
    }

    /** Keeps the row widgets in step with the argument's rows. */
    private static final class Rows {

        private final RecordListArgument argument;
        private final ArgumentViews views;
        private final Flow column = Flow.column()
            .widthRel(1f)
            .coverChildrenHeight()
            .childPadding(GAP);

        Rows(RecordListArgument argument, ArgumentViews views) {
            this.argument = argument;
            this.views = views;
            for (ArgumentForm row : argument.rows()) column.child(row(row));
            column.child(button("+", this::add));
        }

        /** New rows go above the add button. */
        private void add() {
            column.addChild(
                row(argument.add()),
                column.getChildren()
                    .size() - 1);
            column.scheduleResize();
        }

        private IWidget row(ArgumentForm row) {
            Flow widget = Flow.row()
                .widthRel(1f)
                .coverChildrenHeight()
                .childPadding(GAP);
            return widget.child(
                views.form(row)
                    .widthRelOffset(1f, -(ArgumentViews.ROW + GAP)))
                .child(button("-", () -> remove(row, widget)));
        }

        private void remove(ArgumentForm row, IWidget widget) {
            argument.remove(row);
            column.remove(widget);
            column.scheduleResize();
        }

        private static ButtonWidget<?> button(String label, Runnable action) {
            return new ButtonWidget<>().size(ArgumentViews.ROW)
                .overlay(IKey.str(label))
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
        }
    }
}
