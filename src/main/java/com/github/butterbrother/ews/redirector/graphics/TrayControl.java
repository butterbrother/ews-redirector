package com.github.butterbrother.ews.redirector.graphics;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.net.URL;

/**
 * Осуществляет загрузку иконки в системный трей.
 * Создаёт меню для трея.
 */
public class TrayControl {
    private SystemTray systemTray;
    private TrayIcon icon;
    private MenuItem exitItem = new MenuItem("Exit");
    private MenuItem configItem = new MenuItem("Settings...");
    private SettingsWindow win = null;

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
     * Передает экземпляр окна конфигуратора.
     * <p>
     * Передаёт экземпляр окна конфигуратора {@link SettingsWindow} для возможности его открытия,
     * сохранения конфигурации перед выходом и т.п.
     *
     * @param win экземпляр окна конфигуратора
     */
    public void setSettingsWindow(SettingsWindow win) {
        this.win = win;
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
        final PopupMenu menu = new PopupMenu();

        MenuItem header = new MenuItem("EWS redirector (v0.2.2)");
        header.setEnabled(false);
        menu.add(header);
        menu.addSeparator();

        configItem.setActionCommand("config");
        menu.add(configItem);

        exitItem.setActionCommand("exit");
        menu.add(exitItem);

        icon.setPopupMenu(menu);

        ActionListener menuListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (e.getActionCommand()) {
                    case "config":
                        win.showSettingsWin();
                        break;

                    case "exit":
                        win.saveWindowPos();
                        System.exit(0);
                        break;
                }
            }
        };

        configItem.addActionListener(menuListener);
        exitItem.addActionListener(menuListener);

        /*
        // Костыль. Трей не умеет JPopupMenu
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if ((e.getButton() & MouseEvent.BUTTON2) == MouseEvent.BUTTON2 && e.isPopupTrigger()) {
                    menu.setLocation(e.getX(), e.getY());
                    hiddenDialog.setLocation(e.getX(), e.getY());
                    //menu.setInvoker(menu);

                    //if (!hiddenDialog.isUndecorated()) hiddenDialog.setUndecorated(true);
                    hiddenDialog.setVisible(true);
                    menu.setInvoker(hiddenDialog);
                    menu.setVisible(true);
                }
            }
        });

        // Ещё один костыль, на этот раз для того, что бы прятать меню при снятии фокуса
        hiddenDialog.addFocusListener(new FocusAdapter() {
                                          @Override
                                          public void focusLost(FocusEvent e) {
                                              menu.setVisible(false);
                                              hiddenDialog.setVisible(false);
                                          }
          });

        menu.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                hiddenDialog.setVisible(false);
            }
        });
        */

    }

    /**
     * Показывает сообщение из трея. Проброс из специального класса,
     * что бы не управлять треем напрямую.
     *
     * @param caption заголовок
     * @param text    текст сообщения
     * @param type    тип сообщения
     */
    private void showPopup(String caption, String text, TrayIcon.MessageType type) {
        icon.displayMessage(caption, text, type);
        System.out.println("DEBUG: Tray control, show popup: [" + caption + "] " + text);
    }

    public TrayPopup getTrayPopup() {
        return new TrayPopup();
    }

    /**
     * Класс для отображения нотификаций в трее
     */
    public class TrayPopup {
        /**
         * Показывает ошибку из трея
         *
         * @param caption заголовок сообщения
         * @param text    текст сообщения
         */
        public void error(String caption, String text) {
            showPopup(caption, text, TrayIcon.MessageType.ERROR);
        }
    }
}
