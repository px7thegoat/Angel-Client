package io.github.angelclient.client.ui.component.impl;

import io.github.angelclient.client.mod.Mod;
import io.github.angelclient.client.ui.component.Component;
import io.github.angelclient.client.ui.component.controller.Controller;
import io.github.angelclient.client.ui.screen.mods.ModsScreen.ModsScreenComponent;
import io.github.angelclient.client.util.data.Colour;

public class ColouredComponent extends Component {

	private final Controller<Colour> colour;

	public Colour getColour() {
		return colour.get(this, Colour.WHITE);
	}

	public int getColourValue() {
		return getColour().getValue();
	}

	public ColouredComponent(io.github.angelclient.client.ui.component.controller.Controller<io.github.angelclient.client.util.data.Colour> colour) {
		this.colour = colour;
	}

    public void ModListing(Mod mod, ModsScreenComponent screen) {
        //TODO Auto-generated constructor stub
    }

	public boolean useFallback() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'useFallback'");
	}

}