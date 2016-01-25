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
public class PullEventsService extends SafeStopService {
    private ExchangeService service;
    private TrayControl.TrayPopup popup;
    private ConcurrentSkipListSet<MessageElement> messages;

    /**
     * Инициализация
     * @param service   Exchange
     * @param messages  очередь сообщений
     * @param popup     трей для передачи аварийных сообщений
     */
    public PullEventsService(ExchangeService service,
                             ConcurrentSkipListSet<MessageElement> messages,
                             TrayControl.TrayPopup popup) {
        super();
        this.service = service;
        this.messages = messages;
        this.popup = popup;

        super.runService();
    }

    public void run() {
        List<FolderId> folders = new ArrayList<>();
        folders.add(new FolderId(WellKnownFolderName.Inbox));
        GetEventsResults eventsResults;

        while (super.isActive()) {
            try {
                PullSubscription ps = service.subscribeToPullNotifications(folders, 5, null, EventType.NewMail);
                while (super.isActive()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        super.safeStop();
                    }
                    eventsResults = ps.getEvents();
                    for (ItemEvent event : eventsResults.getItemEvents()) {
                        if (! super.isActive()) break;
                        System.out.println("DEBUG: notify reader module: Add one new message");
                        messages.add(new MessageElement(event.getItemId()));
                    }
                }
            } catch (Exception e) {
                popup.error("Exchange error (Notify reader module)", e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    super.safeStop();
                }
            }
        }

        super.wellDone();
    }
}
