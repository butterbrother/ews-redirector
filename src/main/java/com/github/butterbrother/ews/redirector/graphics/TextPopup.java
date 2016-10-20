package com.github.butterbrother.ews.redirector.graphics;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * PopUp для текстовых полей.
 * Автоматически добавляет Listener для текстового объекта.
 * Содержит элементы работы с текстом: вырезать/копировать/вставить
 */
class TextPopup
        extends MouseAdapter
        implements ActionListener {

    private JMenuItem cutItem;
    private JMenuItem copyItem;
    private JMenuItem pasteItem;
    private JPopupMenu editorPopup;

    private JTextComponent ownComponent;

    /**
     * Создание и добавление PopUP для текстовых полей (JField и JTextArea)
     * Полю автоматически добавляется слушатель для обработки Popup
     * Для поля с паролем - только вставка
     *
     * @param ownComponent Текстовое поле
     */
    TextPopup(JTextComponent ownComponent) {
        editorPopup = new JPopupMenu();

        if (!(ownComponent instanceof JPasswordField)) {
            cutItem = new JMenuItem("Cut");
            cutItem.addActionListener(this);
            editorPopup.add(cutItem);

            copyItem = new JMenuItem("Copy");
            copyItem.addActionListener(this);
            editorPopup.add(copyItem);
        }

        pasteItem = new JMenuItem("Paste");
        pasteItem.addActionListener(this);
        editorPopup.add(pasteItem);

        this.ownComponent = ownComponent;
        ownComponent.addMouseListener(this);
    }

    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem) e.getSource();
        if (source == cutItem) {
            ownComponent.cut();
        } else if (source == copyItem) {
            ownComponent.copy();
        } else if (source == pasteItem && ownComponent.isEditable()) {
            ownComponent.paste();
        }
    }

    public void mousePressed(MouseEvent e) {
        showPopupMenuIfPopup(e);
    }

    private void showPopupMenuIfPopup(MouseEvent e) {
        if ((e.getButton() & MouseEvent.BUTTON2) == MouseEvent.BUTTON2) {
            if (ownComponent.isEnabled())
                editorPopup.show(e.getComponent(), e.getX(), e.getY());
        }
    }
}
