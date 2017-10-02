package com.github.butterbrother.ews.redirector.service;

/**
 * Интерфейс нотификации для GUI/логов.
 * Сейчас используется только для Popup из трея.
 *
 * @see com.github.butterbrother.ews.redirector.graphics.TrayControl
 */
public interface Notificator {

    /**
     * Оповещение об ошибке
     *
     * @param caption заголовок сообщения
     * @param text    текст сообщения
     */
    void error(String caption, String text);
}
