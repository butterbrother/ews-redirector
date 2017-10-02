package com.github.butterbrother.ews.redirector.graphics;

import com.github.butterbrother.ews.redirector.filter.FilterRule;
import com.github.butterbrother.ews.redirector.filter.MailFilter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

/**
 * Управление правилом фильтрации
 */
public class FilterEditor {
    private final SettingsWindow owner;
    private JPanel FilterEditorForm;
    private JLabel FilterNameLabel;
    private JTextField FilterNameInput;
    private JLabel RuleOperatorLabel;
    private JComboBox RuleOperatorList;
    private JPanel UpLevel;
    private JPanel RuleTablesLevel;
    private JPanel RulesTablePanel;
    private JScrollPane RulesTableScroll;
    private JPanel RulesControl;
    private JButton AddRuleButton;
    private JButton RemoveRuleButton;
    private JButton DropRulesButton;
    private JButton SaveFilterButton;
    private JPanel FilterControlLevel;
    private JButton CancelFilterButton;
    private JTable RulesTable;
    private DefaultTableModel RulesTableModel;
    private JFrame frame;
    private int rowHeight = 0;
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    /**
     * Создание окна редактирования фильтров
     */
    FilterEditor(final SettingsWindow owner, int posX, int posY, int winH, int winW) {
        $$$setupUI$$$();

        frame = new JFrame("Filter editor");
        frame.setContentPane(this.FilterEditorForm);
        frame.pack();
        frame.setSize(winW, winH);
        frame.setLocation(posX, posY);
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        AddRuleButton.addActionListener(e -> {
            stopRuleEditing();

            RulesTableModel.addRow(new String[]{FilterRule.RuleTypes[0], FilterRule.RuleOperators[0], ""});
        });

        RemoveRuleButton.addActionListener(e -> {
            stopRuleEditing();

            if (RulesTable.getSelectedRow() >= 0) {
                RulesTableModel.removeRow(RulesTable.getSelectedRow());
            }
        });

        DropRulesButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(null, "This action drop all rules in filter. Continue?", "Warning", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);

            stopRuleEditing();

            if (result == JOptionPane.OK_OPTION) {
                dropRulesTable();
            }
        });

        this.owner = owner;

        // Сохранение фильтра
        SaveFilterButton.addActionListener(e -> {
            stopRuleEditing();

            String rawRules[][] = new String[RulesTableModel.getRowCount()][RulesTableModel.getColumnCount()];
            logger.info("DEBUG: filter table data:");
            for (int row = 0; row < RulesTableModel.getRowCount(); ++row) {
                for (int column = 0; column < RulesTableModel.getColumnCount(); ++column)
                    try {
                        rawRules[row][column] = RulesTableModel.getValueAt(row, column).toString();
                        logger.info("[" + rawRules[row][column] + "]");
                    } catch (NullPointerException noData) {
                        rawRules[row][column] = "";
                        logger.info("[null]");
                    }
                System.out.println();
            }

            owner.doneFilterEditing(
                    new MailFilter(
                            FilterNameInput.getText().trim(),
                            rawRules,
                            RuleOperatorList.getSelectedIndex()
                    )
            );
            frame.setVisible(false);
        });
        // Кнопка отмены. Аналогично закрытию окна
        CancelFilterButton.addActionListener(e -> cancelEditing());
        // Кнопка закрытия окна. Считается, что не внести изменения
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancelEditing();
            }
        });

        new TextPopup(FilterNameInput);
    }

    /**
     * Отмена редактирования.
     * По нажатию на кнопку отмены.
     */
    private void cancelEditing() {
        stopRuleEditing();

        frame.setVisible(false);
        owner.doneFilterEditing(null);
    }

    /**
     * Прекращает редактирование правила.
     * Для редактирования используется редактор TextField,
     * поэтому до окончания редактирования в ячейке таблицы не
     * отобразится изменений.
     */
    private void stopRuleEditing() {
        if (RulesTable.isEditing())
            RulesTable.getCellEditor().stopCellEditing();
    }


    /**
     * Опустошает таблицу правил фильтрации
     */
    private void dropRulesTable() {
        buildTableModel();
        RulesTable.setModel(RulesTableModel);
        setCellEditors();
    }

    /**
     * Возвращает размер окна редактирования фильтров
     *
     * @return размер окна
     */
    Dimension getSize() {
        return frame.getSize();
    }

    /**
     * Возвращает позицию окна редактирования фильтров
     *
     * @return позиция окна
     */
    Point getLocation() {
        return frame.getLocation();
    }

    /**
     * Отобразить окно редактирования фильтра с выбранным фильтром.
     *
     * @param filter почтовый фильтр
     */
    void editFilter(MailFilter filter) {
        FilterNameInput.setText(filter.toString());

        dropRulesTable();
        for (String[] rule : filter.getRawRules())
            RulesTableModel.addRow(rule);

        RuleOperatorList.setSelectedIndex(filter.getOperator());
        frame.setVisible(true);
    }

    private void createUIComponents() {
        RuleOperatorList = new JComboBox<>(MailFilter.Operators);

        buildTableModel();
        RulesTable = new JTable(RulesTableModel);
        setCellEditors();
    }

    /**
     * Создаёт модель для таблицы
     */
    private void buildTableModel() {
        RulesTableModel = new DefaultTableModel();
        RulesTableModel.setColumnCount(3);
        RulesTableModel.setColumnIdentifiers(new String[]{"Type", "Operator", "Value"});
    }

    /**
     * Создаёт редакторы-комбобоксы для ячеек с выбором типа и оператора
     */
    private void setCellEditors() {
        TableColumn type = RulesTable.getColumnModel().getColumn(0);
        type.setCellEditor(new DefaultCellEditor(new JComboBox<>(FilterRule.RuleTypes)));
        type.setWidth(type.getWidth() / 2);

        TableColumn operator = RulesTable.getColumnModel().getColumn(1);
        operator.setCellEditor(new DefaultCellEditor(new JComboBox<>(FilterRule.RuleOperators)));
        operator.setWidth(type.getWidth() / 2);

        TableColumn value = RulesTable.getColumnModel().getColumn(2);
        JTextField fieldWithPopup = new JTextField();
        new TextPopup(fieldWithPopup);
        DefaultCellEditor singleClick = new DefaultCellEditor(fieldWithPopup);
        singleClick.setClickCountToStart(1);
        value.setCellEditor(singleClick);

        RulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (rowHeight == 0) {
            rowHeight = (int) (RulesTable.getRowHeight() * 1.2);
        }
        RulesTable.setRowHeight(rowHeight);
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
        FilterEditorForm = new JPanel();
        FilterEditorForm.setLayout(new BorderLayout(0, 0));
        UpLevel = new JPanel();
        UpLevel.setLayout(new GridBagLayout());
        FilterEditorForm.add(UpLevel, BorderLayout.NORTH);
        FilterNameLabel = new JLabel();
        FilterNameLabel.setHorizontalAlignment(11);
        FilterNameLabel.setHorizontalTextPosition(10);
        FilterNameLabel.setText("Filter name:");
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        UpLevel.add(FilterNameLabel, gbc);
        FilterNameInput = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 100;
        UpLevel.add(FilterNameInput, gbc);
        RuleOperatorLabel = new JLabel();
        RuleOperatorLabel.setText("Logical operator:");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        UpLevel.add(RuleOperatorLabel, gbc);
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.ipadx = 100;
        UpLevel.add(RuleOperatorList, gbc);
        FilterControlLevel = new JPanel();
        FilterControlLevel.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.BOTH;
        UpLevel.add(FilterControlLevel, gbc);
        SaveFilterButton = new JButton();
        SaveFilterButton.setText("Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlLevel.add(SaveFilterButton, gbc);
        CancelFilterButton = new JButton();
        CancelFilterButton.setText("Cancel");
        gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlLevel.add(CancelFilterButton, gbc);
        final JPanel spacer1 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        FilterControlLevel.add(spacer1, gbc);
        RuleTablesLevel = new JPanel();
        RuleTablesLevel.setLayout(new GridBagLayout());
        FilterEditorForm.add(RuleTablesLevel, BorderLayout.CENTER);
        RulesTablePanel = new JPanel();
        RulesTablePanel.setLayout(new CardLayout(0, 0));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        RuleTablesLevel.add(RulesTablePanel, gbc);
        RulesTableScroll = new JScrollPane();
        RulesTableScroll.setVerticalScrollBarPolicy(22);
        RulesTablePanel.add(RulesTableScroll, "Card1");
        RulesTableScroll.setViewportView(RulesTable);
        RulesControl = new JPanel();
        RulesControl.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        RuleTablesLevel.add(RulesControl, gbc);
        AddRuleButton = new JButton();
        AddRuleButton.setText("+");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RulesControl.add(AddRuleButton, gbc);
        RemoveRuleButton = new JButton();
        RemoveRuleButton.setText("-");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RulesControl.add(RemoveRuleButton, gbc);
        DropRulesButton = new JButton();
        DropRulesButton.setText("X");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        RulesControl.add(DropRulesButton, gbc);
        final JPanel spacer2 = new JPanel();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        RulesControl.add(spacer2, gbc);
        FilterNameLabel.setLabelFor(FilterNameInput);
        RuleOperatorLabel.setLabelFor(RuleOperatorList);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return FilterEditorForm;
    }
}
