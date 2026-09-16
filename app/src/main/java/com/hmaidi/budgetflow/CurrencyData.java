package com.hmaidi.budgetflow;

import java.util.ArrayList;
import java.util.List;

public class CurrencyData {

    public static class Currency {
        public String code;
        public String symbol;
        public String name;

        public Currency(String code, String symbol, String name) {
            this.code = code;
            this.symbol = symbol;
            this.name = name;
        }

        @Override
        public String toString() {
            if (symbol.isEmpty()) {
                return code + " — " + name;
            }
            return code + " (" + symbol + ") — " + name;
        }
    }

    public static List<Currency> getCurrencies() {
        List<Currency> list = new ArrayList<>();
        list.add(new Currency("TND", "د.ت", "Tunisian Dinar"));
        list.add(new Currency("USD", "$", "US Dollar"));
        list.add(new Currency("EUR", "€", "Euro"));
        list.add(new Currency("GBP", "£", "British Pound"));
        list.add(new Currency("SAR", "﷼", "Saudi Riyal"));
        list.add(new Currency("AED", "د.إ", "UAE Dirham"));
        list.add(new Currency("MAD", "MAD", "Moroccan Dirham"));
        list.add(new Currency("DZD", "DZD", "Algerian Dinar"));
        list.add(new Currency("EGP", "E£", "Egyptian Pound"));
        list.add(new Currency("JPY", "¥", "Japanese Yen"));
        list.add(new Currency("CNY", "¥", "Chinese Yuan"));
        list.add(new Currency("INR", "₹", "Indian Rupee"));
        list.add(new Currency("CAD", "CA$", "Canadian Dollar"));
        list.add(new Currency("AUD", "A$", "Australian Dollar"));
        list.add(new Currency("CHF", "CHF", "Swiss Franc"));
        list.add(new Currency("TRY", "₺", "Turkish Lira"));
        list.add(new Currency("BRL", "R$", "Brazilian Real"));
        list.add(new Currency("MXN", "MX$", "Mexican Peso"));
        list.add(new Currency("RUB", "₽", "Russian Ruble"));
        list.add(new Currency("KWD", "د.ك", "Kuwaiti Dinar"));
        return list;
    }
}