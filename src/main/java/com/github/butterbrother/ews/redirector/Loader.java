package com.github.butterbrother.ews.redirector;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.SettingsWindow;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import com.github.butterbrother.ews.redirector.service.LogNotificator;
import com.github.butterbrother.ews.redirector.service.Notificator;
import com.github.butterbrother.ews.redirector.service.ServiceController;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.github.butterbrother.ews.redirector.graphics.SettingsWindow.*;

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

        if (args.length > 0) {
            Notificator notificator = new LogNotificator();
            String configPath = args[0];
            Settings settings = new Settings(configPath, notificator);
            List<MailFilter> filters = settings.getAllFilters(SettingsWindow.FILTERS);
            MailFilter[] mailFilters = new MailFilter[filters.size()];
            filters.toArray(mailFilters);

            Consumer<Boolean> onStart = a -> logger.info("Service started");
            Consumer<Boolean> onStop = isBefore -> {
                if (isBefore) {
                    logger.info("Service stopping");
                } else {
                    logger.info("Service stopped");
                }
            };
            Consumer<String> onUrlChange = url -> logger.info("Autodiscower URL: " + url);

            ServiceController controller = new ServiceController(
                    settings.getString(DOMAIN),
                    settings.getString(EMAIL),
                    settings.getString(RECIPIENT_EMAIL),
                    settings.getString(LOGIN),
                    settings.getString(PASSWORD),
                    settings.getString(AUTO_DISCOVER_URL),
                    settings.getBoolean(AUTO_DISCOVER_ENABLED),
                    notificator,
                    onStart,
                    onStop,
                    onUrlChange,
                    settings.getBoolean(DELETE_REDIRECTED),
                    mailFilters);

            try {
                controller.join();
            } catch (InterruptedException warn) {
                logger.severe(warn.getMessage());
            }

        } else {

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
                Settings config = new Settings(null, trayControl.getTrayPopup());
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
}
