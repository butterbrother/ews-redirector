package com.github.butterbrother.ews.redirector;

import com.github.butterbrother.ews.redirector.graphics.SettingsWindow;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Запуск приложения начинается здесь.
 * Осуществляет загрузку иконки в трее, загружает настройки приложения.
 */
public class Loader {

    public static void main(String args[]) {
        try {
            LogManager.getLogManager().readConfiguration(Loader.class.getResourceAsStream("/logger.properties"));
        } catch (IOException warn) {
            System.err.println("WARNING: config file not found");
        }

        Logger logger = Logger.getLogger("Global");

        // Пытаемся задействовать системный внешний вид. Linux - GTK+, Windows - нативный вид
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } else if (System.getProperty("os.name").toLowerCase().contains("lin")) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
            } else {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            }
        } catch (Exception ignore) {
            logger.severe(ignore.getMessage());
            logger.severe(ExceptionUtils.getStackTrace(ignore));
        }

        try {
            TrayControl trayControl = new TrayControl();
            Settings config = new Settings(trayControl.getTrayPopup());
            final SettingsWindow win = new SettingsWindow(config, trayControl.getTrayPopup());

            trayControl.setIconDoubleClickListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() >= 2)
                        win.showSettingsWin();
                }
            });

            trayControl.setSettingsWindow(win);

        } catch (AWTException e) {
            logger.severe(e.getMessage());
            logger.severe(ExceptionUtils.getStackTrace(e));
        }
    }
}
