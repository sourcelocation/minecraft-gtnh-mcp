package dev.gtnhmcp.client.debug.view;

import java.util.List;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.layout.Flow;

import dev.gtnhmcp.client.debug.form.Argument;
import dev.gtnhmcp.client.debug.form.ArgumentForm;

/** Lays out forms as labelled rows, picking each argument's editor from the registered views. */
public final class ArgumentViews {

    static final int ROW = 16;
    private static final int LABEL_WIDTH = 90;
    private static final int GAP = 4;

    private final List<ArgumentView<?>> views;

    public ArgumentViews(List<ArgumentView<?>> views) {
        this.views = List.copyOf(views);
    }

    public static ArgumentViews standard() {
        return new ArgumentViews(List.of(new TextFieldView(), new FlagView(), new RecordListView()));
    }

    /** The caller sizes the form's width. */
    public Flow form(ArgumentForm form) {
        Flow column = Flow.column()
            .coverChildrenHeight()
            .childPadding(GAP);
        for (Argument argument : form.arguments()) column.child(row(argument));
        return column;
    }

    /**
     * A label and its editor. The editor sits in a box sized "row minus label", a width MUI2 can compute up front;
     * editors fill whatever box they are given.
     */
    private IWidget row(Argument argument) {
        String label = argument.required() ? argument.name() + " *" : argument.name();
        IWidget name = new TextWidget<>(IKey.str(label)).width(LABEL_WIDTH)
            .height(ROW)
            .tooltip(tooltip -> tooltip.addLine(argument.description()));
        IWidget editorBox = new ParentWidget<>().widthRelOffset(1f, -LABEL_WIDTH)
            .coverChildrenHeight()
            .child(editor(argument));
        return Flow.row()
            .widthRel(1f)
            .coverChildrenHeight()
            .child(name)
            .child(editorBox);
    }

    private IWidget editor(Argument argument) {
        for (ArgumentView<?> view : views) if (view.type()
            .isInstance(argument)) return edit(view, argument);
        throw new IllegalStateException("No view for " + argument.getClass());
    }

    /** Safe: the view's type was just checked against the argument. */
    @SuppressWarnings("unchecked")
    private <A extends Argument> IWidget edit(ArgumentView<A> view, Argument argument) {
        return view.editor((A) argument, this);
    }
}
