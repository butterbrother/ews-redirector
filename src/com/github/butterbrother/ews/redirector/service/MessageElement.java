package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.property.complex.ItemId;

/**
 * Обёртка над ItemId.
 * ItemId - не comparable.
 * Соответственно данный класс обеспечивает сравнение.
 */
public class MessageElement implements Comparable<MessageElement>{
    private ItemId item;
    private String unique;

    public MessageElement(
            ItemId item
    ) throws Exception {
        this.item = item;
        unique = item.getUniqueId();
    }

    @Override
    public int compareTo(MessageElement o) {
        return unique.compareTo(o.getUniqueId());
    }

    public String getUniqueId() {
        return unique;
    }

    public ItemId getItem() {
        return item;
    }
}
