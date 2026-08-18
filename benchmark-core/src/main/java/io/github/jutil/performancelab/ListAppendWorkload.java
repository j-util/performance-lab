package io.github.jutil.performancelab;

import java.util.ArrayList;

import io.github.jutil.splicelist.SpliceList;

/** Fresh-collection append loops used by the append benchmark. */
final class ListAppendWorkload {

    private ListAppendWorkload() {
    }

    static <E> ArrayList<E> arrayListCapacityHintAdd(
            int elementCount,
            int capacityHint,
            E marker
    ) {
        ArrayList<E> list = new ArrayList<>(capacityHint);
        for (int index = 0; index < elementCount; index++) {
            list.add(marker);
        }
        return list;
    }

    static <E> SpliceList<E> spliceListSegmentSizeAdd(
            int elementCount,
            int segmentSize,
            E marker
    ) {
        SpliceList<E> list = new SpliceList<>(segmentSize);
        for (int index = 0; index < elementCount; index++) {
            list.add(marker);
        }
        return list;
    }

    static <E> ArrayList<E> arrayListExactFinalCapacityAdd(int elementCount, E marker) {
        ArrayList<E> list = new ArrayList<>(elementCount);
        for (int index = 0; index < elementCount; index++) {
            list.add(marker);
        }
        return list;
    }

    static <E> ArrayList<E> arrayListDefaultGrowingAdd(
            int elementCount,
            E marker
    ) {
        ArrayList<E> list = new ArrayList<>();
        for (int index = 0; index < elementCount; index++) {
            list.add(marker);
        }
        return list;
    }

    static <E> SpliceList<E> spliceListSegmentSizeAddLast(
            int elementCount,
            int segmentSize,
            E marker
    ) {
        SpliceList<E> list = new SpliceList<>(segmentSize);
        for (int index = 0; index < elementCount; index++) {
            list.addLast(marker);
        }
        return list;
    }

    static long unusedSpliceListCapacity(int elementCount, int segmentSize) {
        if (elementCount == 0) {
            return 0L;
        }
        long segmentCount = Math.ceilDiv((long) elementCount, segmentSize);
        return segmentCount * segmentSize - elementCount;
    }
}
