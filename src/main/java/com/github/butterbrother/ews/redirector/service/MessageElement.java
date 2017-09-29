package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.property.complex.ItemId;

import java.util.Objects;

/**
 * Обёртка над ItemId.
 * ItemId - не comparable.
 * Соответственно данный класс обеспечивает сравнение.
 */
class MessageElement implements Comparable<MessageElement> {
    private ItemId item;
    private String unique;
    private int hash;

    MessageElement(
            ItemId item
    ) throws Exception {
        this.item = item;
        unique = item.getUniqueId();
        hash = Objects.hash(this.item, unique);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MessageElement)) return false;
        MessageElement that = (MessageElement) o;
        return Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public int compareTo(MessageElement o) {
        return unique.compareTo(o.unique);
    }


    ItemId getItem() {
        return item;
    }
}
