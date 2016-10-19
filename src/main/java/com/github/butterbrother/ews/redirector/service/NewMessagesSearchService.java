package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.graphics.TrayControl;
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

/**
 * Сервис обработки входящих сообщений.
 * Выполняет поиск новых сообщений по статусу -
 * сообщение во входящих и не прочитанное.
 * Запускается периодически.
 */
class NewMessagesSearchService extends SafeStopService {

    private ExchangeConnector exchangeConnector;
    private TrayControl.TrayPopup popup;
    private ConcurrentSkipListSet<MessageElement> messages;

    NewMessagesSearchService(ExchangeConnector exchangeConnector,
                                    TrayControl.TrayPopup popup,
                                    ConcurrentSkipListSet<MessageElement> messages
    ) {
        super();
        this.exchangeConnector = exchangeConnector;
        this.popup = popup;
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
                    try (ExchangeService service = exchangeConnector.createService()){

                        FindItemsResults<Item> results = service.findItems(WellKnownFolderName.Inbox, newMessages, view);
                        for (Item item : results.getItems()) {
                            if (!super.isActive()) break;
                            if (item.getSchema().equals(EmailMessageSchema.Instance)) {
                                messages.add(new MessageElement(item.getId()));
                                System.out.println("DEBUG: new mail watch: Add one new message");
                            }
                        }
                    } catch (Exception msgWorkExc) {
                        popup.error("Exchange error (New mail watch module)", msgWorkExc.getMessage());
                    }
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ie) {
                        super.safeStop();
                        break;
                    }
                }
        } catch (ServiceLocalException se) {
            popup.error("Exchange error (New mail watch module)", se.getMessage());
        }

        super.wellDone();
    }
}
