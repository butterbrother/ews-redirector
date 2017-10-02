package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.LogicalOperator;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

/**
 * Сервис обработки входящих сообщений.
 * Выполняет поиск новых сообщений по статусу -
 * сообщение во входящих и не прочитанное.
 * Запускается периодически.
 */
class NewMessagesSearchService extends SafeStopService {

    private ExchangeConnector exchangeConnector;
    private Notificator notificator;
    private ConcurrentSkipListSet<MessageElement> messages;
    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    NewMessagesSearchService(ExchangeConnector exchangeConnector,
                             Notificator notificator,
                             ConcurrentSkipListSet<MessageElement> messages
    ) {
        super();
        this.exchangeConnector = exchangeConnector;
        this.notificator = notificator;
        this.messages = messages;

        super.runService();
    }

    @Override
    public void run() {
        try {
            // Генерируем представление (фильтр)
            SearchFilter newMessages = new SearchFilter.SearchFilterCollection(
                    LogicalOperator.And,
                    new SearchFilter.IsEqualTo(EmailMessageSchema.IsRead, false)
            );
            ItemView view = new ItemView(20);
            view.getOrderBy().clear();
            view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Descending);

            while (super.isActive()) {

                // Подключаемся, пробегаем по списку сообщений, отключаемся
                try (ExchangeService service = exchangeConnector.createService()) {

                    FindItemsResults<Item> results = service.findItems(WellKnownFolderName.Inbox, newMessages, view);
                    for (Item item : results.getItems()) {
                        if (!super.isActive()) break;
                        if (item.getSchema().equals(EmailMessageSchema.Instance)) {
                            messages.add(new MessageElement(item.getId()));
                            logger.info("Add one new message");
                        }
                    }
                } catch (Exception msgWorkExc) {
                    notificator.error("Exchange error (New mail watch module)", msgWorkExc.getMessage());
                    logger.severe("Exchange error: " + msgWorkExc.getMessage());
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    super.safeStop();
                    break;
                }
            }
        } catch (ServiceLocalException se) {
            notificator.error("Exchange error (New mail watch module)", se.getMessage());
            logger.severe("Exchange error: " + se.getMessage());
        }

        super.wellDone();
    }
}
