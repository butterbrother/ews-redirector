package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;

import java.util.function.Consumer;

/**
 * Непосредственно управляет потоками-обработчиками.
 * Устанавливает соединение с EWS
 */
public class ServiceController extends SafeStopService {

    private static final boolean BEFORE_STOP = true;
    private static final boolean AFTER_STOP = false;

    private ExchangeConnector connector;
    private PullEventsService pullEvents = null;
    private NewMessagesSearchService newMessages = null;
    private SendService send = null;

    private Consumer<Boolean> onStart;
    private Consumer<Boolean> onStop;
    private Consumer<String> onUpdateUrl;
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
            Consumer<Boolean> onStart,
            Consumer<Boolean> onStop,
            Consumer<String> onUpdateUrl,
            boolean deleteRedirected,
            MailFilter[] filters
    ) {
        this.recipient = recipient;
        this.deleteRedirected = deleteRedirected;
        this.notificator = notificator;
        this.filters = filters;
        this.onStart = onStart;
        this.onStop = onStop;
        this.onUpdateUrl = onUpdateUrl;

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

                onStart.accept(false);
                while (super.isActive()) {

                    if (connector.isEnableAutodiscover())
                        onUpdateUrl.accept(connector.getAutoDiscoverUrl());

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

        onStop.accept(BEFORE_STOP);

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

        onStop.accept(AFTER_STOP);
    }
}
