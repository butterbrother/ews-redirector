package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.FolderId;

import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Осуществляет отправку переадресованных сообщений.
 * Опционально - удаляет переадресованные сообщения.
 * Если не установлено правило удаления - помечает
 * сообщение прочитанным.
 * Для этого периодически считывает очередь, содержащую
 * id данных сообщений. Очередь создаётся этим классом.
 */
public class SendService extends SafeStopService {
    private ConcurrentSkipListSet<MessageElement> messages = new ConcurrentSkipListSet<>();
    private boolean deleteRedirected;
    private EmailAddress recipientEmail;
    private TrayControl.TrayPopup popup;
    private ExchangeService service;
    private MailFilter[] filters;

    /**
     * Инициализация
     * @param service           EWS
     * @param deleteRedirected  удалять перенаправленные
     * @param recipientEmail    e-mail получателя
     * @param popup             трей для передачи аварийных сообщений
     */
    public SendService(ExchangeService service,
                       boolean deleteRedirected,
                       String recipientEmail,
                       TrayControl.TrayPopup popup,
                       MailFilter[] filters) {
        super();
        this.filters = filters;
        this.service = service;
        this.deleteRedirected = deleteRedirected;
        this.recipientEmail = new EmailAddress(recipientEmail);
        this.popup = popup;

        super.runService();
    }

    /**
     * Получение очереди сообщений на перенаправление.
     * @return  очередь обработки
     */
    public ConcurrentSkipListSet<MessageElement> getQueue() {
        return this.messages;
    }

    /**
     * Поток перенаправления сообщений
     */
    public void run() {
        FolderId deletedItems = new FolderId(WellKnownFolderName.DeletedItems);
        while (super.isActive()) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                super.safeStop();
            }
            while (! messages.isEmpty()) {
                if (! super.isActive()) break;
                try {
                    // Обрабатываем только непрочитанные из входящих
                    EmailMessage emailMessage = EmailMessage.bind(service, messages.pollFirst().getItem());
                    if ((! emailMessage.getParentFolderId().equals(deletedItems)) && (! emailMessage.getIsRead())) {
                        try {
                            if (MailFilter.filtrate(filters, emailMessage)) {
                                System.out.println("DEBUG: message \"" + emailMessage.getSubject() + "\" filtered");
                            } else
                                emailMessage.forward(null, recipientEmail);

                            if (deleteRedirected)
                                emailMessage.move(deletedItems);
                            else
                                emailMessage.setIsRead(true);
                        } catch (Exception forwardError) {
                            if (forwardError.getMessage().contains("The specified object was not found in the store.")) {
                                // Ошибка появляется, если объект уже отправлен
                                System.out.println("DEBUG: " + forwardError.getMessage() + ": " + forwardError.getCause().getMessage());
                            } else
                                popup.error("Email forwarding error", forwardError.getMessage());
                        }
                    }
                } catch (Exception e) {
                    popup.error("Exchange error (Forward module)", e.getMessage());
                }
            }
        }

        super.wellDone();
    }
}
