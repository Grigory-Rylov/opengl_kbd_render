package org.example;

import com.github.grishberg.cad3d.keyboard.ControlPointsController;
import com.github.grishberg.cad3d.keyboard.KeyPlace;
import com.github.grishberg.cad3d.keyboard.cfg.KeyOffsetProvider;
import com.github.grishberg.cad3d.keyboard.cfg.KeyZAngleProvider;
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import com.github.grishberg.cad3d.keyboard.cfg.PowerSwitcherType;
import com.github.grishberg.cad3d.util.SceneBuilder;
import com.github.grishberg.cad3d.util.SceneBuilderKeyboard;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.Vec2i;
import com.jogamp.opengl.util.Animator;
import eu.printingin3d.javascad.vrl.VertexHolder;
import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;


public class Main implements GLEventListener, MouseListener, MouseMotionListener, KeyListener {

    private static final boolean DEBUG = true;
    private static final double LOW_PROFILE_KEYCAP_HEIGHT = 4.5;
    private static final double STANDART_KEYCAP_HEIGHT = 12.7;
    protected GLWindow window;
    protected Animator animator;
    protected Vec2i windowSize = new Vec2i(800, 600);
    private float scaleFactor = 1.0f;
    private float xOffset = 0.0f;
    private static final float SCALE_MULT = 0.001f;
    private static final float X_OFFSET_MULT = 0.001f;
    private static final float ZOOM_SENSITIVITY = 5.0f;
    private static final float ZOOM_MIN_OFFSET = -1200.0f;
    private static final float ZOOM_MAX_OFFSET = 0.0f;

    //public Caps caps;
    // Установка позиции источника света
    float[] lightPosition = {0.0f, 1f, 0.5f, 1.0f};

    private final int rowsCount = 3;
    private final int colsCount = 6;
    private final KeyboardConfig cfg = new KeyboardConfig(
        60, // fn
        10.0, // plateZOffset
        20.1, // rowCurvature
        7, // tentingAngle
        15.1, // columnCurvature
        14.2, // keyswitchHeight
        14.2, // keyswitchWidth
        2.5, // controls overall height; original=9 with centercol=3; use 16 for centercol=2
        1.0, // extra space between the base of keys; original= 2
        3, // plateThickness
        LOW_PROFILE_KEYCAP_HEIGHT, //
        2, // centerCol
        rowsCount, // rowsCount
        colsCount, // colsCount
        rowsCount - 2, // centerRow
        15.7, // keyPlaceHolderWidth
        15.7, // keyPlaceHolderDepth
        4.0, // keyPlaceHolderHeight
        true, // isLowProfile
        new KeyZAngleProvider(),
        new KeyOffsetProvider(),
        PowerSwitcherType.None,
        true, // hasHotswap
        false // magneticWristRestHolder
    );

    private List<VertexHolder> vertexHolderList = new ArrayList<>();

    public static DisplayMode dm, dm_old;
    private GLU glu = new GLU();
    private float rotateX = -55.0f;
    private float rotateY = 0.0f;
    private float rotateZ = 0.0f;
    private int prevMouseX;
    private int prevMouseY;

    private boolean isControlPressed = false; // Добавляем флаг
    private static final float MOUSE_TRANSLATE_SENSITIVITY = 0.5f; // Чувствительность смещения

    private float translateX = 0.0f; // Смещение по X
    private float translateY = 0.0f; // Смещение по Y

    private float translateZ = -300.0f; // Смещение по Y
    private static final float TRANSLATE_STEP = 5.0f; // Шаг смещения

    private final KeyPlace keyPlace = new KeyPlace(cfg);
    private ControlPointsController pointsController = new ControlPointsController(cfg, keyPlace);

    public Main() {

        SceneBuilder builder = new SceneBuilderKeyboard(cfg, keyPlace, pointsController);

        builder.setListener(buffers -> {
            vertexHolderList.clear();
            vertexHolderList.addAll(buffers);
            //TODO invalidate
        });
        builder.requestBuffers();
        setup("test");
    }

    public void setup(String title) {

        GLProfile glProfile = GLProfile.get(GLProfile.GL2);
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        if (DEBUG) {
            window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        }

        window.setUndecorated(false);
        window.setAlwaysOnTop(false);
        window.setFullscreen(false);
        window.setPointerVisible(true);
        window.confinePointer(false);
        window.setTitle(title);
        window.setSize(windowSize.x(), windowSize.y());

        window.setVisible(true);

        window.addGLEventListener(this);
        window.addKeyListener(this);
        window.addMouseListener(this);

        animator = new Animator();
        animator.add(window);
        animator.start();

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                new Thread(new Runnable() {
                    public void run() {

                        //stop the animator thread when user close the window
                        animator.stop();
                        // This is actually redundant since the JVM will terminate when all
                        // threads are closed.
                        // It's useful just in case you create a thread and you forget to stop it.
                        System.exit(1);
                    }
                }).start();
            }
        });
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        // Установка материала для куба
        float[] materialDiffuse = {0.7f, 0.7f, 0.7f, 1.0f};
        gl.glMaterialfv(GL2.GL_FRONT, GLLightingFunc.GL_DIFFUSE, materialDiffuse, 0);

        // Перемещение куба в нужное место
        gl.glTranslatef(translateX + xOffset, translateY, translateZ);
        //gl.glTranslatef(xOffset, 0.0f, -300.0f);

        // Применение вращения только к кубу
        gl.glPushMatrix();
        gl.glScalef(scaleFactor, scaleFactor, scaleFactor);
        gl.glRotatef(rotateX, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(rotateY, 0.0f, 1.0f, 0.0f);
        gl.glRotatef(rotateZ, 0.0f, 0.0f, 1.0f);

        for (VertexHolder vertexHolder : vertexHolderList) {
            // Отрисовка куба
            gl.glBegin(GL2.GL_TRIANGLES);

            int normalArrayIndex = 0;
            int vertexArrayIndex = 0;
            float vert[] = vertexHolder.getVertex();
            float normals[] = vertexHolder.getNormals();
            for (int i = 0; i < vertexHolder.getVerticesCount(); i++) {
                float x = vert[vertexArrayIndex++];
                float y = vert[vertexArrayIndex++];
                float z = vert[vertexArrayIndex++];

                gl.glColor4f(
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++]
                );
                gl.glNormal3f(
                    normals[normalArrayIndex++],
                    normals[normalArrayIndex++],
                    normals[normalArrayIndex++]
                );
                gl.glVertex3f(x, y, z);
            }
            gl.glEnd();
        }

        gl.glPopMatrix(); // Возвращаемся к исходной матрице

        gl.glFlush();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO Auto-generated method stub
    }


    private void initializeProgram(GL2 gl) {
        String vertexShaderSource = ShaderLoader.loadShader("/shader_vertex_104.txt");
        String fragmentShaderSource = ShaderLoader.loadShader("/shader_fragment_104.txt");

        ShaderProgram shaderProgram =
            new ShaderProgram(gl, vertexShaderSource, fragmentShaderSource);
        shaderProgram.use(gl);
        int error = gl.glGetError();
        if (error != 0) {
            System.out.println("Error while setting shaders : " + error);
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        init(gl);
    }

    protected void init(GL2 gl) {
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0f, 0f, 0f, 0f);
        gl.glClearDepth(1.0f);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        // Включение освещения
        gl.glEnable(GLLightingFunc.GL_LIGHTING);
        gl.glEnable(GLLightingFunc.GL_LIGHT0);
        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);

        // Настройка света
        float[] lightAmbient = {0.2f, 0.2f, 0.2f, 1.0f};
        float[] lightDiffuse = {1.0f, 1.0f, 1.0f, 1.0f};

        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, lightAmbient, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, lightDiffuse, 0);
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0);

        initializeProgram(gl);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        // TODO Auto-generated method stub
        final GL2 gl = drawable.getGL().getGL2();
        if (height <= 0) {
            height = 1;
        }

        final float h = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        float aspect = (float) width / height;
        float fov = 45.0f;
        float near = 0.1f;
        float far = 1400.0f;
        glu.gluPerspective(fov, aspect, near, far);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public static void main(String[] args) {
        new Main();
    }

    // ------------------------------------------------

    ////


    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        prevMouseX = mouseEvent.getX();
        prevMouseY = mouseEvent.getY();
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        int currentMouseX = e.getX();
        int currentMouseY = e.getY();

        int deltaX = currentMouseX - prevMouseX;
        int deltaY = currentMouseY - prevMouseY;

        if (isControlPressed) {
            // Смещение объекта при зажатом Control
            translateX += deltaX * MOUSE_TRANSLATE_SENSITIVITY;
            translateY -= deltaY * MOUSE_TRANSLATE_SENSITIVITY;
        } else {
            rotateX += deltaY;
            rotateZ += deltaX;
        }

        prevMouseX = currentMouseX;
        prevMouseY = currentMouseY;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        float[] rotation = e.getRotation();
        float dx = rotation[0];
        float dy = rotation[1];

        //scaleFactor += dy * SCALE_MULT;
        translateZ += dy * ZOOM_SENSITIVITY;
        xOffset += dx * X_OFFSET_MULT;

        // Ограничиваем диапазон значений (опционально)
        translateZ = Math.max(ZOOM_MIN_OFFSET, Math.min(translateZ, ZOOM_MAX_OFFSET));
        // Don't let the object get too small or too large.
        //scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        int currentMouseX = e.getX();
        int currentMouseY = e.getY();

        int deltaX = currentMouseX - prevMouseX;
        int deltaY = currentMouseY - prevMouseY;

        if (isControlPressed) {
            // Смещение объекта при зажатом Control
            translateX += deltaX * MOUSE_TRANSLATE_SENSITIVITY;
            translateY -= deltaY * MOUSE_TRANSLATE_SENSITIVITY;
        } else {
            rotateX += deltaY;
            rotateY += deltaX;
        }

        prevMouseX = currentMouseX;
        prevMouseY = currentMouseY;
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        switch (keyCode) {
            case KeyEvent.VK_CONTROL: // Отслеживаем нажатие Control
                isControlPressed = true;
                break;
            case KeyEvent.VK_A:      // Влево
            case KeyEvent.VK_LEFT:
                translateX -= TRANSLATE_STEP;
                break;
            case KeyEvent.VK_D:      // Вправо
            case KeyEvent.VK_RIGHT:
                translateX += TRANSLATE_STEP;
                break;
            case KeyEvent.VK_W:      // Вверх
            case KeyEvent.VK_UP:
                translateY += TRANSLATE_STEP;
                break;
            case KeyEvent.VK_S:      // Вниз
            case KeyEvent.VK_DOWN:
                translateY -= TRANSLATE_STEP;
                break;
        }
        //window.reparentWindow(); // Обновляем отображение
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_CONTROL) { // Отслеживаем отпускание Control
            isControlPressed = false;
        }
    }
}
