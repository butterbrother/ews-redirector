package com.github.butterbrother.ews.redirector.oldtestclasses;

import com.github.butterbrother.ews.redirector.service.RedirectionUrlCallback;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.PullSubscription;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FolderView;

import java.util.ArrayList;
import java.util.List;


/**
 * <EMPTY HEADER, PLEASE EDIT>
 * Created by user on 16.01.16.
 */
public class test {
    public static void main(String args[]) {
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        ExchangeCredentials credentials = new WebCredentials("xxx", "xxx", "xxx");
        service.setCredentials(credentials);
        try {
            System.out.println("Connecting...");
            service.autodiscoverUrl("xxx@xxx", new RedirectionUrlCallback());
            System.out.println("EWS Url: " + service.getUrl());

            // Отображаем все входящие
            Folder inboxFolder = Folder.bind(service, WellKnownFolderName.Inbox);
            System.out.println(inboxFolder.getDisplayName());
            showSubfolders(service, inboxFolder, 1);

            // Подписываемся на pull-нотификации
            List<FolderId> folders = new ArrayList<>();
            folders.add(inboxFolder.getId());
            PullSubscription ps = service.subscribeToPullNotifications(folders, 5, null, EventType.NewMail);
            // Ждём письма
            GetEventsResults results;
            do {
                Thread.sleep(200);  // Это всё должно быть отдельным потоком, но для теста и так хватит
                results = ps.getEvents();
                // Перечисляем только письма
                for (ItemEvent item : results.getItemEvents()) {
                    System.out.println("New event: " + item.getEventType());
                    if (item.getEventType() == EventType.NewMail) {
                        // Получаем сообщение, выводим заголовок
                        EmailMessage message = EmailMessage.bind(service, item.getItemId());
                        System.out.println("e-mail: " + message.getSubject());
                        // Перенаправляем сообщение
                        message.forward(
                                //new MessageBody(BodyType.HTML, prefixBuilder.toString()),
                                null,
                                new EmailAddress("another@domain.ru")
                        );
                        // Удаляем переданное сообщение
                        message.move(WellKnownFolderName.DeletedItems);
                    }
                }
            } while (results.getAllEvents().size() == 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showSubfolders(ExchangeService service, Folder parent, int tab) throws Exception {
        FindFoldersResults foldersResults = service.findFolders(parent.getId(), new FolderView(parent.getChildFolderCount()));
        for (Folder folder : foldersResults.getFolders()) {
            for (int i = 1; i <= tab; ++i) System.out.print(' ');
            System.out.println(folder.getDisplayName() + " (" + folder.getUnreadCount() + "/" + folder.getTotalCount() + ")");
            if (folder.getChildFolderCount() > 0)
                showSubfolders(service, folder, tab + 1);
        }
    }
}
