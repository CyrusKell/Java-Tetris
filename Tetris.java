
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Tetris extends JPanel implements ActionListener {

    public static final int HEIGHT = 1000;
    public static final int WIDTH = HEIGHT / 2;
    public static final int UNIT_SIZE = WIDTH / 10;
//    public static final int UNIT_SIZE = WIDTH / 50;
    private final Font font = new Font("Comic Sans MS", Font.BOLD, 30);
//    private final Block[] blocks = {new I(), new J(), new L(), new O(), new S(), new T(), new Z()};
    private final Block[] blocks = {new J(), new L()};
//    private final Block[] blocks = {new Cock()};
    private final int FPS = 40;
    private final int TICK_DELAY = 1000 / FPS;
    private Timer timer;
    private int level = 0;
    private int ticksPassed = 0;
    private int ticksPerCell = getTicksPerCell();
    private int score = 0;
    private int totalLinesCleared = 0;
    private int linesClearedOnCurrentLevel = 0;
    private int lastMove = 0;
    private int moveResets = 0;
    private boolean isRunning = true;
    private boolean isPaused = false;
    private boolean gameOver = false;
    private int x;
    private int y;
    private int ghostYOffset;
    private int offset;
    private Block activeBlock;
    private ArrayList<inactiveSquare> inactiveSquares;

    public Tetris() {
        inactiveSquares = new ArrayList<>();
        newBlock();
        timer = new Timer(TICK_DELAY, this);
        timer.start();
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(new MyKeyAdapter());
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.black);
        g.setFont(font);
        checkForLineFilled();
        checkForGameOver();
        paintGrid(g);
        for (inactiveSquare s : inactiveSquares) {
            s.draw(g);
        }
        paintGhost(g);
        activeBlock.draw(x, y, g);
        paintStats(g);
        g.setColor(new Color(0, 0, 0, 150));
        if (!isRunning) {
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
        if (isPaused) {
            g.setColor(Color.white);
            drawCenteredString(g, "Paused", 100);
        }
        if (gameOver) {
            g.setColor(Color.white);
            drawCenteredString(g, "Game Over", 200);
            drawCenteredString(g, "Press ESC to play again", HEIGHT / 2);
        }
    }

    public void paintGrid(Graphics g) {
        g.setColor(new Color(211, 211, 211, 50));
        for (int i = 0; i < HEIGHT; i += UNIT_SIZE) {
            g.drawLine(i, 0, i, HEIGHT);
            g.drawLine(0, i, WIDTH, i);
        }
    }

    public void paintGhost(Graphics g) {
        ghostYOffset = 0;
        while (canGhostMoveDown()) {
            ghostYOffset += UNIT_SIZE;
        }
        activeBlock.drawOutline(x, y + ghostYOffset, g);
    }

    public void paintStats(Graphics g) {
        g.setColor(Color.white);
        g.drawString("Level:", 10, 40);
        g.drawString("" + level, 125, 40);
        g.drawString("Score:", 10, 80);
        g.drawString("" + score, 125, 80);
        g.drawString("Clears:", 10, 120);
        g.drawString("" + totalLinesCleared, 125, 120);
    }

    public void drawCenteredString(Graphics g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics(font);
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    public void moveBlockDown() {
        if (canMoveDown()) {
            y += UNIT_SIZE;
            repaint();
            lastMove = ticksPassed;
            moveResets = 0;
        } else if (canLock()) {
            newBlock();
        }
    }

    public void rotate() {
        activeBlock.rotate();
        if (isObstructed()) {
            if (canMoveRight()) {
                x += UNIT_SIZE;
            } else if (canMoveLeft()) {
                x -= UNIT_SIZE;
            } else {
                for (int i = 0; i < 3; i++) {
                    activeBlock.rotate();
                }
            }
        }
        while (getSmallestXPos() < 0) {
            x += UNIT_SIZE;
        }
        while (getGreatestXPos() > WIDTH - UNIT_SIZE) {
            x -= UNIT_SIZE;
        }
        while (getGreatestYPos() >= HEIGHT) {
            y -= UNIT_SIZE;
        }
        repaint();
    }

    public void newBlock() {
        moveResets = 0;
        if (activeBlock != null) {
            for (Square s : activeBlock.getSquares()) {
                inactiveSquares.add(new inactiveSquare(getXCoord(s), getYCoord(s), activeBlock.getColor()));
            }
        }
        activeBlock = blocks[(int) (Math.random() * blocks.length)];
        if (activeBlock.isRotatedAroundCenter()) {
            offset = 0;
        } else {
            offset = (int) (UNIT_SIZE * 0.5);
        }
        x = WIDTH / 2 - UNIT_SIZE;
        y = 0;
//        while(isObstructed()) {
//            y -= UNIT_SIZE;
//        }
        repaint();
    }

    public int getXCoord(Square s) {
        return (int) (s.getXOffset() * UNIT_SIZE + offset + x);
    }

    public int getYCoord(Square s) {
        return (int) (s.getYOffset() * UNIT_SIZE + offset + y);
    }

    public int getSmallestYPos() {
        int min = HEIGHT;
        for (Square s : activeBlock.getSquares()) {
            if (getYCoord(s) < min) {
                min = getYCoord(s);
            }
        }
        return min;
    }
    
    public int getGreatestYPos() {
        int max = 0;
        for (Square s : activeBlock.getSquares()) {
            if (getYCoord(s) > max) {
                max = getYCoord(s);
            }
        }
        return max;
    }

    public int getSmallestXPos() {
        int min = WIDTH;
        for (Square s : activeBlock.getSquares()) {
            if (getXCoord(s) < min) {
                min = getXCoord(s);
            }
        }
        return min;
    }

    public int getGreatestXPos() {
        int max = 0;
        for (Square s : activeBlock.getSquares()) {
            if (getXCoord(s) > max) {
                max = getXCoord(s);
            }
        }
        return max;
    }

    public boolean squareExistsAt(int a, int b) {
        for (inactiveSquare s : inactiveSquares) {
            if (s.getXCoord() == a && s.getYCoord() == b) {
                return true;
            }
        }
        return false;
    }

    public boolean canMoveDown() {
        if (getGreatestYPos() + UNIT_SIZE + offset >= HEIGHT) {
            return false;
        }
        for (Square s : activeBlock.getSquares()) {
            if (squareExistsAt(getXCoord(s), getYCoord(s) + UNIT_SIZE)) {
                return false;
            }
        }
        return true;
    }

    public boolean canGhostMoveDown() {
        if (getGreatestYPos() + ghostYOffset + offset + UNIT_SIZE >= HEIGHT) {
            return false;
        }
        for (Square s : activeBlock.getSquares()) {
            if (squareExistsAt(getXCoord(s), getYCoord(s) + ghostYOffset + UNIT_SIZE)) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveLeft() {
        for (Square s : activeBlock.getSquares()) {
            if (squareExistsAt(getXCoord(s) - UNIT_SIZE, getYCoord(s))) {
                return false;
            }
            if (getXCoord(s) - UNIT_SIZE < 0) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveRight() {
        for (Square s : activeBlock.getSquares()) {
            if (squareExistsAt(getXCoord(s) + UNIT_SIZE, getYCoord(s))) {
                return false;
            }
            if (getXCoord(s) + UNIT_SIZE >= WIDTH) {
                return false;
            }
        }
        return true;
    }

    public boolean isObstructed() {
        for (Square s : activeBlock.getSquares()) {
            if (squareExistsAt(getXCoord(s), getYCoord(s))) {
                return true;
            }
            if (getYCoord(s) >= HEIGHT) {
                return true;
            }
            if (getXCoord(s) >= WIDTH) {
                return true;
            }
            if (getXCoord(s) < 0) {
                return true;
            }
        }
        return false;
    }

    public void checkForLineFilled() {
        int linesClearedAtOnce = 0;
        for (int y = 0; y < HEIGHT; y += UNIT_SIZE) {
            if (isLineFilled(y)) {
                clearLine(y);
                linesClearedAtOnce++;
                y -= UNIT_SIZE;
            }
        }
        addPoints(linesClearedAtOnce);
        addLinesCleared(linesClearedAtOnce);
    }

    public void addLinesCleared(int n) {
        linesClearedOnCurrentLevel += n;
        totalLinesCleared += n;
        if (linesClearedOnCurrentLevel > level * 10 + 10) {
            linesClearedOnCurrentLevel = 0;
            level++;
            ticksPerCell = getTicksPerCell();
        }
    }

    public boolean isLineFilled(int y) {
        for (int x = 0; x < WIDTH; x += UNIT_SIZE) {
            if (!squareExistsAt(x, y)) {
                return false;
            }
        }
        return true;
    }

    public void clearLine(int y) {
        for (int i = 0; i < inactiveSquares.size(); i++) {
            if (inactiveSquares.get(i).getYCoord() == y) {
                inactiveSquares.remove(i--);
            }
        }
        for (int i = 0; i < inactiveSquares.size(); i++) {
            if (inactiveSquares.get(i).getYCoord() < y) {
                inactiveSquares.get(i).moveDown();
            }
        }

        repaint();
    }

    public void addPoints(int n) {
        switch (n) {
            case 1:
                score += 40 * (level + 1);
                break;
            case 2:
                score += 100 * (level + 1);
                break;
            case 3:
                score += 300 * (level + 1);
                break;
            case 4:
                score += 1200 * (level + 1);
                break;
        }
    }

    public int getTicksPerCell() {
        if (level <= 8) {
            return 48 - level * 5;
        }
        if (level == 9) {
            return 6;
        }
        if (level <= 12) {
            return 5;
        }
        if (level <= 15) {
            return 4;
        }
        if (level <= 18) {
            return 3;
        }
        if (level <= 28) {
            return 2;
        }
        return 1;
    }

    public void checkForGameOver() {
        for (inactiveSquare s : inactiveSquares) {
            if (s.getYCoord() < 0) {
                gameOver();
                return;
            }
        }
    }

    public void gameOver() {
        isRunning = false;
        gameOver = true;
    }

    public boolean canLock() {
        return ticksPassed - lastMove > TICK_DELAY || moveResets >= 15;
    }

    public void resetGame() {
        inactiveSquares.clear();
        activeBlock = null;
        newBlock();
        level = 0;
        score = 0;
        totalLinesCleared = 0;
        linesClearedOnCurrentLevel = 0;
        gameOver = false;
        isRunning = true;
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == timer && isRunning) {
            if (ticksPassed % ticksPerCell == 0) {
                moveBlockDown();
            }
            ticksPassed++;
        }
    }

    public class MyKeyAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (isRunning) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if (canMoveLeft()) {
                            x -= UNIT_SIZE;
                            lastMove = ticksPassed;
                            moveResets++;
                        }
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (canMoveRight()) {
                            x += UNIT_SIZE;
                            lastMove = ticksPassed;
                            moveResets++;
                        }
                        break;
                    case KeyEvent.VK_DOWN:
                        moveResets = 16;
                        moveBlockDown();
                        break;
                    case KeyEvent.VK_UP:
                        rotate();
                        lastMove = ticksPassed;
                        moveResets++;
                        break;
                    case KeyEvent.VK_SPACE:
                        y += ghostYOffset;
                        newBlock();
                        break;
                }
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                if (gameOver) {
                    resetGame();
                } else {
                    isRunning = !isRunning;
                    isPaused = !isPaused;
                }

            }
            repaint();

        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.add(new Tetris());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("Tetris");
        frame.pack();
        frame.setResizable(false);
        frame.setVisible(true);
    }

}

class Square {

    double xOffset;
    double yOffset;
    Color color;

    public Square(double a, double b, Color c) {
        xOffset = a;
        yOffset = b;
        color = c;
    }

    public void setXOffset(double a) {
        xOffset = a;
    }

    public void setYOffset(double b) {
        yOffset = b;
    }

    public double getXOffset() {
        return xOffset;
    }

    public double getYOffset() {
        return yOffset;
    }

    public void draw(int a, int b, Graphics g) {
        g.setColor(color);
        g.fillRect((int) (xOffset * Tetris.UNIT_SIZE + a), (int) (yOffset * Tetris.UNIT_SIZE + b), Tetris.UNIT_SIZE, Tetris.UNIT_SIZE);
        g.setColor(color.black);
        g.drawRect((int) (xOffset * Tetris.UNIT_SIZE + a), (int) (yOffset * Tetris.UNIT_SIZE + b), Tetris.UNIT_SIZE, Tetris.UNIT_SIZE);
    }

    public void drawOutline(int a, int b, Graphics g) {
        g.setColor(color);
        g.drawRect((int) (xOffset * Tetris.UNIT_SIZE + a), (int) (yOffset * Tetris.UNIT_SIZE + b), Tetris.UNIT_SIZE, Tetris.UNIT_SIZE);
    }
}

class inactiveSquare {

    int x;
    int y;
    Color color;

    public inactiveSquare(int a, int b, Color c) {
        x = a;
        y = b;
        color = c;
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, y, Tetris.UNIT_SIZE, Tetris.UNIT_SIZE);
        g.setColor(color.black);
        g.drawRect(x, y, Tetris.UNIT_SIZE, Tetris.UNIT_SIZE);
    }

    public int getXCoord() {
        return x;
    }

    public int getYCoord() {
        return y;
    }

    public void moveDown() {
        y += Tetris.UNIT_SIZE;
    }
}

class Block {

    Color color;
    List<Square> squares;

    public Block(Color c) {
        color = c;
    }

    public void setSquares(List<Square> s) {
        squares = s;
    }

    public List<Square> getSquares() {
        return squares;
    }

    public Color getColor() {
        return color;
    }

    public void draw(int x, int y, Graphics g) {
        for (Square s : squares) {
            s.draw(x, y, g);
        }
    }

    public void drawOutline(int x, int y, Graphics g) {
        for (Square s : squares) {
            s.drawOutline(x, y, g);
        }
    }

    public void rotate() {
        for (Square s : squares) {
            double temp = s.getXOffset();
            s.setXOffset(-1 * s.getYOffset());
            s.setYOffset(temp);
        }
    }

    public boolean isRotatedAroundCenter() {
        return true;
    }
}

class OffCenterBlock extends Block {

    public OffCenterBlock(Color c) {
        super(c);
    }

    public void draw(int x, int y, Graphics g) {
        for (Square s : squares) {
            s.draw((int) (x + 0.5 * Tetris.UNIT_SIZE), (int) (y + 0.5 * Tetris.UNIT_SIZE), g);
        }
    }

    public void drawOutline(int x, int y, Graphics g) {
        for (Square s : squares) {
            s.drawOutline((int) (x + 0.5 * Tetris.UNIT_SIZE), (int) (y + 0.5 * Tetris.UNIT_SIZE), g);
        }
    }

    public boolean isRotatedAroundCenter() {
        return false;
    }

}

class I extends OffCenterBlock {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1.5, -0.5, getColor()),
            new Square(-0.5, -0.5, getColor()),
            new Square(0.5, -0.5, getColor()),
            new Square(1.5, -0.5, getColor())
    );

    public I() {
        super(new Color(49, 199, 239));
        super.setSquares(new ArrayList<Square>(defaultSquares));
    }
}

class J extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1, 0, getColor()),
            new Square(-1, -1, getColor()),
            new Square(0, 0, getColor()),
            new Square(1, 0, getColor())
    );

    public J() {
        super(new Color(88, 102, 175));
        super.setSquares(defaultSquares);
    }

}

class L extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1, 0, getColor()),
            new Square(1, -1, getColor()),
            new Square(0, 0, getColor()),
            new Square(1, 0, getColor())
    );

    public L() {
        super(new Color(239, 121, 34));
        super.setSquares(defaultSquares);
    }

}

class O extends OffCenterBlock {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-0.5, -0.5, getColor()),
            new Square(-0.5, 0.5, getColor()),
            new Square(0.5, -0.5, getColor()),
            new Square(0.5, 0.5, getColor())
    );

    public O() {
        super(new Color(246, 212, 8));
        super.setSquares(defaultSquares);
    }

}

class S extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1, 0, getColor()),
            new Square(0, 0, getColor()),
            new Square(0, -1, getColor()),
            new Square(1, -1, getColor())
    );

    public S() {
        super(new Color(64, 183, 63));
        super.setSquares(defaultSquares);
    }

}

class T extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1, 0, getColor()),
            new Square(0, 0, getColor()),
            new Square(0, -1, getColor()),
            new Square(1, 0, getColor())
    );

    public T() {
        super(new Color(173, 77, 156));
        super.setSquares(defaultSquares);
    }

}

class Z extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-1, -1, getColor()),
            new Square(0, 0, getColor()),
            new Square(0, -1, getColor()),
            new Square(1, 0, getColor())
    );

    public Z() {
        super(new Color(239, 32, 41));
        super.setSquares(defaultSquares);
    }

}

class Cock extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-5, 7, getColor()),
            new Square(-5, 6, getColor()),
            new Square(-5, 5, getColor()),
            new Square(-4, 8, getColor()),
            new Square(-4, 4, getColor()),
            new Square(-3, 8, getColor()),
            new Square(-3, 3, getColor()),
            new Square(-2, 8, getColor()),
            new Square(-2, 2, getColor()),
            new Square(-2, 1, getColor()),
            new Square(-2, 0, getColor()),
            new Square(-2, -1, getColor()),
            new Square(-2, -2, getColor()),
            new Square(-2, -3, getColor()),
            new Square(-2, -4, getColor()),
            new Square(-2, -5, getColor()),
            new Square(-2, -6, getColor()),
            new Square(-2, -7, getColor()),
            new Square(-1, 8, getColor()),
            new Square(-1, -5, getColor()),
            new Square(-1, -8, getColor()),
            new Square(0, 7, getColor()),
            new Square(0, -5, getColor()),
            new Square(0, -7, getColor()),
            new Square(0, -8, getColor()),
            new Square(5, 7, getColor()),
            new Square(5, 6, getColor()),
            new Square(5, 5, getColor()),
            new Square(4, 8, getColor()),
            new Square(4, 4, getColor()),
            new Square(3, 8, getColor()),
            new Square(3, 3, getColor()),
            new Square(2, 8, getColor()),
            new Square(2, 2, getColor()),
            new Square(2, 1, getColor()),
            new Square(2, 0, getColor()),
            new Square(2, -1, getColor()),
            new Square(2, -2, getColor()),
            new Square(2, -3, getColor()),
            new Square(2, -4, getColor()),
            new Square(2, -5, getColor()),
            new Square(2, -6, getColor()),
            new Square(2, -7, getColor()),
            new Square(1, 8, getColor()),
            new Square(1, -5, getColor()),
            new Square(1, -8, getColor())
    );

    public Cock() {
        super(new Color(239, 32, 41));
        super.setSquares(defaultSquares);
    }

}

class Triangle extends Block {

    private List<Square> defaultSquares = Arrays.asList(
            new Square(-2, 1, getColor()),
            new Square(-1, 1, getColor()),
            new Square(-1, 0, getColor()),
            new Square(0, 0, getColor()),
            new Square(0, -1, getColor()),
            new Square(1, 0, getColor()),
            new Square(0, 1, getColor()),
            new Square(1, 1, getColor()),
            new Square(2, 1, getColor())
    );

    public Triangle() {
        super(new Color(239, 32, 41));
        super.setSquares(defaultSquares);
    }

}
