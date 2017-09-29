package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.BodyType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.*;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
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
    private static final String P_OPEN_TAG = "<p style=\"font-size: 10pt; margin: 0px; padding: 0px;\">";
    private static final String P_CLOSE_TAG = "</p>";

    /**
     * Инициализация
     *
     * @param connector        EWS
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

                        int thirdSize = (int) (sendedMessages.size() / 3.0);
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

                    // Подключаемся к серверу для отправки только если в очереди есть сообщения.
                    // После чего отключаемся
                    try (ExchangeService service = exchangeConnector.createService()) {
                        MessageElement me;
                        while ((me = messages.pollFirst()) != null) {
                            ItemId messageId = me.getItem();

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
                                } else {

                                    StringBuilder bodyPrefix = new StringBuilder();

                                    EmailAddress from = emailMessage.getFrom();
                                    if (from != null) {
                                        bodyPrefix
                                                .append(P_OPEN_TAG)
                                                .append("From: ")
                                                .append(StringEscapeUtils.escapeHtml4(from.toString()))
                                                .append(P_CLOSE_TAG);
                                    }
                                    Optional<String> recipients = emailAddressesEnumerator(emailMessage.getToRecipients());
                                    recipients.ifPresent(res -> bodyPrefix.append(P_OPEN_TAG).append("To: ").append(res).append(P_CLOSE_TAG));
                                    Optional<String> cc = emailAddressesEnumerator(emailMessage.getCcRecipients());
                                    cc.ifPresent(res -> bodyPrefix.append(P_OPEN_TAG).append("CC: ").append(res).append(P_CLOSE_TAG));
                                    Optional<String> bcc = emailAddressesEnumerator(emailMessage.getBccRecipients());
                                    bcc.ifPresent(res -> bodyPrefix.append(P_OPEN_TAG).append("BCC: ").append(res).append(P_CLOSE_TAG));
                                    Optional<String> reply = emailAddressesEnumerator(emailMessage.getReplyTo());
                                    reply.ifPresent(res -> bodyPrefix.append(P_OPEN_TAG).append("Reply: ").append(res).append(P_CLOSE_TAG));

                                    MessageBody body = new MessageBody();
                                    body.setBodyType(BodyType.HTML);
                                    body.setText(bodyPrefix.toString());

                                    emailMessage.forward(body, recipientEmail);
                                }

                                if (deleteRedirected)
                                    emailMessage.move(deletedItems);
                                else
                                    emailMessage.setIsRead(true);
                            }
                        }

                    } catch (Exception e) {
                        popup.error("Exchange error (Forward module)", e.getMessage());

                        // Пауза между переподключениями, при получении ошибки
                        if (super.isActive())
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException inter) {
                                super.safeStop();
                            }
                    }
                }
            }
        }

        super.wellDone();
    }

    /**
     * Извлекает список адресов и коллекции адресов и преобразует в строку, где эти адреса перечислены через
     * точку с запятой
     *
     * @param emails список адресов в виде коллекции
     * @return список адресов в виде строки
     */
    private Optional<String> emailAddressesEnumerator(EmailAddressCollection emails) {
        if (emails != null) {
            return emails.getItems()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(mail -> StringEscapeUtils.escapeHtml4(mail.toString()))
                    .reduce((m1, m2) -> m1 + "; " + m2);
        } else {
            return Optional.empty();
        }
    }
}
