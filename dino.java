import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Run Dino Run - improved version with upgraded graphics.
 *
 * Controls:
 * SPACE / UP / W / ENTER - jump (hold for a higher jump)
 * DOWN / S - duck (in the air: fast fall)
 * P / ESC - pause
 * SPACE - start / restart
 */
public class Dino extends JPanel implements ActionListener, KeyListener {

    // ---------- Configuration ----------
    static final int W = 900, H = 400;
    static final int GROUND = 320;
    static final int DX = 90; // dino x position

    static final double GRAVITY = 2600;
    static final double JUMP_V = 820;
    static final double SHORT_HOP_V = 520;
    static final double START_SPEED = 380;
    static final double MAX_SPEED = 900;
    static final double ACCEL = 9;

    enum State {
        READY, RUNNING, PAUSED, OVER
    }

    // ---------- Game objects ----------
    static class Obstacle {
        double x;
        int w, h;
        boolean bird;
        int birdHeight; // distance of bird's bottom from the ground
        int[] stems; // cactus stem heights
    }

    static class Cloud {
        double x, y, scale, v;
    }

    static class Particle {
        double x, y, vx, vy, life, max;
    }

    // ---------- State ----------
    State state = State.READY;
    final Random rnd = new Random();

    double dinoY = 0, vy = 0;
    boolean onGround = true, duckHeld = false, jumpHeld = false, ducking = false;
    double speed = START_SPEED, distance = 0, score = 0;
    int hi = 0;
    boolean newHi = false;
    double animT = 0, overTime = 0, shake = 0, dustTimer = 0, blink = 0;
    int lastMilestone = 0;
    double nightness = 0, nightTarget = 0;
    double spawnCountdown = 600;
    long last = System.nanoTime();

    final List<Obstacle> obstacles = new ArrayList<>();
    final List<Cloud> clouds = new ArrayList<>();
    final List<Particle> particles = new ArrayList<>();
    final double[][] stars = new double[50][3];
    final double[][] pebbles = new double[70][3];

    final Path hiFile = Paths.get(System.getProperty("user.home"), ".dino_highscore");

    // ---------- Setup ----------
    public Dino() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setDoubleBuffered(true);
        addKeyListener(this);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (state == State.RUNNING)
                    state = State.PAUSED;
            }
        });

        for (double[] s : stars) {
            s[0] = rnd.nextInt(W);
            s[1] = rnd.nextInt(GROUND - 60);
            s[2] = rnd.nextDouble() * 6.28;
        }
        for (double[] p : pebbles) {
            p[0] = rnd.nextInt(W);
            p[1] = 12 + rnd.nextInt(60);
            p[2] = 2 + rnd.nextInt(6);
        }
        for (int i = 0; i < 5; i++) {
            Cloud c = new Cloud();
            c.x = rnd.nextInt(W);
            c.y = 30 + rnd.nextInt(120);
            c.scale = 0.7 + rnd.nextDouble() * 0.8;
            c.v = 8 + rnd.nextDouble() * 14;
            clouds.add(c);
        }
        loadHi();
        new javax.swing.Timer(16, this).start();
    }

    void loadHi() {
        try {
            hi = Integer.parseInt(new String(Files.readAllBytes(hiFile)).trim());
        } catch (Exception ignored) {
            hi = 0;
        }
    }

    void saveHi() {
        try {
            Files.write(hiFile, Integer.toString(hi).getBytes());
        } catch (Exception ignored) {
        }
    }

    void resetGame() {
        obstacles.clear();
        particles.clear();
        dinoY = 0;
        vy = 0;
        onGround = true;
        speed = START_SPEED;
        distance = 0;
        score = 0;
        spawnCountdown = 600;
        lastMilestone = 0;
        blink = 0;
        nightTarget = 0;
        newHi = false;
        shake = 0;
        state = State.RUNNING;
    }

    // ---------- Input ----------
    void pressJump() {
        switch (state) {
            case READY:
                resetGame();
                jump();
                break;
            case RUNNING:
                if (onGround)
                    jump();
                break;
            case PAUSED:
                state = State.RUNNING;
                break;
            case OVER:
                if (overTime > 0.4)
                    resetGame();
                break;
        }
    }

    void jump() {
        vy = JUMP_V;
        onGround = false;
        burst(DX + 20, GROUND, 5);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_ENTER) {
            if (!jumpHeld) {
                jumpHeld = true;
                pressJump();
            }
        } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            duckHeld = true;
        } else if (k == KeyEvent.VK_P || k == KeyEvent.VK_ESCAPE) {
            if (state == State.RUNNING)
                state = State.PAUSED;
            else if (state == State.PAUSED)
                state = State.RUNNING;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP || k == KeyEvent.VK_W || k == KeyEvent.VK_ENTER) {
            jumpHeld = false;
        } else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) {
            duckHeld = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // ---------- Game loop ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1e9);
        last = now;
        update(dt);
        repaint();
    }

    void update(double dt) {
        animT += dt;

        // clouds always drift
        for (Cloud c : clouds) {
            double v = c.v + (state == State.RUNNING ? speed * 0.08 : 0);
            c.x -= v * dt;
            if (c.x < -120) {
                c.x = W + 40 + rnd.nextInt(200);
                c.y = 30 + rnd.nextInt(120);
                c.scale = 0.7 + rnd.nextDouble() * 0.8;
            }
        }

        nightness += (nightTarget - nightness) * Math.min(1, dt * 0.9);
        updateParticles(dt);

        if (shake > 0)
            shake = Math.max(0, shake - dt);
        if (state == State.OVER)
            overTime += dt;
        if (blink > 0)
            blink -= dt;
        if (state != State.RUNNING)
            return;

        // progression
        speed = Math.min(MAX_SPEED, speed + ACCEL * dt);
        distance += speed * dt;
        score = distance / 12.0;
        nightTarget = ((int) (score / 600)) % 2 == 1 ? 1 : 0;

        int milestone = (int) (score / 100);
        if (milestone > lastMilestone) {
            lastMilestone = milestone;
            blink = 0.7;
        }

        // dino physics
        if (!onGround) {
            if (!jumpHeld && vy > SHORT_HOP_V)
                vy = SHORT_HOP_V; // variable jump height
            dinoY += vy * dt;
            vy -= GRAVITY * (duckHeld ? 3.0 : 1.0) * dt;
            if (dinoY <= 0) {
                dinoY = 0;
                vy = 0;
                onGround = true;
                burst(DX + 20, GROUND, 6);
            }
        }
        ducking = duckHeld && onGround;

        // running dust
        if (onGround) {
            dustTimer -= dt;
            if (dustTimer <= 0) {
                dustTimer = 0.09;
                burst(DX + 8, GROUND, 1);
            }
        }

        // obstacles
        spawnCountdown -= speed * dt;
        if (spawnCountdown <= 0) {
            spawn();
            spawnCountdown = 380 + rnd.nextInt(380) + speed * 0.45;
        }
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.x -= (speed + (o.bird ? 60 : 0)) * dt;
            if (o.x + o.w < -30)
                obstacles.remove(i);
        }

        if (collides())
            die();
    }

    void die() {
        state = State.OVER;
        overTime = 0;
        shake = 0.35;
        if ((int) score > hi) {
            hi = (int) score;
            newHi = true;
            saveHi();
        }
        burst(DX + 20, GROUND - dinoY - 20, 14);
    }

    void spawn() {
        Obstacle o = new Obstacle();
        o.x = W + 40;
        if (score > 250 && rnd.nextInt(100) < 28) {
            o.bird = true;
            o.w = 46;
            o.h = 32;
            int[] heights = { 4, 24, 80 };
            o.birdHeight = heights[rnd.nextInt(heights.length)];
        } else {
            int r = rnd.nextInt(10);
            int n = r < 5 ? 1 : (r < 8 || score < 150 ? 2 : 3);
            o.stems = new int[n];
            for (int i = 0; i < n; i++)
                o.stems[i] = rnd.nextBoolean() ? 40 : 54;
            o.w = n * 18 + (n - 1) * 6;
            int max = 0;
            for (int s : o.stems)
                max = Math.max(max, s);
            o.h = max;
        }
        obstacles.add(o);
    }

    // ---------- Collision ----------
    Rectangle2D.Double dinoBox() {
        double bottom = GROUND - dinoY;
        if (ducking)
            return new Rectangle2D.Double(DX + 6, bottom - 22, 46, 22);
        return new Rectangle2D.Double(DX + 10, bottom - 40, 26, 40);
    }

    boolean collides() {
        Rectangle2D.Double d = dinoBox();
        for (Obstacle o : obstacles) {
            if (o.bird) {
                double top = GROUND - o.birdHeight - 32;
                Rectangle2D.Double b = new Rectangle2D.Double(o.x + 6, top + 7, 36, 18);
                if (d.intersects(b))
                    return true;
            } else {
                for (int i = 0; i < o.stems.length; i++) {
                    double sx = o.x + i * 24 + 4;
                    Rectangle2D.Double c = new Rectangle2D.Double(sx, GROUND - o.stems[i] + 2, 10, o.stems[i] - 2);
                    if (d.intersects(c))
                        return true;
                }
            }
        }
        return false;
    }

    // ---------- Particles ----------
    void burst(double x, double y, int n) {
        for (int i = 0; i < n; i++) {
            Particle p = new Particle();
            p.x = x + rnd.nextDouble() * 10;
            p.y = y - 2;
            p.vx = -40 - rnd.nextDouble() * 120;
            p.vy = 30 + rnd.nextDouble() * 90;
            p.max = p.life = 0.3 + rnd.nextDouble() * 0.3;
            particles.add(p);
        }
    }

    void updateParticles(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y -= p.vy * dt;
            p.vy -= 260 * dt;
        }
    }

    // ---------- Drawing helpers ----------
    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    Color dayNight(Color day, Color night) {
        return mix(day, night, nightness);
    }

    void centered(Graphics2D g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (W - fm.stringWidth(s)) / 2, y);
    }

    // soft ground shadow
    void drawShadow(Graphics2D g, double cx, double w, double lift) {
        double k = Math.max(0.35, 1 - lift / 220.0);
        g.setColor(new Color(0, 0, 0, (int) (55 * k)));
        g.fill(new Ellipse2D.Double(cx - w * k / 2, GROUND - 3, w * k, 7));
    }

    // ---------- Painting ----------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (shake > 0) {
            g.translate((rnd.nextDouble() - 0.5) * shake * 30, (rnd.nextDouble() - 0.5) * shake * 30);
        }

        drawSky(g);
        drawStars(g);
        drawCelestial(g);
        drawHills(g, 0.05, GROUND - 40, 55, dayNight(new Color(176, 214, 196), new Color(28, 34, 68)), 0.011, 0.027);
        drawHills(g, 0.12, GROUND - 10, 40, dayNight(new Color(140, 194, 154), new Color(22, 28, 54)), 0.017, 0.041);
        drawClouds(g);
        drawGround(g);

        for (Obstacle o : obstacles) {
            if (o.bird)
                drawBird(g, o);
            else
                drawCactus(g, o);
        }
        drawParticles(g);
        drawDino(g);
        drawHud(g);
        drawOverlay(g);

        g.dispose();
        Toolkit.getDefaultToolkit().sync();
    }

    void drawSky(Graphics2D g) {
        Color top = dayNight(new Color(110, 190, 250), new Color(8, 12, 38));
        Color bot = dayNight(new Color(255, 238, 214), new Color(46, 44, 96));
        g.setPaint(new GradientPaint(0, 0, top, 0, GROUND, bot));
        g.fillRect(-40, -40, W + 80, H + 80);
    }

    void drawStars(Graphics2D g) {
        if (nightness < 0.05)
            return;
        for (double[] s : stars) {
            double tw = 0.5 + 0.5 * Math.sin(animT * 2 + s[2]);
            float a = (float) (nightness * (0.3 + 0.7 * tw));
            g.setColor(new Color(1f, 1f, 1f, Math.max(0f, Math.min(1f, a))));
            g.fillOval((int) s[0], (int) s[1], 2, 2);
        }
    }

    void drawCelestial(Graphics2D g) {
        int cx = 770, cy = 80;
        float sunA = (float) (1 - nightness);
        if (sunA > 0.02f) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sunA));
            g.setPaint(new RadialGradientPaint(cx, cy, 70, new float[] { 0f, 1f },
                    new Color[] { new Color(255, 240, 160, 200), new Color(255, 240, 160, 0) }));
            g.fillOval(cx - 70, cy - 70, 140, 140);
            g.setColor(new Color(255, 214, 64));
            g.fillOval(cx - 26, cy - 26, 52, 52);
            g.setComposite(old);
        }
        float moonA = (float) nightness;
        if (moonA > 0.02f) {
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, moonA));
            g.setColor(new Color(238, 238, 220));
            g.fillOval(cx - 24, cy - 24, 48, 48);
            g.setColor(new Color(210, 210, 192));
            g.fillOval(cx - 12, cy - 10, 12, 12);
            g.fillOval(cx + 4, cy + 4, 9, 9);
            g.fillOval(cx + 6, cy - 14, 7, 7);
            g.setComposite(old);
        }
    }

    void drawHills(Graphics2D g, double parallax, int base, int amp, Color c, double f1, double f2) {
        double off = distance * parallax;
        Path2D.Double p = new Path2D.Double();
        p.moveTo(-10, GROUND + 5);
        for (int x = -10; x <= W + 10; x += 8) {
            double y = base - amp * 0.5 * (Math.sin((x + off) * f1) + 0.5 * Math.sin((x + off) * f2) + 1.0);
            p.lineTo(x, y);
        }
        p.lineTo(W + 10, GROUND + 5);
        p.closePath();
        g.setColor(c);
        g.fill(p);
    }

    void drawClouds(Graphics2D g) {
        int a = (int) (255 * (1 - 0.65 * nightness));
        g.setColor(new Color(255, 255, 255, a));
        for (Cloud c : clouds) {
            double s = c.scale;
            g.fill(new Ellipse2D.Double(c.x, c.y + 10 * s, 70 * s, 22 * s));
            g.fill(new Ellipse2D.Double(c.x + 12 * s, c.y, 34 * s, 26 * s));
            g.fill(new Ellipse2D.Double(c.x + 34 * s, c.y + 5 * s, 28 * s, 22 * s));
        }
    }

    void drawGround(Graphics2D g) {
        Color top = dayNight(new Color(226, 190, 140), new Color(70, 58, 80));
        Color bot = dayNight(new Color(190, 150, 104), new Color(40, 34, 54));
        g.setPaint(new GradientPaint(0, GROUND, top, 0, H, bot));
        g.fillRect(-40, GROUND, W + 80, H - GROUND + 40);
        g.setColor(dayNight(new Color(120, 90, 60), new Color(130, 120, 150)));
        g.fillRect(-40, GROUND, W + 80, 3);

        g.setColor(dayNight(new Color(150, 115, 80), new Color(100, 90, 120)));
        for (double[] p : pebbles) {
            double x = ((p[0] - distance) % W + W) % W;
            g.fillRoundRect((int) x, (int) (GROUND + p[1] / 1.2 + 8), (int) p[2], 2, 2, 2);
        }
    }

    // ---------- CACTUS ----------
    void drawCactus(Graphics2D g, Obstacle o) {
        Color base = dayNight(new Color(52, 148, 92), new Color(88, 176, 128));
        Color light = dayNight(new Color(116, 204, 144), new Color(156, 224, 182));
        Color dark = dayNight(new Color(30, 104, 66), new Color(60, 134, 96));
        Stroke old = g.getStroke();

        for (int i = 0; i < o.stems.length; i++) {
            double x0 = o.x + i * 24;
            int h = o.stems[i];
            double top = GROUND - h;

            drawShadow(g, x0 + 9, 30, 0);

            // arms (drawn first so the trunk overlaps their joints)
            if (h >= 44) {
                double ay = GROUND - h * 0.55;
                g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(base);

                Path2D.Double left = new Path2D.Double();
                left.moveTo(x0 + 7, ay + 3);
                left.lineTo(x0 - 1, ay + 3);
                left.lineTo(x0 - 1, ay - 10);
                g.draw(left);

                Path2D.Double right = new Path2D.Double();
                right.moveTo(x0 + 11, ay - 3);
                right.lineTo(x0 + 19, ay - 3);
                right.lineTo(x0 + 19, ay - 16);
                g.draw(right);

                // arm highlights
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(light);
                g.draw(new Line2D.Double(x0 - 2.5, ay - 9, x0 - 2.5, ay + 1));
                g.draw(new Line2D.Double(x0 + 16.5, ay - 15, x0 + 16.5, ay - 5));
                g.setStroke(old);
            }

            // trunk with cylindrical shading
            g.setPaint(new LinearGradientPaint(
                    new Point2D.Double(x0 + 3, 0), new Point2D.Double(x0 + 15, 0),
                    new float[] { 0f, 0.4f, 1f }, new Color[] { light, base, dark }));
            g.fill(new RoundRectangle2D.Double(x0 + 3, top, 12, h + 2, 12, 12));

            // vertical ribs
            g.setColor(new Color(dark.getRed(), dark.getGreen(), dark.getBlue(), 120));
            g.fill(new Rectangle2D.Double(x0 + 6.5, top + 5, 1, h - 6));
            g.fill(new Rectangle2D.Double(x0 + 9.5, top + 4, 1, h - 5));
            g.fill(new Rectangle2D.Double(x0 + 12, top + 6, 1, h - 7));

            // spines
            g.setColor(new Color(245, 240, 205, 210));
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int k = 0, y = (int) top + 9; y < GROUND - 6; k++, y += 10) {
                double sx = (k % 2 == 0) ? x0 + 4 : x0 + 14;
                double dx = (k % 2 == 0) ? -3 : 3;
                g.draw(new Line2D.Double(sx, y, sx + dx, y - 2));
            }
            g.setStroke(old);

            // little flower on the tall stems
            if (h >= 54) {
                double fx = x0 + 9, fy = top - 1;
                g.setColor(new Color(255, 120, 160));
                for (int p = 0; p < 5; p++) {
                    double a = p * Math.PI * 2 / 5;
                    g.fill(new Ellipse2D.Double(fx + Math.cos(a) * 3 - 2.2, fy + Math.sin(a) * 3 - 2.2, 4.4, 4.4));
                }
                g.setColor(new Color(255, 214, 64));
                g.fill(new Ellipse2D.Double(fx - 2, fy - 2, 4, 4));
            }
        }
    }

    // ---------- BIRD ----------
    Path2D.Double birdWing(double f, double dx) {
        Path2D.Double w = new Path2D.Double();
        w.moveTo(16 + dx, 15);
        w.quadTo(24 + dx, 15 - 24 * f, 42 + dx, 15 - 18 * f);
        w.quadTo(32 + dx, 16, 34 + dx, 19);
        w.closePath();
        return w;
    }

    void drawBird(Graphics2D g0, Obstacle o) {
        double y = GROUND - o.birdHeight - 32;
        Color c = dayNight(new Color(124, 92, 214), new Color(168, 140, 240));
        Color dark = dayNight(new Color(84, 56, 170), new Color(124, 96, 206));
        Color belly = dayNight(new Color(218, 206, 252), new Color(232, 222, 255));
        Color beak = new Color(255, 184, 48);
        Color beakLo = new Color(226, 142, 28);

        Graphics2D g = (Graphics2D) g0.create();
        g.translate(o.x, y);

        double f = Math.sin(animT * 14); // flap phase (-1 .. 1)
        double f2 = Math.sin(animT * 14 - 0.9) * 0.8;

        // far wing (behind the body)
        g.setColor(dark);
        g.fill(birdWing(f2, 5));

        // tail fan
        Path2D.Double tail = new Path2D.Double();
        tail.moveTo(40, 14);
        tail.lineTo(53, 9);
        tail.lineTo(54, 16);
        tail.lineTo(52, 23);
        tail.lineTo(40, 20);
        tail.closePath();
        g.setColor(dark);
        g.fill(tail);

        // body + belly
        g.setColor(c);
        g.fill(new Ellipse2D.Double(10, 9, 34, 18));
        g.setColor(belly);
        g.fill(new Ellipse2D.Double(13, 17, 26, 8));

        // head crest
        Path2D.Double crest = new Path2D.Double();
        crest.moveTo(7, 8);
        crest.lineTo(5, 0);
        crest.lineTo(11, 6);
        crest.lineTo(13, -1);
        crest.lineTo(16, 8);
        crest.closePath();
        g.setColor(dark);
        g.fill(crest);

        // head
        g.setColor(c);
        g.fill(new Ellipse2D.Double(2, 6, 17, 17));

        // beak (upper + lower)
        Path2D.Double up = new Path2D.Double();
        up.moveTo(4, 12);
        up.lineTo(-8, 16.5);
        up.lineTo(4, 17.5);
        up.closePath();
        g.setColor(beak);
        g.fill(up);
        Path2D.Double lo = new Path2D.Double();
        lo.moveTo(4, 17.5);
        lo.lineTo(-5, 17.5);
        lo.lineTo(4, 21);
        lo.closePath();
        g.setColor(beakLo);
        g.fill(lo);

        // eye + angry brow
        g.setColor(Color.WHITE);
        g.fill(new Ellipse2D.Double(6, 10, 6, 6));
        g.setColor(Color.BLACK);
        g.fill(new Ellipse2D.Double(6.5, 11.5, 3.2, 3.6));
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(dark);
        g.draw(new Line2D.Double(4.5, 8.5, 12.5, 11));

        // near wing (in front)
        g.setColor(c);
        Path2D.Double wing = birdWing(f, 0);
        g.fill(wing);
        g.setColor(dark);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(wing);

        g.dispose();
    }

    // ---------- DINO ----------
    void drawDino(Graphics2D g0) {
        Color base = dayNight(new Color(240, 142, 52), new Color(255, 176, 96));
        Color dark = dayNight(new Color(190, 94, 30), new Color(214, 124, 64));
        Color belly = dayNight(new Color(255, 228, 176), new Color(255, 238, 205));
        Color ink = dayNight(new Color(60, 34, 20), new Color(40, 24, 20));

        boolean dead = state == State.OVER;
        int phase = ((int) (animT * 12)) % 2;
        boolean running = state == State.RUNNING && onGround;

        drawShadow(g0, DX + (ducking ? 32 : 24), ducking ? 56 : 40, dinoY);
        Graphics2D g = (Graphics2D) g0.create();

        // ---------------- DUCKING ----------------
        if (ducking) {
            g.translate(DX, GROUND - dinoY - 30);

            // legs
            int h1 = (running && phase == 0) ? 5 : 8;
            int h2 = (running && phase == 1) ? 5 : 8;
            g.setColor(dark);
            g.fill(new RoundRectangle2D.Double(14, 22, 7, h1, 3, 3));
            g.fill(new RoundRectangle2D.Double(14, 22 + h1 - 3, 10, 3, 3, 3));
            g.fill(new RoundRectangle2D.Double(30, 22, 7, h2, 3, 3));
            g.fill(new RoundRectangle2D.Double(30, 22 + h2 - 3, 10, 3, 3, 3));

            // tail
            Path2D.Double tail = new Path2D.Double();
            tail.moveTo(12, 10);
            tail.quadTo(2, 12, -8, 20);
            tail.quadTo(4, 21, 14, 22);
            tail.closePath();
            g.setColor(base);
            g.fill(tail);

            // back spikes (peek out above the body)
            g.setColor(dark);
            for (int x = 14; x <= 34; x += 8) {
                Path2D.Double sp = new Path2D.Double();
                sp.moveTo(x, 10);
                sp.lineTo(x + 4, 1);
                sp.lineTo(x + 8, 10);
                sp.closePath();
                g.fill(sp);
            }

            // body + belly
            g.setColor(base);
            g.fill(new Ellipse2D.Double(6, 6, 42, 20));
            g.setColor(belly);
            g.fill(new Ellipse2D.Double(14, 16, 26, 9));

            // head
            g.setColor(base);
            g.fill(new RoundRectangle2D.Double(40, 0, 24, 16, 10, 10));

            // face
            drawDinoFace(g, 51, 3, 62, 5, 48, 11, 63, 11, dead, ink);
            g.dispose();
            return;
        }

        // ---------------- STANDING / RUNNING / JUMPING ----------------
        g.translate(DX, GROUND - dinoY - 44);

        int hBack = 14, hFront = 14;
        if (running) {
            if (phase == 0)
                hBack = 9;
            else
                hFront = 9;
        } else if (!onGround) {
            hBack = 12;
            hFront = 12;
        }

        // legs + feet
        g.setColor(dark);
        g.fill(new RoundRectangle2D.Double(12, 28, 8, hBack + 2, 4, 4));
        g.fill(new RoundRectangle2D.Double(12, 30 + hBack - 4, 12, 4, 4, 4));
        g.setColor(base);
        g.fill(new RoundRectangle2D.Double(25, 28, 8, hFront + 2, 4, 4));
        g.fill(new RoundRectangle2D.Double(25, 30 + hFront - 4, 12, 4, 4, 4));

        // tail
        Path2D.Double tail = new Path2D.Double();
        tail.moveTo(10, 20);
        tail.quadTo(0, 17, -8, 29);
        tail.quadTo(4, 30, 13, 35);
        tail.closePath();
        g.setColor(base);
        g.fill(tail);

        // back spikes
        g.setColor(dark);
        int[][] sp = { { 11, 21 }, { 17, 15 }, { 23, 10 } };
        for (int[] s : sp) {
            Path2D.Double t = new Path2D.Double();
            t.moveTo(s[0] - 4, s[1] + 6);
            t.lineTo(s[0] - 1, s[1] - 5);
            t.lineTo(s[0] + 5, s[1] + 3);
            t.closePath();
            g.fill(t);
        }

        // body + belly
        g.setColor(base);
        g.fill(new Ellipse2D.Double(6, 14, 34, 26));
        g.setColor(belly);
        g.fill(new Ellipse2D.Double(17, 24, 21, 14));

        // neck + head
        g.setColor(base);
        g.fill(new RoundRectangle2D.Double(26, 6, 14, 22, 8, 8));
        g.fill(new RoundRectangle2D.Double(26, 0, 24, 16, 10, 10));

        // tiny arm
        g.setColor(dark);
        g.fill(new RoundRectangle2D.Double(34, 26, 9, 4, 4, 4));

        // face
        drawDinoFace(g, 38, 3, 46, 5, 35, 12, 50, 12, dead, ink);
        g.setColor(new Color(255, 120, 120, 110)); // cheek blush
        g.fill(new Ellipse2D.Double(33, 8, 5, 3));

        g.dispose();
    }

    // eye (ex,ey), nostril (nx,ny), mouth line (mx1,my1)-(mx2,my2)
    void drawDinoFace(Graphics2D g, double ex, double ey, double nx, double ny,
            double mx1, double my1, double mx2, double my2,
            boolean dead, Color ink) {
        Stroke old = g.getStroke();
        if (dead) {
            g.setColor(ink);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(ex, ey, ex + 6, ey + 6));
            g.draw(new Line2D.Double(ex + 6, ey, ex, ey + 6));
            g.draw(new Line2D.Double(mx1 + 2, my1 + 1, mx2 - 4, my2 + 2)); // wobbly mouth
        } else {
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(ex, ey, 7, 7));
            g.setColor(ink);
            g.fill(new Ellipse2D.Double(ex + 3, ey + 1.5, 3.5, 4));
            g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(mx1, my1, mx2, my2));
        }
        g.setStroke(old);
        g.setColor(ink);
        g.fill(new Ellipse2D.Double(nx, ny, 2, 2));
    }

    void drawParticles(Graphics2D g) {
        Color c = dayNight(new Color(150, 115, 80), new Color(150, 140, 170));
        for (Particle p : particles) {
            float a = (float) Math.max(0, Math.min(1, p.life / p.max));
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (200 * a)));
            double r = 2 + 3 * (1 - a);
            g.fill(new Ellipse2D.Double(p.x, p.y, r, r));
        }
    }

    void drawHud(Graphics2D g) {
        Color c = dayNight(new Color(50, 54, 66), new Color(235, 236, 245));
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 22));
        String hs = String.format("HI %05d", hi);
        String sc = String.format("%05d", (int) score);
        FontMetrics fm = g.getFontMetrics();
        int right = W - 24;
        boolean hide = blink > 0 && ((int) (blink * 10)) % 2 == 0;
        if (!hide) {
            g.setColor(c);
            g.drawString(sc, right - fm.stringWidth(sc), 36);
        }
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        g.drawString(hs, right - fm.stringWidth(sc) - 24 - fm.stringWidth(hs), 36);
    }

    void drawOverlay(Graphics2D g) {
        if (state == State.RUNNING)
            return;
        Color text = Color.WHITE;

        if (state == State.READY) {
            g.setColor(new Color(0, 0, 0, 70));
            g.fillRect(0, 0, W, H);
            g.setColor(text);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            centered(g, "RUN DINO RUN", 140);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            double pulse = 0.6 + 0.4 * Math.sin(animT * 4);
            g.setColor(new Color(255, 255, 255, (int) (255 * pulse)));
            centered(g, "Press SPACE to start", 190);
            g.setColor(new Color(255, 255, 255, 190));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            centered(g, "SPACE / UP = jump (hold for higher)   DOWN = duck   P = pause", 225);
        } else if (state == State.PAUSED) {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRect(0, 0, W, H);
            g.setColor(text);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
            centered(g, "PAUSED", 180);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            centered(g, "Press P or SPACE to resume", 220);
        } else if (state == State.OVER) {
            g.setColor(new Color(0, 0, 0, 130));
            g.fillRect(0, 0, W, H);
            g.setColor(text);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
            centered(g, "GAME OVER", 140);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            centered(g, "Score: " + (int) score, 185);
            if (newHi) {
                g.setColor(new Color(255, 214, 64));
                centered(g, "NEW HIGH SCORE!", 220);
                g.setColor(text);
            }
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
            if (overTime > 0.4) {
                double pulse = 0.6 + 0.4 * Math.sin(animT * 4);
                g.setColor(new Color(255, 255, 255, (int) (255 * pulse)));
                centered(g, "Press SPACE to restart", 265);
            }
        }
    }

    // ---------- Entry point ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Run Dino Run");
            Dino game = new Dino();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(game);
            f.setResizable(false);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
