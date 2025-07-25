import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseInputListener;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ActionEvent;

public class ClickRepeater extends JFrame implements NativeMouseInputListener, NativeKeyListener {

    private Robot robot;
    private volatile boolean mirroringEnabled = false;
    private volatile boolean isLeftClickMirroring = false;
    private volatile boolean isRightClickMirroring = false;

    private JLabel statusLabel;
    private JTextField delayField, keyField, clickCountField, interClickDelayField;
    private JCheckBox keyToggle, leftEnable, rightEnable;

    public ClickRepeater() {
        super("Click Mirror GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 320);
        setLayout(new GridLayout(8, 2, 6, 6));
        setLocationRelativeTo(null);

        statusLabel = new JLabel("OFF", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(new JLabel("Mirroring Status:", SwingConstants.RIGHT));
        add(statusLabel);

        delayField = new JTextField("10");
        clickCountField = new JTextField("1");
        interClickDelayField = new JTextField("5");
        keyField = new JTextField("F8");

        keyToggle = new JCheckBox("Enable key toggle", true);
        leftEnable = new JCheckBox("Enable Left Click", true);
        rightEnable = new JCheckBox("Enable Right Click", true);

        add(new JLabel("Initial Delay (ms):", SwingConstants.RIGHT));
        add(delayField);

        add(new JLabel("Clicks per trigger (shared):", SwingConstants.RIGHT));
        add(clickCountField);

        add(new JLabel("Delay between clicks (ms):", SwingConstants.RIGHT));
        add(interClickDelayField);

        add(new JLabel("Activation Key:", SwingConstants.RIGHT));
        add(keyField);

        add(keyToggle);
        add(new JPanel()); // spacer
        add(leftEnable);
        add(rightEnable);

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");

        startButton.addActionListener((ActionEvent e) -> {
            mirroringEnabled = true;
            updateStatus();
        });

        stopButton.addActionListener((ActionEvent e) -> {
            mirroringEnabled = false;
            updateStatus();
            System.out.println("🛑 Burst interrupted manually.");
        });

        add(startButton);
        add(stopButton);

        setVisible(true);

        try {
            robot = new Robot();
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeMouseListener(this);
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException | AWTException e) {
            e.printStackTrace();
        }
    }

    private void updateStatus() {
        statusLabel.setText(mirroringEnabled ? "ON" : "OFF");
    }

    private int parseField(JTextField field, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(field.getText().trim()));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void mirrorClick(int buttonMask, int count, int initialDelay, int interClickDelay, Runnable afterBurst) {
        new Thread(() -> {
            robot.delay(initialDelay);
            for (int i = 0; i < count; i++) {
                if (!mirroringEnabled) {
                    System.out.println("⛔ Burst interrupted mid-loop.");
                    break;
                }
                robot.mousePress(buttonMask);
                robot.mouseRelease(buttonMask);
                if (i < count - 1) robot.delay(interClickDelay);
            }
            if (afterBurst != null) afterBurst.run();
        }).start();
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {
        if (!mirroringEnabled) return;

        int initialDelay = parseField(delayField, 10);
        int interClickDelay = parseField(interClickDelayField, 5);
        int clickCount = parseField(clickCountField, 1);

        switch (e.getButton()) {
            case 1:
                if (!leftEnable.isSelected() || isLeftClickMirroring) return;
                isLeftClickMirroring = true;
                mirrorClick(InputEvent.BUTTON1_DOWN_MASK, clickCount, initialDelay, interClickDelay,
                        () -> isLeftClickMirroring = false);
                break;

            case 2:
                if (!rightEnable.isSelected() || isRightClickMirroring) return;
                isRightClickMirroring = true;
                mirrorClick(InputEvent.BUTTON3_DOWN_MASK, clickCount, initialDelay, interClickDelay,
                        () -> isRightClickMirroring = false);
                break;
        }
    }

    @Override public void nativeMouseReleased(NativeMouseEvent e) {}
    @Override public void nativeMouseClicked(NativeMouseEvent e) {}

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (!keyToggle.isSelected()) return;
        if (NativeKeyEvent.getKeyText(e.getKeyCode()).equalsIgnoreCase(keyField.getText().trim())) {
            mirroringEnabled = !mirroringEnabled;
            updateStatus();
            System.out.println("🧠 Mirroring toggled via key.");
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClickRepeater::new);
    }
}