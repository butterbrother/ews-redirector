package com.github.butterbrother.ews.redirector.filter;

import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;

import java.util.ArrayList;
import java.util.List;

/**
 * Рабочий фильтр для проверки и фильтрации
 * сообщений.
 */
public class MailFilter {
    public static final int OPERATOR_AND = 0;
    public static final int OPERATOR_OR = 1;
    public static final String[] Operators = {"And", "Or"};
    // Имена параметров, хранящихся в файле настроек
    // public static final String FILTER_NAME = "name";
    public static final String FILTER_OPERATOR = "operator";
    public static final String FILTER_RULES = "rules";

    private String name;
    private int operator;
    private FilterRule[] rules;

    /**
     * Инициализация данными из таблицы правил.
     * Правило с пустым значением исключается из набора
     *
     * @param name     Имя фильтра
     * @param rawRules Правила из редактора фильтра:
     *                 - тип из {@link FilterRule#RuleTypes}
     *                 - оператор из {@link FilterRule#RuleOperators}
     *                 - значение. Правило с пустым значением игнорируется
     *                 и удаляется из фильтра
     * @param operator Логический оператор для правил из {@link #Operators}
     */
    public MailFilter(String name, String[][] rawRules, int operator) {
        this.name = name.trim().isEmpty() ? "New filter" : name.trim();
        List<FilterRule> result = new ArrayList<>();

        for (String[] rawRule : rawRules) {
            if (rawRule[2].trim().isEmpty()) continue;
            try {
                result.add(new FilterRule(rawRule));
            } catch (ArrayIndexOutOfBoundsException ignore) {
                System.out.print("DEBUG: rule dropped:");
                for (String i : rawRule) {
                    System.out.print("[" + i + "]");
                }
                System.out.println(ignore.getMessage());
            }
        }

        this.operator = operator;


        rules = new FilterRule[result.size()];
        result.toArray(rules);
    }

    /**
     * Инициализация нового пустого фильтра
     */
    public MailFilter() {
        name = "New filter";
        operator = OPERATOR_AND;
        rules = new FilterRule[0];
    }

    /**
     * Фильтрация сообщения
     *
     * @param filters текущие фильтры
     * @param message сообщение
     * @return true - один или несколько фильтров сработали на сообщении
     * @throws ServiceLocalException    ошибка обработки сообщений
     */
    public static boolean filtrate(MailFilter[] filters, EmailMessage message) throws ServiceLocalException {
        for (MailFilter filter : filters)
            if (filter.check(message))
                return true;

        return false;
    }

    /**
     * Возвращает имя фильтра
     *
     * @return Имя
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Возвращает оператор применения правил
     *
     * @return Логический оператор. Индекс согласно {@link #Operators}
     */
    public int getOperator() {
        return operator;
    }

    /**
     * Получение табличного представления для всех правил в фильтре
     *
     * @return Представление для таблицы размером Nx3
     */
    public String[][] getRawRules() {
        String[][] rawRules = new String[rules.length][3];
        int i = 0;
        for (FilterRule rule : rules) {
            rawRules[i++] = rule.getRuleView();
        }

        return rawRules;
    }

    /**
     * Фильтрация сообщения
     *
     * @param message сообщение
     * @return true - одно или несколько правил сработали на сообщении
     */
    private boolean check(EmailMessage message) throws ServiceLocalException {
        if (operator == OPERATOR_AND) {
            for (FilterRule rule : rules) {
                if (!checkRule(rule, message))
                    return false;
            }

            return true;
        } else {
            for (FilterRule rule : rules) {
                if (checkRule(rule, message))
                    return true;
            }

            return false;
        }
    }


    /**
     * Проверка сообщения на срабатывание по правилу
     *
     * @param rule    правило
     * @param message сообщение
     * @return true - правило сработало для данного сообщения
     * @throws ServiceLocalException    ошибка проверки сообщения
     */
    private boolean checkRule(FilterRule rule, EmailMessage message) throws ServiceLocalException {
        switch (rule.getType()) {
            case FilterRule.TYPE_FROM:
                return rule.check(message.getFrom().getAddress());
            case FilterRule.TYPE_SUBJECT:
                return rule.check(message.getSubject());
            case FilterRule.TYPE_MESSAGE:
                return rule.check(message.getBody().toString());
            case FilterRule.TYPE_TO:
                return checkAddressesRule(rule, message.getToRecipients());
            case FilterRule.TYPE_CC:
                return checkAddressesRule(rule, message.getCcRecipients());
            case FilterRule.TYPE_BCC:
                return checkAddressesRule(rule, message.getBccRecipients());
        }

        return false;
    }

    /**
     * Проверяет на соответствие правилу каждый адрес в списке.
     * Необходимо для обработки правил для списка e-mail: получатели, в копии и т.д.
     *
     * @param rule       Правило
     * @param recipients Список адресов
     * @return true - правило сработало на одном или нескольких адресатах
     */
    private boolean checkAddressesRule(FilterRule rule, EmailAddressCollection recipients) {
        for (EmailAddress address : recipients.getItems()) {
            if (rule.check(address.getAddress()))
                return true;
        }

        return false;
    }

}
