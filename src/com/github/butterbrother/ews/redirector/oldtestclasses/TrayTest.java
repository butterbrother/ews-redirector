package com.github.butterbrother.ews.redirector.oldtestclasses;

import java.awt.*;

/**
 * <EMPTY HEADER, PLEASE EDIT>
 * Created by user on 16.01.16.
 */
public class TrayTest {
    public static void main(String args[]) {
        if (SystemTray.isSupported()) {
            System.out.println("Systray is supported");
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Image image = toolkit.getImage("/usr/share/icons/hicolor/14x14/apps/utox.png");
            TrayIcon icon = new TrayIcon(image, "Tray test");
            icon.setImageAutoSize(true);
            SystemTray tray = SystemTray.getSystemTray();
            Dimension size = tray.getTrayIconSize();
            System.out.println("Tray icon size: " + size.getHeight() + "x" + size.getWidth());
            try {
                tray.add(icon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Systray is not supported");
        }
    }
}
