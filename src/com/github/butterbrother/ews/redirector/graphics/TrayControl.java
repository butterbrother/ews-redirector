package com.github.butterbrother.ews.redirector.graphics;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;

/**
 * Осуществляет загрузку иконки в системный трей.
 * Создаёт меню для трея.
 */
public class TrayControl {
    private SystemTray systemTray;
    private TrayIcon icon;
    private JMenuItem exitItem = new JMenuItem("Exit");
    private JMenuItem configItem = new JMenuItem("Settings...");

    public TrayControl() throws AWTException {
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported");
            System.exit(1);
        }
        systemTray = SystemTray.getSystemTray();
        icon = new TrayIcon(getTrayImage(), "EWS redirector");
        createTrayPopupMenu();
        systemTray.add(icon);
    }

    /**
     * Добавляет слушателя для кнопки конфигурации
     *
     * @param listener Слушатель кнопки конфигурации
     */
    public void setConfigListener(MouseListener listener) {
        configItem.addMouseListener(listener);
    }

    /**
     * Добавляет слушателя для иконки.
     * В нашем случае нужно для открытия окна настроек
     * при двойном щелчке по иконке в трее.
     *
     * @param listener Слушатель иконки
     */
    public void setIconDoubleClickListener(MouseListener listener) {
        icon.addMouseListener(listener);
    }

    /**
     * Добавляет слушателя для кнопки завершения работы приложения.
     *
     * @param listener Слушатель кнопки
     */
    public void setCloseListener(MouseListener listener) {
        exitItem.addMouseListener(listener);
    }

    /**
     * Находит подходящую картинку для трея исходя из его размера.
     * Если картинка не будет найдена - вернёт самую большую.
     * Картинки должны существовать в jar-файле, в images.
     * Разрешения: 16, 22, 24, 32, 48 и 64.
     * Формат: png
     *
     * @return Картинка из jar.
     */
    private Image getTrayImage() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        ClassLoader loader = this.getClass().getClassLoader();

        Dimension iconSize = systemTray.getTrayIconSize();
        URL iconPath = loader.getResource("icons/" + (int) iconSize.getHeight() + ".png");
        if (iconPath == null)
            iconPath = loader.getResource("icons/64.png");

        return toolkit.getImage(iconPath);
    }

    /**
     * Создаёт контекстное меню трея
     */
    private void createTrayPopupMenu() {
        final JPopupMenu menu = new JPopupMenu();
        JMenuItem header = new JMenuItem("EWS redirector");
        header.setEnabled(false);
        menu.add(header);
        menu.addSeparator();
        menu.add(configItem);

        menu.add(exitItem);
        // Костыль. Трей не умеет JPopupMenu
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if ((e.getButton() & MouseEvent.BUTTON2) == MouseEvent.BUTTON2) {
                    menu.setLocation(e.getX(), e.getY());
                    menu.setInvoker(menu);
                    menu.setVisible(true);
                }
            }
        });
    }

    /**
     * Показывает сообщение из трея. Проброс из специального класса,
     * что бы не управлять треем напрямую.
     * @param caption   заголовок
     * @param text      текст сообщения
     * @param type      тип сообщения
     */
    private void showPopup(String caption, String text, TrayIcon.MessageType type) {
        icon.displayMessage(caption, text, type);
        System.out.println("DEBUG: Tray control, show popup: [" + caption + "] " + text);
    }

    /**
     * Класс для отображения нотификаций в трее
     */
    public class TrayPopup {
        /**
         * Показывает ошибку из трея
         * @param caption   заголовок сообщения
         * @param text      текст сообщения
         */
        public void error(String caption, String text) {
            showPopup(caption, text, TrayIcon.MessageType.ERROR);
        }
    }

    public TrayPopup getTrayPopup() {
        return new TrayPopup();
    }
}
