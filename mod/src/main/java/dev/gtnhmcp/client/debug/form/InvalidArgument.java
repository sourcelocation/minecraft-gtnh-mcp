package dev.gtnhmcp.client.debug.form;

/** Input the form cannot turn into a value; the message is shown to the player. */
public final class InvalidArgument extends RuntimeException {

    public InvalidArgument(String message) {
        super(message);
    }
}
