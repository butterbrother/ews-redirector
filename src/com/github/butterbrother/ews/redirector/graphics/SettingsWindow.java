package com.github.butterbrother.ews.redirector.graphics;

import com.github.butterbrother.ews.redirector.Settings;
import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.service.ServiceController;
import org.json.JSONException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

/**
 * Окно с параметрами.
 * Пока что здесь только параметры подключения.
 */
public class SettingsWindow {
    public static final String KEY_WINDOW_HEIGHT = "Settings window height";
    public static final String KEY_WINDOW_WIDTH = "Settings window width";
    public static final String KEY_WINDOW_X = "Settings window X position";
    public static final String KEY_WINDOW_Y = "Settings window Y position";
    public static final String DOMAIN = "Domain name";
    public static final String EMAIL = "e-mail";
    public static final String LOGIN = "User login";
    public static final String PASSWORD = "User password";
    public static final String AUTO_DISCOVER_URL = "Auto discover EWS service URL";
    public static final String AUTO_DISCOVER_ENABLED = "Enable auto discover URL";
    public static final String DELETE_REDIRECTED = "Enable delete redirected mail";
    public static final String RECIPIENT_EMAIL = "Recipient e-mail";
    public static final String FILTERS = "Email filters";
    public static final String FILTER_EDITOR_HEIGHT = "Filter editor window height";
    public static final String FILTER_EDITOR_WIDTH = "Filter editor window width";
    public static final String FILTER_EDITOR_X = "Filter editor window X position";
    public static final String FILTER_EDITOR_Y = "Filter editor window Y position";

    private JPanel switchPanel;
    private JTextField LoginInput;
    private JPasswordField PasswordInput;
    private JTextField DomainInput;
    private JTextField EMailInput;
    private JCheckBox AutoDiscover;
    private JTextField URLInput;
    private JButton ApplyButton;
    private JButton StartStopButton;
    private JButton ExitButton;
    private JTextField RecipientInput;
    private JCheckBox DeleteRedirected;
    private JTabbedPane SettingsViewSwitcher;
    private JPanel CSInputPanel;
    private JLabel EMailLabel;
    private JLabel DomainLabel;
    private JLabel LoginLabel;
    private JLabel PasswordLabel;
    private JLabel URLLabel;
    private JLabel RecipientLabel;
    private JPanel ControlPanel;
    private JPanel FilterPanel;
    private JPanel FilterListPanel;
    private JScrollPane FilterListScroll;
    private JList RulesList;
    private JPanel FilterControlPanel;
    private JButton AddRule;
    private JButton RemoveRule;
    private JButton ClearRules;
    // Родительское окно
    private JFrame win;
    // Настройки
    private Settings settings;
    // Сервис
    private ServiceController controller = null;
    // Оповещение из трея
    private TrayControl.TrayPopup popup;
    // Расположение и размер окна фильтрации
    private int filterX = 1, filterY = 1, filterW = 640, filterH = 400;
    // Фильтры
    private LinkedList<MailFilter> filters;
    private DefaultListModel<String> RulesListModel;
    // Редактор фильтров
    private FilterEditor filterEditor;
    // Текущий редактируемый фильтр
    private int edited = -1;

    public SettingsWindow(final Settings settings, final TrayControl.TrayPopup popup) {
        this.settings = settings;
        this.popup = popup;
        $$$setupUI$$$();

        win = new JFrame("EWS mail redirection");
        win.setContentPane(switchPanel);
        win.pack();

        // Добавляем меню для работы с текстом в поля ввода
        new TextPopup(DomainInput);
        new TextPopup(EMailInput);
        new TextPopup(LoginInput);
        new TextPopup(PasswordInput);
        new TextPopup(URLInput);
        // При закрытии окна - скрываемся
        win.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowPos();
                win.setVisible(false);
            }
        });
        // Активация автоопределения EWS URL
        AutoDiscover.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                URLInput.setEnabled(!AutoDiscover.isSelected());
            }
        });
        // Применение настроек
        ApplyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveConnectionSettings();
                if (checkServiceControl())
                    restartService();
            }
        });
        // Кнопка выхода
        ExitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopServiceControl();
                saveWindowPos();
                System.exit(0);
            }
        });
        // Запуск и останов
        // При останове элементы управления разблокируются сервисом
        StartStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (checkServiceControl()) {
                    stopServiceControl();
                } else {
                    StartStopButton.setEnabled(false);
                    ApplyButton.setEnabled(false);
                    createServiceControl();
                    StartStopButton.setText("Stop");
                    StartStopButton.setEnabled(true);
                    ApplyButton.setEnabled(true);
                }
            }
        });

        // Пробуем запустить сервис автоматически
        if (loadSettings())
            StartStopButton.doClick();

        // Инициализируем окно настроек
        filterEditor = new FilterEditor(this, filterX, filterY, filterH, filterW);

        // Добавление правила фильтрации
        AddRule.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                win.setEnabled(false);
                edited = -1;
                filterEditor.editFilter(new MailFilter());
            }
        });
        // Удаление правила фильтрации
        RemoveRule.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = RulesList.getSelectedIndex();
                if (selected >= 0) {
                    RulesListModel.remove(selected);
                    filters.remove(selected);
                }
            }
        });
        // Удаление всех правил фильтрации
        ClearRules.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(null, "This action drop all filters. Continue?", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    RulesListModel.removeAllElements();
                    filters = new LinkedList<>();
                }
            }
        });
        // Редактирование фильтра по двойному щелчку
        RulesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if ((e.getButton() & MouseEvent.BUTTON1) == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int selected = RulesList.getSelectedIndex();
                    if (selected >= 0) {
                        edited = selected;
                        win.setEnabled(false);
                        filterEditor.editFilter(filters.get(selected));
                    }
                }
            }
        });
    }

    /**
     * Окончание редактирования/создания нового фильтра
     * Разблокирует окно настроек.
     *
     * @param filter Обновлённый фильтр. Если null, то считается,
     *               что изменения были отклонены.
     */
    protected void doneFilterEditing(MailFilter filter) {
        if (filter != null)
            if (edited >= 0) {
                filters.remove(edited);
                filters.add(edited, filter);
                RulesListModel.remove(edited);
                RulesListModel.add(edited, filter.toString());
            } else {
                filters.add(filter);
                RulesListModel.addElement(filter.toString());
            }

        win.setEnabled(true);
        win.setState(Frame.NORMAL);
        win.toFront();
    }

    /**
     * Проверка, что сервис включен и работает.
     *
     * @return статус сервиса
     */
    private boolean checkServiceControl() {
        return controller != null && controller.isActive() && !controller.isDone();
    }

    /**
     * Создание управляющего сервиса
     */
    private void createServiceControl() {
        MailFilter[] mailFilters = new MailFilter[filters.size()];
        filters.toArray(mailFilters);
        controller = new ServiceController(
                settings.getString(DOMAIN),
                settings.getString(EMAIL),
                settings.getString(RECIPIENT_EMAIL),
                settings.getString(LOGIN),
                settings.getString(PASSWORD),
                settings.getString(AUTO_DISCOVER_URL),
                settings.getBoolean(AUTO_DISCOVER_ENABLED),
                popup,
                URLInput,
                StartStopButton,
                ApplyButton,
                settings.getBoolean(DELETE_REDIRECTED),
                mailFilters
        );
    }

    /**
     * Остановка управляющего сервиса
     */
    public void stopServiceControl() {
        if (checkServiceControl()) {
            controller.safeStop();
            try {
                while (!controller.isDone())
                    Thread.sleep(50);
            } catch (InterruptedException ignore) {
            }
        }
    }


    /**
     * Перезапуск управляющего сервиса
     */
    private void restartService() {
        if (checkServiceControl()) {
            ApplyButton.setEnabled(false);
            StartStopButton.setEnabled(false);
            stopServiceControl();
            createServiceControl();
            ApplyButton.setEnabled(true);
            StartStopButton.setEnabled(true);
        }
    }

    /**
     * Сохраняет позицию окна в файл конфигурации
     */
    public void saveWindowPos() {
        settings.setInteger(KEY_WINDOW_HEIGHT, win.getHeight());
        settings.setInteger(KEY_WINDOW_WIDTH, win.getWidth());
        settings.setInteger(KEY_WINDOW_X, win.getX());
        settings.setInteger(KEY_WINDOW_Y, win.getY());

        Dimension filterEditorSize = filterEditor.getSize();
        Point filterEditorLocation = filterEditor.getLocation();
        settings.setInteger(FILTER_EDITOR_HEIGHT, (int) filterEditorSize.getHeight());
        settings.setInteger(FILTER_EDITOR_WIDTH, (int) filterEditorSize.getWidth());
        settings.setInteger(FILTER_EDITOR_X, (int) filterEditorLocation.getX());
        settings.setInteger(FILTER_EDITOR_Y, (int) filterEditorLocation.getY());

        settings.saveSettings();
    }

    /**
     * Сохранение настроек соединения
     */
    public void saveConnectionSettings() {
        settings.setString(DOMAIN, DomainInput.getText());
        settings.setString(EMAIL, EMailInput.getText());
        settings.setString(LOGIN, LoginInput.getText());
        settings.setString(PASSWORD, new String(PasswordInput.getPassword()));
        settings.setBoolean(AUTO_DISCOVER_ENABLED, AutoDiscover.isSelected());
        settings.setString(AUTO_DISCOVER_URL, URLInput.getText());
        settings.setString(RECIPIENT_EMAIL, RecipientInput.getText());
        settings.setBoolean(DELETE_REDIRECTED, DeleteRedirected.isSelected());
        settings.writeAllFilters(FILTERS, filters);
        settings.saveSettings();
    }

    /**
     * Отображаем окно. При каждом отображении восстанавливаем статус.
     */
    public void showSettingsWin() {
        win.setVisible(true);
        loadSettings();
    }

    /**
     * Загрузка настроек приложения.
     *
     * @return Все настройки успешно загружены
     */
    private boolean loadSettings() {
        // Загрузка фильтров
        filters = settings.getAllFilters(FILTERS);
        RulesListModel.removeAllElements();
        for (MailFilter filter : filters) {
            RulesListModel.addElement(filter.toString());
        }

        try {
            // Загрузка параметров основного окна настроек
            win.setLocation(settings.getInteger(KEY_WINDOW_X), settings.getInteger(KEY_WINDOW_Y));
            win.setSize(settings.getInteger(KEY_WINDOW_WIDTH), settings.getInteger(KEY_WINDOW_HEIGHT));

            // Загрузка настроек подключения
            DomainInput.setText(settings.getString(DOMAIN));
            EMailInput.setText(settings.getString(EMAIL));
            LoginInput.setText(settings.getString(LOGIN));
            PasswordInput.setText(settings.getString(PASSWORD));
            AutoDiscover.setSelected(settings.getBoolean(AUTO_DISCOVER_ENABLED));
            URLInput.setText(settings.getString(AUTO_DISCOVER_URL));
            URLInput.setEnabled(!AutoDiscover.isSelected());
            RecipientInput.setText(settings.getString(RECIPIENT_EMAIL));
            DeleteRedirected.setSelected(settings.getBoolean(DELETE_REDIRECTED));

            // Загрузка окна настроек фильтра
            filterH = settings.getInteger(FILTER_EDITOR_HEIGHT);
            filterW = settings.getInteger(FILTER_EDITOR_WIDTH);
            filterX = settings.getInteger(FILTER_EDITOR_X);
            filterY = settings.getInteger(FILTER_EDITOR_Y);
        } catch (JSONException ignore) {
            System.out.println("DEBUG: " + ignore.getMessage());
            return false;
        }
        return true;
    }

    private void createUIComponents() {
        RulesListModel = new DefaultListModel<>();
        RulesList = new JList<>(RulesListModel);
        RulesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        switchPanel = new JPanel();
        switchPanel.setLayout(new BorderLayout(0, 0));
        ControlPanel = new JPanel();
        ControlPanel.setLayout(new GridBagLayout());
        switchPanel.add(ControlPanel, BorderLayout.SOUTH);
        StartStopButton = new JButton();
        StartStopButton.setText("Start");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 10;
        ControlPanel.add(StartStopButton, gbc);
        ApplyButton = new JButton();
        ApplyButton.setText("Apply");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 10;
        ControlPanel.add(ApplyButton, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ControlPanel.add(spacer1, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        ControlPanel.add(spacer2, gbc);
        ExitButton = new JButton();
        ExitButton.setText("Exit");
        gbc = new GridBagConstraints();
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 10;
        ControlPanel.add(ExitButton, gbc);
        SettingsViewSwitcher = new JTabbedPane();
        SettingsViewSwitcher.setTabPlacement(1);
        switchPanel.add(SettingsViewSwitcher, BorderLayout.CENTER);
        CSInputPanel = new JPanel();
        CSInputPanel.setLayout(new GridBagLayout());
        SettingsViewSwitcher.addTab("Connection Settings", CSInputPanel);
        EMailLabel = new JLabel();
        EMailLabel.setHorizontalAlignment(4);
        EMailLabel.setHorizontalTextPosition(10);
        EMailLabel.setText("e-mail:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(EMailLabel, gbc);
        EMailInput = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(EMailInput, gbc);
        DomainLabel = new JLabel();
        DomainLabel.setHorizontalAlignment(4);
        DomainLabel.setHorizontalTextPosition(10);
        DomainLabel.setText("Domain:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(DomainLabel, gbc);
        DomainInput = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(DomainInput, gbc);
        LoginLabel = new JLabel();
        LoginLabel.setText("Login:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(LoginLabel, gbc);
        LoginInput = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(LoginInput, gbc);
        PasswordLabel = new JLabel();
        PasswordLabel.setText("Password:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(PasswordLabel, gbc);
        PasswordInput = new JPasswordField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(PasswordInput, gbc);
        AutoDiscover = new JCheckBox();
        AutoDiscover.setSelected(true);
        AutoDiscover.setText("Auto discover EWS URL");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.anchor = GridBagConstraints.WEST;
        CSInputPanel.add(AutoDiscover, gbc);
        URLLabel = new JLabel();
        URLLabel.setText("EWS URL:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(URLLabel, gbc);
        URLInput = new JTextField();
        URLInput.setEnabled(false);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(URLInput, gbc);
        RecipientLabel = new JLabel();
        RecipientLabel.setHorizontalAlignment(4);
        RecipientLabel.setHorizontalTextPosition(4);
        RecipientLabel.setText("Recipient:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.EAST;
        CSInputPanel.add(RecipientLabel, gbc);
        RecipientInput = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        CSInputPanel.add(RecipientInput, gbc);
        DeleteRedirected = new JCheckBox();
        DeleteRedirected.setText("Delete redirected e-mails");
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.WEST;
        CSInputPanel.add(DeleteRedirected, gbc);
        final JPanel spacer3 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 8;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        CSInputPanel.add(spacer3, gbc);
        FilterPanel = new JPanel();
        FilterPanel.setLayout(new GridBagLayout());
        SettingsViewSwitcher.addTab("Filter settings", FilterPanel);
        FilterListPanel = new JPanel();
        FilterListPanel.setLayout(new BorderLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        FilterPanel.add(FilterListPanel, gbc);
        FilterListScroll = new JScrollPane();
        FilterListScroll.setHorizontalScrollBarPolicy(30);
        FilterListScroll.setVerticalScrollBarPolicy(22);
        FilterListPanel.add(FilterListScroll, BorderLayout.CENTER);
        FilterListScroll.setViewportView(RulesList);
        FilterControlPanel = new JPanel();
        FilterControlPanel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        FilterPanel.add(FilterControlPanel, gbc);
        AddRule = new JButton();
        AddRule.setText("+");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlPanel.add(AddRule, gbc);
        RemoveRule = new JButton();
        RemoveRule.setText("-");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlPanel.add(RemoveRule, gbc);
        ClearRules = new JButton();
        ClearRules.setText("X");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlPanel.add(ClearRules, gbc);
        final JPanel spacer4 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        FilterControlPanel.add(spacer4, gbc);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return switchPanel;
    }
}
