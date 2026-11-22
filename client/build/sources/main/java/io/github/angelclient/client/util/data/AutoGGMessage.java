package io.github.angelclient.client.util.data;


public enum AutoGGMessage {
	GG("gg"),
	GF("gf"),
	GOOD_GAME("good game"),
	GOOD_FIGHT("good fight");

	private final String message;

	AutoGGMessage(String message) { this.message = message; }
	public String getMessage() { return message; }

	@Override
	public String toString() {
		return message;
	}


}