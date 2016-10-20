package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.PullSubscription;
import microsoft.exchange.webservices.data.property.complex.FolderId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

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
    private TrayControl.TrayPopup popup;
    private ConcurrentSkipListSet<MessageElement> messages;

    /**
     * Инициализация
     *
     * @param connector Exchange
     * @param messages  очередь сообщений
     * @param popup     трей для передачи аварийных сообщений
     */
    PullEventsService(ExchangeConnector connector,
                      ConcurrentSkipListSet<MessageElement> messages,
                      TrayControl.TrayPopup popup) {
        super();
        this.exchangeConnector = connector;
        this.messages = messages;
        this.popup = popup;

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

                    PullSubscription ps = service.subscribeToPullNotifications(folders, 5, null, EventType.NewMail);

                    while (super.isActive()) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            super.safeStop();
                            break processing;
                        }

                        eventsResults = ps.getEvents();

                        for (ItemEvent event : eventsResults.getItemEvents()) {
                            if (!super.isActive()) break;
                            System.out.println("DEBUG: notify reader module: Add one new message");
                            messages.add(new MessageElement(event.getItemId()));
                        }

                    }
                } catch (Exception e) {

                    /*
                     * Отображаем ошибку только при неудачном подключении, а не спустя таймаута push-уведомлений.
                     * Если ошибка авторизации - это увидим при повторной попытке, когда service будет null.
                     */
                    if (!successConnection) {
                        popup.error("Exchange error (Notify reader module)", e.getMessage());

                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            super.safeStop();
                            break processing;
                        }
                    }
                }
            }
        }

        super.wellDone();
    }
}
