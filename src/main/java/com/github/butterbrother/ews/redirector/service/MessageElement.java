package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.property.complex.ItemId;

/**
 * Обёртка над ItemId.
 * ItemId - не comparable.
 * Соответственно данный класс обеспечивает сравнение.
 */
class MessageElement implements Comparable<MessageElement> {
    private ItemId item;
    private String unique;

    MessageElement(
            ItemId item
    ) throws Exception {
        this.item = item;
        unique = item.getUniqueId();
    }

    @Override
    public int compareTo(MessageElement o) {
        return unique.compareTo(o.getUniqueId());
    }

    private String getUniqueId() {
        return unique;
    }

    ItemId getItem() {
        return item;
    }
}
