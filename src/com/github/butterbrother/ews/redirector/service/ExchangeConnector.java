package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;

import java.net.URI;

/**
 * Осуществляет проверку возможности подключения к EWS.
 * Генерирует объекты ExchangeService с авторизацией для сервисов.
 */
public class ExchangeConnector {
    private ExchangeCredentials credentials;
    private String email;
    private URI url;
    private boolean enableAutodiscover;

    /**
     * Инициализация с проверкой учётных данных и возможности подключиться.
     *
     * @param domain             Домен. Опционален.
     * @param login              Логин
     * @param password           Пароль
     * @param email              e-mail. Используется только если указана необходимость автоматического определения EWS URL
     * @param url                url EWS. Опционален. Если не указан - будет включено автоматическое определение
     * @param enableAutodiscover Необходимость автоматического определения EWS URL
     * @throws Exception
     */
    protected ExchangeConnector(
            String domain,
            String login,
            String password,
            String email,
            String url,
            boolean enableAutodiscover
    ) throws Exception {
        this.email = email;

        this.enableAutodiscover = enableAutodiscover;
        if (url.isEmpty() && !enableAutodiscover)
            this.enableAutodiscover = true;

        if (domain.isEmpty()) {
            credentials = new WebCredentials(login, password);
        } else {
            credentials = new WebCredentials(login, password, domain);
        }

        ExchangeService connectionTest = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        connectionTest.setCredentials(credentials);

        if (this.enableAutodiscover) {
            connectionTest.autodiscoverUrl(email, new RedirectionUrlCallback());
            this.url = connectionTest.getUrl();
        } else {
            this.url = new URI(url);
            connectionTest.setUrl(this.url);
        }

        System.out.println("DEBUG: connection test, get inbox mail count");
        System.out.println(Folder.bind(connectionTest, WellKnownFolderName.Inbox).getTotalCount());
    }

    /**
     * Создаёт подключение к EWS.
     * Считается одиночным окном браузера, т.е. в один момент времени можно проводить
     * только одну операцию.
     *
     * @return подключение к EWS
     * @throws Exception
     */
    public ExchangeService createService() throws Exception {
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.setCredentials(credentials);
        if (enableAutodiscover)
            service.autodiscoverUrl(email, new RedirectionUrlCallback());
        else
            service.setUrl(url);

        System.out.println("DEBUG: connection test, get inbox mail count");
        System.out.println(Folder.bind(service, WellKnownFolderName.Inbox).getTotalCount());

        return service;
    }

    /**
     * Возвращает используемый URL
     *
     * @return EWS URL
     */
    public String getCurrentUrl() {
        return url.toString();
    }
}
