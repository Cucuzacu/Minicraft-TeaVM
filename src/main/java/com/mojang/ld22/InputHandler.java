package com.mojang.ld22;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.KeyboardEvent;

public class InputHandler {
	private static final int VK_TAB = 9;
	private static final int VK_ENTER = 13;
	private static final int VK_CONTROL = 17;
	private static final int VK_ALT = 18;
	private static final int VK_SPACE = 32;
	private static final int VK_LEFT = 37;
	private static final int VK_UP = 38;
	private static final int VK_RIGHT = 39;
	private static final int VK_DOWN = 40;
	private static final int VK_INSERT = 45;
	private static final int VK_A = 65;
	private static final int VK_C = 67;
	private static final int VK_D = 68;
	private static final int VK_S = 83;
	private static final int VK_W = 87;
	private static final int VK_X = 88;
	private static final int VK_NUMPAD0 = 96;
	private static final int VK_NUMPAD2 = 98;
	private static final int VK_NUMPAD4 = 100;
	private static final int VK_NUMPAD6 = 102;
	private static final int VK_NUMPAD8 = 104;

	public class Key {
		public int presses, absorbs;
		public boolean down, clicked;

		public Key() {
			keys.add(this);
		}

		public void toggle(boolean pressed) {
			if (pressed != down) {
				down = pressed;
			}
			if (pressed) {
				presses++;
			}
		}

		public void tick() {
			if (absorbs < presses) {
				absorbs++;
				clicked = true;
			} else {
				clicked = false;
			}
		}
	}

	public List<Key> keys = new ArrayList<Key>();

	public Key up = new Key();
	public Key down = new Key();
	public Key left = new Key();
	public Key right = new Key();
	public Key attack = new Key();
	public Key menu = new Key();

	public void releaseAll() {
		for (int i = 0; i < keys.size(); i++) {
			keys.get(i).down = false;
		}
	}

	public void tick() {
		for (int i = 0; i < keys.size(); i++) {
			keys.get(i).tick();
		}
	}

	public InputHandler(Game game) {
		Window.current().getDocument().addEventListener("keydown", new EventListener<KeyboardEvent>() {
			@Override
			public void handleEvent(KeyboardEvent ke) {
				toggle(ke, true);
				
				int code = ke.getKeyCode();
				if ((code >= 37 && code <= 40) || code == 32 || code == 9 || code == 13) {
					ke.preventDefault();
				}
			}
		});

		Window.current().getDocument().addEventListener("keyup", new EventListener<KeyboardEvent>() {
			@Override
			public void handleEvent(KeyboardEvent ke) {
				toggle(ke, false);
			}
		});
	}

	private void toggle(KeyboardEvent ke, boolean pressed) {
		int keyCode = ke.getKeyCode();

		if (keyCode == VK_NUMPAD8) up.toggle(pressed);
		if (keyCode == VK_NUMPAD2) down.toggle(pressed);
		if (keyCode == VK_NUMPAD4) left.toggle(pressed);
		if (keyCode == VK_NUMPAD6) right.toggle(pressed);
		if (keyCode == VK_W) up.toggle(pressed);
		if (keyCode == VK_S) down.toggle(pressed);
		if (keyCode == VK_A) left.toggle(pressed);
		if (keyCode == VK_D) right.toggle(pressed);
		if (keyCode == VK_UP) up.toggle(pressed);
		if (keyCode == VK_DOWN) down.toggle(pressed);
		if (keyCode == VK_LEFT) left.toggle(pressed);
		if (keyCode == VK_RIGHT) right.toggle(pressed);

		if (keyCode == VK_TAB) menu.toggle(pressed);
		if (keyCode == VK_ALT) menu.toggle(pressed);
		if (keyCode == VK_SPACE) attack.toggle(pressed);
		if (keyCode == VK_CONTROL) attack.toggle(pressed);
		if (keyCode == VK_NUMPAD0) attack.toggle(pressed);
		if (keyCode == VK_INSERT) attack.toggle(pressed);
		if (keyCode == VK_ENTER) menu.toggle(pressed);

		if (keyCode == VK_X) menu.toggle(pressed);
		if (keyCode == VK_C) attack.toggle(pressed);
	}
}