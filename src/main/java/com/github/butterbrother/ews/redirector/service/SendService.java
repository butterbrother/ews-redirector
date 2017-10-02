package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
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
import java.util.logging.Logger;

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
    private ArrayList<ItemId> sentMessages = new ArrayList<>();   // Прочитанные/пересланные сообщения
    private boolean deleteRedirected;
    private EmailAddress recipientEmail;
    private Notificator notificator;
    private ExchangeConnector exchangeConnector;
    private MailFilter[] filters;
    private static final String P_OPEN_TAG = "<p style=\"font-size: 10pt; margin: 0px; padding: 0px;\">";
    private static final String P_CLOSE_TAG = "</p>";
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    /**
     * Инициализация
     *
     * @param connector        EWS
     * @param deleteRedirected удалять перенаправленные
     * @param recipientEmail   e-mail получателя
     * @param notificator            трей для передачи аварийных сообщений
     */
    SendService(ExchangeConnector connector,
                boolean deleteRedirected,
                String recipientEmail,
                Notificator notificator,
                MailFilter[] filters) {
        super();
        this.filters = filters;
        this.exchangeConnector = connector;
        this.deleteRedirected = deleteRedirected;
        this.recipientEmail = new EmailAddress(recipientEmail);
        this.notificator = notificator;

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

        int sentFlushCount = 5;

        process:
        {
            while (super.isActive()) {

                // Пауза перед повторной отправкой
                try {
                    Thread.sleep(200);

                    // Очищаем треть списка уже переданных каждую секунду
                    if (sentMessages.size() > 0)
                        --sentFlushCount;
                    else
                        sentFlushCount = 5;

                    if (sentFlushCount <= 0) {
                        sentFlushCount = 5;

                        int thirdSize = (int) (sentMessages.size() / 3.0);
                        if (thirdSize <= sentMessages.size())
                            for (int i = 0; i < thirdSize; ++i)
                                sentMessages.remove(0);

                        sentMessages.trimToSize();
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
                            if (sentMessages.contains(messageId))
                                continue;

                            sentMessages.add(messageId);   // Добавляем во временный список прочитанных

                            EmailMessage emailMessage = EmailMessage.bind(service, messageId);

                            // Обрабатываем только непрочитанные из входящих
                            if ((!emailMessage.getParentFolderId().equals(deletedItems)) && (!emailMessage.getIsRead())) {
                                if (MailFilter.filtrate(filters, emailMessage)) {
                                    logger.info("Message \"" + emailMessage.getSubject() + "\" filtered");
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
                                    logger.info("Message \"" + emailMessage.getSubject() + "\" forwarded");
                                }

                                if (deleteRedirected)
                                    emailMessage.move(deletedItems);
                                else
                                    emailMessage.setIsRead(true);
                            }
                        }

                    } catch (Exception e) {
                        notificator.error("Exchange error (Forward module)", e.getMessage());
                        logger.severe("Exchange error: " + e.getMessage());

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
