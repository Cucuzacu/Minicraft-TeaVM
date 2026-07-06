package com.mojang.ld22.sound;

import org.teavm.jso.JSBody;

public class Sound {
	public static final Sound playerHurt = new Sound("/playerhurt.wav");
	public static final Sound playerDeath = new Sound("/death.wav");
	public static final Sound monsterHurt = new Sound("/monsterhurt.wav");
	public static final Sound test = new Sound("/test.wav");
	public static final Sound pickup = new Sound("/pickup.wav");
	public static final Sound bossdeath = new Sound("/bossdeath.wav");
	public static final Sound craft = new Sound("/craft.wav");

	private String name;

	private Sound(String name) {
		this.name = name;
		initWebAudio();
		loadAudio(name);
	}

	public void play() {
		playAudio(name);
	}

	@JSBody(script = 
		"if (typeof window.audioCtx === 'undefined') {" +
		"    window.audioCtx = new (window.AudioContext || window.webkitAudioContext)();" +
		"    window.audioBufferCache = {};" +
		"}"
	)
	private static native void initWebAudio();

	@JSBody(params = { "name" }, script = 
		"if (window.audioBufferCache[name]) return;" +
		"window.audioBufferCache[name] = 'loading';" +
		"var url = name.indexOf('/') === 0 ? name.substring(1) : name;" +
		"fetch(url)" +
		"    .then(function(res) { return res.arrayBuffer(); })" +
		"    .then(function(buf) { return window.audioCtx.decodeAudioData(buf); })" +
		"    .then(function(decoded) { window.audioBufferCache[name] = decoded; })" +
		"    .catch(function(err) { console.error('Error loading sound ' + name, err); });"
	)
	private static native void loadAudio(String name);

	@JSBody(params = { "name" }, script = 
		"var buffer = window.audioBufferCache[name];" +
		"if (buffer && buffer !== 'loading') {" +
		"    if (window.audioCtx.state === 'suspended') window.audioCtx.resume();" + // Fixes Chrome/Safari autoplay policies
		"    var source = window.audioCtx.createBufferSource();" +
		"    source.buffer = buffer;" +
		"    source.connect(window.audioCtx.destination);" +
		"    source.start(0);" +
		"}"
	)
	private static native void playAudio(String name);
}