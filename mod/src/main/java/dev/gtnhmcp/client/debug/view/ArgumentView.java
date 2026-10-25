package dev.gtnhmcp.client.debug.view;

import com.cleanroommc.modularui.api.widget.IWidget;

import dev.gtnhmcp.client.debug.form.Argument;

/** The editor widget for one kind of {@link Argument}; register new views with {@link ArgumentViews}. */
public interface ArgumentView<A extends Argument> {

    /** The argument class this view edits, including subclasses. */
    Class<A> type();

    IWidget editor(A argument, ArgumentViews views);
}
