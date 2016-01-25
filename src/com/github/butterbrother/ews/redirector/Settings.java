package com.github.butterbrother.ews.redirector;

import com.github.butterbrother.ews.redirector.filter.FilterRule;
import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.TextPopup;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Set;

/**
 * Хранит, загружает и сохраняет настройки
 * приложения
 */
public class Settings {
    private Path settingsFile = Paths.get(System.getProperty("user.home"), ".ews_redirector.json");
    private JSONObject file;
    private TrayControl.TrayPopup popup;

    /**
     * Инициализация и чтение настроек
     */
    public Settings(TrayControl.TrayPopup popup) {
        this.popup = popup;
        if (Files.notExists(settingsFile)) {
            file = new JSONObject();
        } else {
            StringBuilder settingsLoader = new StringBuilder();
            try (BufferedReader reader = Files.newBufferedReader(settingsFile, Charset.forName("UTF-8"))) {
                String buffer;
                while ((buffer = reader.readLine()) != null) {
                    settingsLoader.append(buffer).append('\n');
                }
                file = new JSONObject(settingsLoader.toString());
            } catch (JSONException | IOException e) {
                popup.error("Unable to read settings", e.getMessage());
                file = new JSONObject();
            }
        }
    }

    /**
     * Сохранение настроек
     */
    public void saveSettings() {
        try (BufferedWriter writer = Files.newBufferedWriter(settingsFile, Charset.forName("UTF-8"))) {
            writer.write(file.toString());
        } catch (IOException e) {
            popup.error("Unable to save settings", e.getMessage());
        }
    }

    public int getInteger(String key) {
        return file.getInt(key);
    }

    public String getString(String key) {
        return file.getString(key);
    }

    public void setInteger(String key, int value) {
        file.put(key, value);
    }

    public void setString(String key, String value) {
        file.put(key, value);
    }

    public boolean getBoolean(String key) {
        return file.getBoolean(key);
    }

    public void setBoolean(String key, boolean value) {
        file.put(key, value);
    }

    /**
     * Получение всех фильтров сообщений
     * @param key   Ключ, под которым хранятся фильтры
     * @return      Связанный список со всеми обнаруженными фильтрами.
     *              Если произошла ошибка чтения/фильтров нет, то вернётся
     *              пустой список
     */
    public LinkedList<MailFilter> getAllFilters(String key) {
        LinkedList<MailFilter> filters = new LinkedList<>();
        try {
            JSONObject rawFilters = file.getJSONObject(key);
            for (String name : rawFilters.keySet()) {
                try {
                    JSONObject rawFilter = rawFilters.getJSONObject(name);

                    String rawFilterOperator = rawFilter.getString(MailFilter.FILTER_OPERATOR);
                    int filterOperator;
                    if (rawFilterOperator.trim().equalsIgnoreCase(MailFilter.Operators[0]))
                        filterOperator = MailFilter.OPERATOR_AND;
                    else
                        filterOperator = MailFilter.OPERATOR_OR;

                    JSONArray rawFilterRules = rawFilter.getJSONArray(MailFilter.FILTER_RULES);
                    LinkedList<String[]> rawRules = new LinkedList<>();
                    for (int i = 0; i < rawFilterRules.length(); ++i) {
                        try {
                            JSONObject rawRule = rawFilterRules.getJSONObject(i);
                            rawRules.add(new String[]{
                                    rawRule.getString(FilterRule.RULE_TYPE),
                                    rawRule.getString(FilterRule.RULE_OPERATOR),
                                    rawRule.getString(FilterRule.RULE_VALUE)
                            });
                        } catch (JSONException loadRuleFail) {
                            System.out.println("DEBUG: " + loadRuleFail.getMessage());
                        }
                    }
                    String[][] rawRulesArray = new String[rawRules.size()][];
                    rawRules.toArray(rawRulesArray);

                    filters.add(new MailFilter(
                            name,
                            rawRulesArray,
                            filterOperator
                    ));
                } catch (JSONException filterLoadFail) {
                    System.out.println("DEBUG: " + filterLoadFail.getMessage());
                }
            }
        } catch (JSONException ignore) {
            System.out.println("DEBUG: " + ignore.getMessage());
        }

        return filters;
    }

    /**
     * Запись всх фильтров сообщений в файл конфигурации
     * @param key       Ключ в файле настроек
     * @param filters   Фильтры сообщений
     */
    public void writeAllFilters(String key, LinkedList<MailFilter> filters) {
        JSONObject rawFilters = new JSONObject();
        for (MailFilter filter : filters) {
            JSONObject rawFilter = new JSONObject();

            rawFilter.put(MailFilter.FILTER_OPERATOR, MailFilter.Operators[filter.getOperator()]);

            JSONArray rawRules = new JSONArray();
            for (String[] rule : filter.getRawRules()) {
                JSONObject rawRule = new JSONObject()
                        .put(FilterRule.RULE_TYPE, rule[0])
                        .put(FilterRule.RULE_OPERATOR, rule[1])
                        .put(FilterRule.RULE_VALUE, rule[2]);
                rawRules.put(rawRule);
            }
            rawFilter.put(MailFilter.FILTER_RULES, rawRules);

            rawFilters.put(filter.toString(), rawFilter);
        }

        file.put(key, rawFilters);
    }
}
