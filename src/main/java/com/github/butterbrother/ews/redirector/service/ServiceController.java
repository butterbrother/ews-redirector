package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;

import javax.swing.*;
import java.util.function.Consumer;

/**
 * Непосредственно управляет потоками-обработчиками.
 * Устанавливает соединение с EWS
 */
public class ServiceController extends SafeStopService {
    private ExchangeConnector connector = null;
    private PullEventsService pullEvents = null;
    private NewMessagesSearchService newMessages = null;
    private SendService send = null;

    private JButton startStopButton;
    private JButton applyButton;
    private JTextField urlField;
    private String recipient;
    private boolean deleteRedirected;
    private Notificator notificator;
    private MailFilter[] filters;

    public ServiceController(
            String domain,
            String email,
            String recipient,
            String login,
            String password,
            String url,
            boolean enableAuto,
            Notificator notificator,
            JTextField urlField,
            JButton startStopButton,
            JButton applyButton,
            boolean deleteRedirected,
            MailFilter[] filters
    ) {
        this.startStopButton = startStopButton;
        this.applyButton = applyButton;
        this.recipient = recipient;
        this.deleteRedirected = deleteRedirected;
        this.notificator = notificator;
        this.filters = filters;
        this.urlField = urlField;

        connector = new ExchangeConnector(domain, login, password, email, url, enableAuto);
        super.runService();

    }

    /**
     * Запуск обработчиков и ожидание
     */
    @Override
    public void run() {
        if (connector != null) {
            try {
                send = new SendService(connector, deleteRedirected, recipient, notificator, filters);
                pullEvents = new PullEventsService(connector, send.getQueue(), notificator);
                newMessages = new NewMessagesSearchService(connector, notificator, send.getQueue());

                startStopButton.setText("Stop");
                while (super.isActive()) {

                    if (connector.isEnableAutodiscover() && !urlField.isEnabled())
                        urlField.setText(connector.getAutoDiscoverUrl());

                    try {
                        Thread.sleep(200);
                        if (send.isDone() || pullEvents.isDone() || newMessages.isDone())
                            if (super.isActive())
                                safeStop();
                    } catch (InterruptedException ie) {
                        if (super.isActive())
                            safeStop();
                    }
                }
            } catch (Exception e) {
                notificator.error("Connection error", e.getMessage());
                if (super.isActive())
                    safeStop();
            }
        }

        super.wellDone();
    }

    /**
     * Плавная остановка с ожиданием завершения работы
     * всех сервисов.
     */
    @Override
    public synchronized void safeStop() {
        super.safeStop();

        startStopButton.setEnabled(false);
        applyButton.setEnabled(false);

        Consumer<SafeStopService> st = srv -> {
            if (srv != null) {
                srv.safeStop();
                while (!srv.isDone()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        };

        st.accept(pullEvents);
        st.accept(newMessages);
        st.accept(send);

        startStopButton.setText("Start");
        startStopButton.setEnabled(true);
        applyButton.setEnabled(true);
    }
}
