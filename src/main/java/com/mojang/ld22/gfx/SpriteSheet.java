package com.mojang.ld22.gfx;

public class SpriteSheet {
    public int width, height;
    public int[] pixels;

    public SpriteSheet(int width, int height, int[] pixels) {
        this.width = width;
        this.height = height;
        this.pixels = new int[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            this.pixels[i] = (pixels[i] & 0xff) / 64;
        }
    }
}