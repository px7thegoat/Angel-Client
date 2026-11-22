package io.github.angelclient.client.ui.component.controller;

import io.github.angelclient.client.ui.component.Component;

@FunctionalInterface
public interface Controller<T> {

	T get(Component component, T defaultValue);

}
