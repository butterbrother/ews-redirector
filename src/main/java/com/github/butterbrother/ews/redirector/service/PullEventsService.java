package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.PullSubscription;
import microsoft.exchange.webservices.data.property.complex.FolderId;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Сервис обработки входящих сообщений.
 * Выполняет поиск новых сообщений по событиям, полученным
 * от Exchange-сервера.
 * Соответственно обрабатывает только те письма, события о
 * прибытии которых появились уже после запуска сервиса.
 * Предпочтительно, что бы запускался первым
 */
class PullEventsService extends SafeStopService {
    private ExchangeConnector exchangeConnector;
    private Notificator notificator;
    private ConcurrentSkipListSet<MessageElement> messages;
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    /**
     * Инициализация
     *
     * @param connector Exchange
     * @param messages  очередь сообщений
     * @param notificator     трей для передачи аварийных сообщений
     */
    PullEventsService(ExchangeConnector connector,
                      ConcurrentSkipListSet<MessageElement> messages,
                      Notificator notificator) {
        super();
        this.exchangeConnector = connector;
        this.messages = messages;
        this.notificator = notificator;

        super.runService();
    }

    public void run() {
        List<FolderId> folders = new ArrayList<>();
        folders.add(new FolderId(WellKnownFolderName.Inbox));
        GetEventsResults eventsResults;

        boolean successConnection;
        processing:
        {

            while (super.isActive()) {

                successConnection = false;

                try (ExchangeService service = exchangeConnector.createService()) {
                    successConnection = true;

                    PullSubscription ps = service.subscribeToPullNotifications(folders, 1, null, EventType.NewMail);
                    Calendar expireDate = Calendar.getInstance();
                    expireDate.add(Calendar.SECOND, 50);

                    while (super.isActive()) {
                        Calendar current = Calendar.getInstance();
                        if (current.compareTo(expireDate) >= 0) {
                            break;
                        }
                        eventsResults = ps.getEvents();

                        for (ItemEvent event : eventsResults.getItemEvents()) {
                            if (!super.isActive()) break;
                            logger.info("Add one new message");
                            messages.add(new MessageElement(event.getItemId()));
                        }

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            super.safeStop();
                            break processing;
                        }
                    }
                } catch (Exception e) {
                    /*
                     * Отображаем ошибку только при неудачном подключении, а не спустя таймаута push-уведомлений.
                     * Если ошибка авторизации - это увидим при повторной попытке, когда service будет null.
                     */
                    if (!successConnection) {
                        notificator.error("Exchange error (Notify reader module)", e.getMessage());
                        logger.severe("Exchange error: " + e.getMessage());

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            super.safeStop();
                            break processing;
                        }
                    } else {
                        logger.warning("Exchange error: " + e.getMessage());
                    }
                }
            }
        }

        super.wellDone();
    }
}
