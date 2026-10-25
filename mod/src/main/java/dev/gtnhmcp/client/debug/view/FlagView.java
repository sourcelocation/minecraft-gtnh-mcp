package dev.gtnhmcp.client.debug.view;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;

import dev.gtnhmcp.client.debug.form.FlagArgument;

/** A button cycling unset, true and false. */
final class FlagView implements ArgumentView<FlagArgument> {

    private static final int WIDTH = 50;

    public Class<FlagArgument> type() {
        return FlagArgument.class;
    }

    public IWidget editor(FlagArgument argument, ArgumentViews views) {
        return new ButtonWidget<>().size(WIDTH, ArgumentViews.ROW)
            .overlay(IKey.dynamic(argument::label))
            .onMousePressed(mouseButton -> {
                argument.cycle();
                return true;
            });
    }
}
