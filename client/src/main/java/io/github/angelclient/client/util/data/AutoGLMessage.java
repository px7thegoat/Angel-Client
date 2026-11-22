package io.github.angelclient.client.util.data;


public enum AutoGLMessage {
	GLHF("glhf"),
	GL("gl"),
	GOOD_LUCK("good luck!"),
	GOOD_LUCK_HAVE_FUN("good luck, have fun!");

	private final String message;

	AutoGLMessage(String message) { this.message = message; }
	public String getMessage() { return message; }

	@Override
	public String toString() {
		return message;
	}


}