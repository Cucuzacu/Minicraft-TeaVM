package com.mojang.ld22;

import java.util.Random;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import com.mojang.ld22.entity.Player;
import com.mojang.ld22.gfx.Color;
import com.mojang.ld22.gfx.Font;
import com.mojang.ld22.gfx.Screen;
import com.mojang.ld22.gfx.SpriteSheet;
import com.mojang.ld22.level.Level;
import com.mojang.ld22.level.tile.Tile;
import com.mojang.ld22.screen.DeadMenu;
import com.mojang.ld22.screen.LevelTransitionMenu;
import com.mojang.ld22.screen.Menu;
import com.mojang.ld22.screen.TitleMenu;
import com.mojang.ld22.screen.WonMenu;

public class Game {
    private Random random = new Random();
    public static final String NAME = "Minicraft";
    public static final int HEIGHT = 120;
    public static final int WIDTH = 160;
    private static final int SCALE = 3;

    private HTMLCanvasElement canvas;
    private CanvasRenderingContext2D ctx;
    private ImageData imageData;
    private Uint8ClampedArray imagePixels;

    private int[] pixels = new int[WIDTH * HEIGHT];
    private boolean running = false;
    private boolean isFocused = true;
    
    private Screen screen;
    private Screen lightScreen;
    private InputHandler input = new InputHandler(this);

    private int[] colors = new int[256];
    private int tickCount = 0;
    public int gameTime = 0;

    private Level level;
    private Level[] levels = new Level[5];
    private int currentLevel = 3;
    public Player player;

    public Menu menu;
    private int playerDeadTime;
    private int pendingLevelChange;
    private int wonTimer = 0;
    public boolean hasWon = false;

    private long lastTime;
    private double unprocessed = 0;
    private double nsPerTick = 1000000000.0 / 60.0;
    private int frames = 0;
    private int ticks = 0;
    private long lastTimer1;

    public void setMenu(Menu menu) {
        this.menu = menu;
        if (menu != null) menu.init(this, input);
    }

    public void start() {
        running = true;
        initBrowserCanvas();
        init();
    }

    public void stop() {
        running = false;
    }

    private static final int SCALED_WIDTH = WIDTH * SCALE;
    private static final int SCALED_HEIGHT = HEIGHT * SCALE;

    private void initBrowserCanvas() {
        HTMLDocument doc = HTMLDocument.current();
        canvas = (HTMLCanvasElement) doc.createElement("canvas");
    
        canvas.setWidth(SCALED_WIDTH);
        canvas.setHeight(SCALED_HEIGHT);
    
        canvas.getStyle().setProperty("width", SCALED_WIDTH + "px");
        canvas.getStyle().setProperty("height", SCALED_HEIGHT + "px");
        canvas.getStyle().setProperty("image-rendering", "pixelated");
        canvas.getStyle().setProperty("outline", "none");
    
        canvas.setTabIndex(0);
        canvas.addEventListener("focus", evt -> isFocused = true);
        canvas.addEventListener("blur", evt -> isFocused = false);
    
        doc.getBody().appendChild(canvas);
        canvas.focus();

        ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
        imageData = ctx.createImageData(SCALED_WIDTH, SCALED_HEIGHT);
        imagePixels = imageData.getData();
    }

    public boolean hasFocus() {
        return isFocused;
    }

    public void resetGame() {
        playerDeadTime = 0;
        wonTimer = 0;
        gameTime = 0;
        hasWon = false;

        levels = new Level[5];
        currentLevel = 3;

        levels[4] = new Level(128, 128, 1, null);
        levels[3] = new Level(128, 128, 0, levels[4]);
        levels[2] = new Level(128, 128, -1, levels[3]);
        levels[1] = new Level(128, 128, -2, levels[2]);
        levels[0] = new Level(128, 128, -3, levels[1]);

        level = levels[currentLevel];
        player = new Player(this, input);
        player.findStartPos(level);

        level.add(player);

        for (int i = 0; i < 5; i++) {
            levels[i].trySpawn(5000);
        }
    }

    private void init() {
        int pp = 0;
        for (int r = 0; r < 6; r++) {
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int rr = (r * 255 / 5);
                    int gg = (g * 255 / 5);
                    int bb = (b * 255 / 5);
                    int mid = (rr * 30 + gg * 59 + bb * 11) / 100;

                    int r1 = ((rr + mid * 1) / 2) * 230 / 255 + 10;
                    int g1 = ((gg + mid * 1) / 2) * 230 / 255 + 10;
                    int b1 = ((bb + mid * 1) / 2) * 230 / 255 + 10;
                    colors[pp++] = r1 << 16 | g1 << 8 | b1;
                }
            }
        }

        HTMLDocument doc = HTMLDocument.current();
HTMLImageElement img = (HTMLImageElement) doc.createElement("img");

        img.addEventListener("load", evt -> {
            int imgWidth = img.getNaturalWidth();
            int imgHeight = img.getNaturalHeight();

            HTMLCanvasElement offscreen = (HTMLCanvasElement) doc.createElement("canvas");
            offscreen.setWidth(imgWidth);
            offscreen.setHeight(imgHeight);
            CanvasRenderingContext2D oCtx = (CanvasRenderingContext2D) offscreen.getContext("2d");
            oCtx.drawImage(img, 0, 0);

            ImageData oData = oCtx.getImageData(0, 0, imgWidth, imgHeight);
            Uint8ClampedArray rgba = oData.getData();
            int[] iconPixels = new int[imgWidth * imgHeight];

            for (int i = 0; i < iconPixels.length; i++) {
                int b = rgba.get(i * 4 + 2) & 0xFF;
                iconPixels[i] = b;
            }

            screen = new Screen(WIDTH, HEIGHT, new SpriteSheet(imgWidth, imgHeight, iconPixels));
            lightScreen = new Screen(WIDTH, HEIGHT, new SpriteSheet(imgWidth, imgHeight, iconPixels));

            resetGame();
            setMenu(new TitleMenu());

            lastTime = System.nanoTime();
            lastTimer1 = System.currentTimeMillis();
            requestNextFrame();
        });

        img.setSrc("icons.png");
    }

    private void requestNextFrame() {
        if (running) {
            Window.current().requestAnimationFrame(timestamp -> loop());
        }
    }

    public void loop() {
        long now = System.nanoTime();
        unprocessed += (now - lastTime) / nsPerTick;
        lastTime = now;
        boolean shouldRender = true;

        while (unprocessed >= 1) {
            ticks++;
            tick();
            unprocessed -= 1;
            shouldRender = true;
        }

        if (shouldRender) {
            frames++;
            render();
        }

        if (System.currentTimeMillis() - lastTimer1 > 1000) {
            lastTimer1 += 1000;
            System.out.println(ticks + " ticks, " + frames + " fps");
            frames = 0;
            ticks = 0;
        }

        requestNextFrame();
    }

    public void tick() {
        tickCount++;
        if (!hasFocus()) {
            input.releaseAll();
        } else {
            if (!player.removed && !hasWon) gameTime++;

            input.tick();
            if (menu != null) {
                menu.tick();
            } else {
                if (player.removed) {
                    playerDeadTime++;
                    if (playerDeadTime > 60) {
                        setMenu(new DeadMenu());
                    }
                } else {
                    if (pendingLevelChange != 0) {
                        setMenu(new LevelTransitionMenu(pendingLevelChange));
                        pendingLevelChange = 0;
                    }
                }
                if (wonTimer > 0) {
                    if (--wonTimer == 0) {
                        setMenu(new WonMenu());
                    }
                }
                level.tick();
                Tile.tickCount++;
            }
        }
    }

    public void changeLevel(int dir) {
        level.remove(player);
        currentLevel += dir;
        level = levels[currentLevel];
        player.x = (player.x >> 4) * 16 + 8;
        player.y = (player.y >> 4) * 16 + 8;
        level.add(player);
    }

    public void render() {
        int xScroll = player.x - screen.w / 2;
        int yScroll = player.y - (screen.h - 8) / 2;
        if (xScroll < 16) xScroll = 16;
        if (yScroll < 16) yScroll = 16;
        if (xScroll > level.w * 16 - screen.w - 16) xScroll = level.w * 16 - screen.w - 16;
        if (yScroll > level.h * 16 - screen.h - 16) yScroll = level.h * 16 - screen.h - 16;
        
        if (currentLevel > 3) {
            int col = Color.get(20, 20, 121, 121);
            for (int y = 0; y < 14; y++)
                for (int x = 0; x < 24; x++) {
                    screen.render(x * 8 - ((xScroll / 4) & 7), y * 8 - ((yScroll / 4) & 7), 0, col, 0);
                }
        }

        level.renderBackground(screen, xScroll, yScroll);
        level.renderSprites(screen, xScroll, yScroll);

        if (currentLevel < 3) {
            lightScreen.clear(0);
            level.renderLight(lightScreen, xScroll, yScroll);
            screen.overlay(lightScreen, xScroll, yScroll);
        }

        renderGui();

        if (!hasFocus()) renderFocusNagger();

        for (int y = 0; y < screen.h; y++) {
            for (int x = 0; x < screen.w; x++) {
                int cc = screen.pixels[x + y * screen.w];
                if (cc < 255) pixels[x + y * WIDTH] = colors[cc];
            }
        }

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int color = pixels[x + y * WIDTH];
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                for (int ys = 0; ys < SCALE; ys++) {
                    int canvasY = y * SCALE + ys;
                    for (int xs = 0; xs < SCALE; xs++) {
                        int canvasX = x * SCALE + xs;
                        int idx = (canvasX + canvasY * SCALED_WIDTH) * 4;

                        imagePixels.set(idx, (short) r);
                        imagePixels.set(idx + 1, (short) g);
                        imagePixels.set(idx + 2, (short) b);
                        imagePixels.set(idx + 3, (short) 255);
                    }
                }
            }
        }
    
        ctx.putImageData(imageData, 0, 0);
    }

    private void renderGui() {
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 20; x++) {
                screen.render(x * 8, screen.h - 16 + y * 8, 0 + 12 * 32, Color.get(000, 000, 000, 000), 0);
            }
        }

        for (int i = 0; i < 10; i++) {
            if (i < player.health)
                screen.render(i * 8, screen.h - 16, 0 + 12 * 32, Color.get(000, 200, 500, 533), 0);
            else
                screen.render(i * 8, screen.h - 16, 0 + 12 * 32, Color.get(000, 100, 000, 000), 0);

            if (player.staminaRechargeDelay > 0) {
                if (player.staminaRechargeDelay / 4 % 2 == 0)
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 555, 000, 000), 0);
                else
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 110, 000, 000), 0);
            } else {
                if (i < player.stamina)
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 220, 550, 553), 0);
                else
                    screen.render(i * 8, screen.h - 8, 1 + 12 * 32, Color.get(000, 110, 000, 000), 0);
            }
        }
        if (player.activeItem != null) {
            player.activeItem.renderInventory(screen, 10 * 8, screen.h - 16);
        }

        if (menu != null) {
            menu.render(screen);
        }
    }

    private void renderFocusNagger() {
        String msg = "Click to focus!";
        int xx = (WIDTH - msg.length() * 8) / 2;
        int yy = (HEIGHT - 8) / 2;
        int w = msg.length();
        int h = 1;

        screen.render(xx - 8, yy - 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
        screen.render(xx + w * 8, yy - 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
        screen.render(xx - 8, yy + 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
        screen.render(xx + w * 8, yy + 8, 0 + 13 * 32, Color.get(-1, 1, 5, 445), 3);
        for (int x = 0; x < w; x++) {
            screen.render(xx + x * 8, yy - 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
            screen.render(xx + x * 8, yy + 8, 1 + 13 * 32, Color.get(-1, 1, 5, 445), 2);
        }
        for (int y = 0; y < h; y++) {
            screen.render(xx - 8, yy + y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 0);
            screen.render(xx + w * 8, yy + y * 8, 2 + 13 * 32, Color.get(-1, 1, 5, 445), 1);
        }

        if ((tickCount / 20) % 2 == 0) {
            Font.draw(msg, screen, xx, yy, Color.get(5, 333, 333, 333));
        } else {
            Font.draw(msg, screen, xx, yy, Color.get(5, 555, 555, 555));
        }
    }

    public void scheduleLevelChange(int dir) {
        pendingLevelChange = dir;
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.start();
    }

    public void won() {
        wonTimer = 60 * 3;
        hasWon = true;
    }
}