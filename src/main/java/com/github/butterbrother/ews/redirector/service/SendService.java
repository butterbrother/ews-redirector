package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.ItemId;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Осуществляет отправку переадресованных сообщений.
 * Опционально - удаляет переадресованные сообщения.
 * Если не установлено правило удаления - помечает
 * сообщение прочитанным.
 * Для этого периодически считывает очередь, содержащую
 * id данных сообщений. Очередь создаётся этим классом.
 */
class SendService extends SafeStopService {
    private ConcurrentSkipListSet<MessageElement> messages = new ConcurrentSkipListSet<>();
    private ArrayList<ItemId> sendedMessages = new ArrayList<>();   // Прочитанные/пересланные сообщения
    private boolean deleteRedirected;
    private EmailAddress recipientEmail;
    private TrayControl.TrayPopup popup;
    private ExchangeConnector exchangeConnector;
    private MailFilter[] filters;

    /**
     * Инициализация
     *
     * @param connector          EWS
     * @param deleteRedirected удалять перенаправленные
     * @param recipientEmail   e-mail получателя
     * @param popup            трей для передачи аварийных сообщений
     */
    SendService(ExchangeConnector connector,
                       boolean deleteRedirected,
                       String recipientEmail,
                       TrayControl.TrayPopup popup,
                       MailFilter[] filters) {
        super();
        this.filters = filters;
        this.exchangeConnector = connector;
        this.deleteRedirected = deleteRedirected;
        this.recipientEmail = new EmailAddress(recipientEmail);
        this.popup = popup;

        super.runService();
    }

    /**
     * Получение очереди сообщений на перенаправление.
     *
     * @return очередь обработки
     */
    ConcurrentSkipListSet<MessageElement> getQueue() {
        return this.messages;
    }

    /**
     * Поток перенаправления сообщений
     */
    public void run() {
        FolderId deletedItems = new FolderId(WellKnownFolderName.DeletedItems);

        int sendedFlushCount = 5;

        process:
        {
            while (super.isActive()) {
                try (ExchangeService service = exchangeConnector.createService()) {

                    while (super.isActive()) {

                        // Пауза перед повторной отправкой
                        try {
                            Thread.sleep(200);

                            // Очищаем треть списка уже переданных каждую секунду
                            if (sendedMessages.size() > 0)
                                --sendedFlushCount;
                            else
                                sendedFlushCount = 5;

                            if (sendedFlushCount <= 0) {
                                sendedFlushCount = 5;

                                int thirdSize = (int)(sendedMessages.size() / 3.0);
                                if (thirdSize <= sendedMessages.size())
                                    for (int i = 0; i < thirdSize; ++i)
                                        sendedMessages.remove(0);

                                sendedMessages.trimToSize();
                            }
                        } catch (InterruptedException e) {
                            super.safeStop();
                            break process;
                        }

                        // Отсылаем все уведомления из очереди
                        while (!messages.isEmpty()) {
                            if (!super.isActive()) break process;

                            ItemId messageId = messages.pollFirst().getItem();

                            // Пропускаем уже переданные сообщения. Сообщения с одинаковыми Id могут поступать одновременно
                            // с двух разных источников - PullEventService и NewMessageSearchService
                            if (sendedMessages.contains(messageId))
                                continue;

                            sendedMessages.add(messageId);   // Добавляем во временный список прочитанных

                            EmailMessage emailMessage = EmailMessage.bind(service, messageId);

                            // Обрабатываем только непрочитанные из входящих
                            if ((!emailMessage.getParentFolderId().equals(deletedItems)) && (!emailMessage.getIsRead())) {
                                    if (MailFilter.filtrate(filters, emailMessage)) {
                                        System.out.println("DEBUG: message \"" + emailMessage.getSubject() + "\" filtered");
                                    } else
                                        emailMessage.forward(null, recipientEmail);

                                    if (deleteRedirected)
                                        emailMessage.move(deletedItems);
                                    else
                                        emailMessage.setIsRead(true);
                            }
                        }
                    }
                } catch (Exception e) {
                    popup.error("Exchange error (Forward module)", e.getMessage());
                }

                // Пауза между переподключениями, при получении ошибки
                if (super.isActive())
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        super.safeStop();
                    }
            }
        }

        super.wellDone();
    }
}
