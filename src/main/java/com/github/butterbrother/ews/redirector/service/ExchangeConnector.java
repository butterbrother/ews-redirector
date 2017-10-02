package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;

import java.net.URI;
import java.util.logging.Logger;

/**
 * Осуществляет проверку возможности подключения к EWS.
 * Генерирует объекты ExchangeService с авторизацией для сервисов.
 */
class ExchangeConnector {
    private ExchangeCredentials credentials;
    private String email;
    private String url;
    private String autoDiscoverUrl = "";
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
     */
    ExchangeConnector(
            String domain,
            String login,
            String password,
            String email,
            String url,
            boolean enableAutodiscover
    ) {
        this.email = email;
        this.url = url;

        this.enableAutodiscover = enableAutodiscover;
        if (url.isEmpty() && !enableAutodiscover)
            this.enableAutodiscover = true;

        if (domain.isEmpty()) {
            credentials = new WebCredentials(login, password);
        } else {
            credentials = new WebCredentials(login, password, domain);
        }
    }

    /**
     * Создаёт подключение к EWS.
     * Считается одиночным окном браузера, т.е. в один момент времени можно проводить
     * только одну операцию.
     *
     * @return подключение к EWS
     * @throws Exception
     */
    ExchangeService createService() throws Exception {
        ExchangeService service = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        service.setCredentials(credentials);
        if (enableAutodiscover) {
            service.autodiscoverUrl(email, new RedirectionUrlCallback());
            autoDiscoverUrl = service.getUrl().toString();
        } else {
            service.setUrl(new URI(url));
        }

        Logger.getLogger(this.getClass().getSimpleName())
                .info("Connection test, get inbox mail count: " +
                        Folder.bind(service, WellKnownFolderName.Inbox).getTotalCount());

        return service;
    }

    /**
     * Возвращает найденный Autodiscover URL
     *
     * @return EWS URL
     */
    String getAutoDiscoverUrl() {
        return autoDiscoverUrl;
    }

    /**
     * Возвращает статус автонахождения EWS URL
     *
     * @return статус автонахождения, задающийся галочкой в интерфейсе
     */
    public boolean isEnableAutodiscover() {
        return enableAutodiscover;
    }
}
