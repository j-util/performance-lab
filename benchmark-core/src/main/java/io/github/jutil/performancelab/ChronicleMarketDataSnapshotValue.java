package io.github.jutil.performancelab;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.values.MaxUtf8Length;
import net.openhft.chronicle.values.NotNull;

/** Chronicle Values schema for a complete market-data snapshot. */
interface ChronicleMarketDataSnapshotValue extends Byteable {

    int SYMBOL_UTF8_CAPACITY = 7;

    long getCapturedAtNanos();

    void setCapturedAtNanos(long value);

    CharSequence getSymbol();

    void setSymbol(@NotNull @MaxUtf8Length(SYMBOL_UTF8_CAPACITY) CharSequence value);

    double getLastTradePrice();

    void setLastTradePrice(double value);

    double getLastTradeSize();

    void setLastTradeSize(double value);

    double getBidPrice();

    void setBidPrice(double value);

    double getAskPrice();

    void setAskPrice(double value);

    double getBidSize();

    void setBidSize(double value);

    double getAskSize();

    void setAskSize(double value);
}
