package org.example.sample;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Jogl {

    public static void main(String[] args) {
        GLProfile glp = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities(glp);
        GLCanvas canvas = new GLCanvas(caps);
        Cube unCube = new Cube();
        canvas.addGLEventListener(unCube);
        Frame frame = new Frame("AWT Window Test");
        FPSAnimator anim = new FPSAnimator(canvas, 60);
        anim.start();
        frame.setSize(400, 400);
        frame.add(canvas);
        frame.setVisible(true);

        frame.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                // TODO Auto-generated method stub
                int keyCode = e.getKeyCode();
                unCube.handleKeyPress(keyCode);        //Here is the handleKeyPress function's call
                frame.add(canvas);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void keyTyped(KeyEvent e) {
                // TODO Auto-generated method stub

            }

        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
