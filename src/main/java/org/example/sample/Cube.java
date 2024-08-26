package org.example.sample;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;

public class Cube implements GLEventListener {

    private double theta = 5.0;    //Cubes' rotation speeds used in update()
    private double toto = 3.0;
    private double zero = 7.0;

    // angle of rotation for the camera direction
    float angle = 0.0f;
    // actual vector representing the camera's direction
    float lx = 0.0f, lz = -1.0f;
    // XZ position of the camera
    float x = 0.0f, z = 0.0f;

    @Override
    public void display(GLAutoDrawable dr) {
        // TODO Auto-generated method stub
        GL2 gl = dr.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        update();
        render(dr);
    }

    @Override
    public void dispose(GLAutoDrawable arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void init(GLAutoDrawable dr) {
        // TODO Auto-generated method stub
        GL2 gl = dr.getGL().getGL2();
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearDepth(1.0f);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
    }

    //@Override

    /**
     * Used of gluLookAt on every reshape
     */
    public void reshape(GLAutoDrawable dr, int arg1, int arg2, int width, int height) {
        // TODO Auto-generated method stub
        GL2 gl = dr.getGL().getGL2();

        gl.glLoadIdentity();

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        (new GLU()).gluPerspective(45.0f, (float) width / (float) height, 0.1f, 10f);
        gl.glLoadIdentity();
        (new GLU()).gluLookAt(x, 0.0f, z,
            x + lx, 0.0f, z + lz,
            0.0f, 1.0f, 0.0f
        );
    }

    /**
     * Allow to change the angle of the "camera" by using the arrow keys. Left and Right: ok / Up
     * and Down: not ok
     *
     * @param keyCode
     */
    public void handleKeyPress(int keyCode) {
        float fraction = 0.1f;
        switch (keyCode) {
            case 37://LEFT KEY
                angle -= 0.01f;
                lx = (float) Math.sin(angle);
                lz = (float) -Math.cos(angle);
                break;
            case 38://UP KEY
                x += lx * fraction;
                z += lz * fraction;
                break;
            case 39://RIGHT KEY
                angle += 0.01f;
                lx = (float) Math.sin(angle);
                lz = (float) -Math.cos(angle);
                break;
            case 40://DOWN KEY
                x -= lx * fraction;
                z -= lz * fraction;
                break;
        }
    }

    public void update() {
        zero += 7.0;
        theta += 5.0;
        toto += 3.0;
    }

    private void render(GLAutoDrawable dr) {
        GL2 gl = dr.getGL().getGL2();
        float ud = (float) 0.1;

        //First Cube, central
        gl.glPushMatrix();
        gl.glRotatef((float) theta, 0.0f, -1.0f, 0.0f);       //right Rotation

        gl.glBegin(GL2.GL_QUADS);
        // Front
        gl.glColor3f(0f, 1f, 1f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(+ud, -ud, +ud);

        // Back
        gl.glColor3f(0.5f, 1f, 0.5f);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);

        // Left & Right
        gl.glColor3f(1f, 1f, 0f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, +ud);

        gl.glColor3f(1f, 0f, 0f);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(-ud, -ud, +ud);

        // Top & Bottom
        gl.glColor3f(1f, 0f, 1f);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);

        gl.glColor3f(0f, 0f, 1f);
        gl.glVertex3f(+ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glEnd();
        gl.glPopMatrix();
        //End First cube

        //second cube
        gl.glPushMatrix();
        gl.glTranslated(0, 0.4, 0);
        gl.glRotatef((float) zero, -1.0f, 0.0f, 0.0f);        //Frontward Rotation
        gl.glBegin(GL2.GL_QUADS);
        // Front
        gl.glColor3f(0f, 1f, 1f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(+ud, -ud, +ud);

        // Back
        gl.glColor3f(0.5f, 1f, 0.5f);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);

        // Left & Right
        gl.glColor3f(1f, 1f, 0f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, +ud);

        gl.glColor3f(1f, 0f, 0f);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(-ud, -ud, +ud);

        // Top & Bottom
        gl.glColor3f(1f, 0f, 1f);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);

        gl.glColor3f(0f, 0f, 1f);
        gl.glVertex3f(+ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glEnd();
        gl.glPopMatrix();
        //End second cube

        //Third Cube
        gl.glPushMatrix();
        gl.glRotatef((float) toto, 1.0f, 0.0f, 0.0f);    //Rotate above the two other cubes
        gl.glTranslated(0, 0.7, 0);
        gl.glBegin(GL2.GL_QUADS);
        // Front
        gl.glColor3f(0f, 1f, 1f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(+ud, -ud, +ud);

        // Back
        gl.glColor3f(0.5f, 1f, 0.5f);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);

        // Left & Right
        gl.glColor3f(1f, 1f, 0f);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, +ud);

        gl.glColor3f(1f, 0f, 0f);
        gl.glVertex3f(-ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(-ud, -ud, +ud);

        // Top & Bottom
        gl.glColor3f(1f, 0f, 1f);
        gl.glVertex3f(-ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, -ud);
        gl.glVertex3f(+ud, +ud, +ud);
        gl.glVertex3f(-ud, +ud, +ud);

        gl.glColor3f(0f, 0f, 1f);
        gl.glVertex3f(+ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, +ud);
        gl.glVertex3f(-ud, -ud, -ud);
        gl.glVertex3f(+ud, -ud, -ud);
        gl.glEnd();
        gl.glPopMatrix();
        //End Third cube
    }
}
