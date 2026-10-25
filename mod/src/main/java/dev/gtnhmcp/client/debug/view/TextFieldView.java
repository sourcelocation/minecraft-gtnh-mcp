package dev.gtnhmcp.client.debug.view;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import dev.gtnhmcp.client.debug.form.TextualArgument;

/** Text, numbers and comma-separated lists: a text field bound to the argument's text. */
final class TextFieldView implements ArgumentView<TextualArgument> {

    private static final int MAX_LENGTH = 1000;

    public Class<TextualArgument> type() {
        return TextualArgument.class;
    }

    public IWidget editor(TextualArgument argument, ArgumentViews views) {
        return new TextFieldWidget().value(new StringValue.Dynamic(argument::text, argument::text))
            .setMaxLength(MAX_LENGTH)
            // Write every keystroke to the argument, so Run sees text that is still being typed.
            .autoUpdateOnChange(true)
            .hintText(argument.hint())
            .widthRel(1f)
            .height(ArgumentViews.ROW);
    }
}
