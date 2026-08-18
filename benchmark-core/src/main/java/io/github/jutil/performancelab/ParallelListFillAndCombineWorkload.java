package io.github.jutil.performancelab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import io.github.jutil.splicelist.SpliceList;

/** Collection-only parallel fill and deterministic consolidation operations. */
final class ParallelListFillAndCombineWorkload {

    private ParallelListFillAndCombineWorkload() {
    }

    /**
     * Fills exactly sized worker-local ArrayLists and copies them into one exactly sized list.
     */
    static <E> ArrayList<E> arrayListAddAll(
            E[] elements,
            int parallelism,
            ExecutorService executor
    ) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(executor, "executor");

        int partitionCount = activePartitionCount(elements.length, parallelism);
        ArrayList<Future<ArrayList<E>>> futures = new ArrayList<>(partitionCount);
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            int start = partitionStart(elements.length, partitionCount, partitionIndex);
            int size = expectedPartitionSize(elements.length, partitionCount, partitionIndex);
            futures.add(executor.submit(() -> fillArrayList(elements, start, size)));
        }

        return combineArrayLists(await(futures), elements.length);
    }

    /**
     * Fills worker-local SpliceLists and transfers their segments in partition order.
     */
    static <E> SpliceList<E> spliceListSpliceTail(
            E[] elements,
            int parallelism,
            ExecutorService executor
    ) throws InterruptedException, ExecutionException {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(executor, "executor");

        int partitionCount = activePartitionCount(elements.length, parallelism);
        ArrayList<Future<SpliceList<E>>> futures = new ArrayList<>(partitionCount);
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            int start = partitionStart(elements.length, partitionCount, partitionIndex);
            int size = expectedPartitionSize(elements.length, partitionCount, partitionIndex);
            futures.add(executor.submit(() -> fillSpliceList(elements, start, size)));
        }

        return combineSpliceLists(await(futures));
    }

    static <E> ArrayList<E> combineArrayLists(
            Iterable<? extends ArrayList<E>> partials,
            int elementCount
    ) {
        ArrayList<E> destination = new ArrayList<>(elementCount);
        for (ArrayList<E> partial : partials) {
            destination.addAll(partial);
        }
        return destination;
    }

    static <E> SpliceList<E> combineSpliceLists(
            Iterable<? extends SpliceList<E>> partials
    ) {
        SpliceList<E> destination = new SpliceList<>();
        for (SpliceList<E> partial : partials) {
            destination.spliceTail(partial);
        }
        return destination;
    }

    static <E> List<ArrayList<E>> prepareArrayListPartials(E[] elements, int parallelism) {
        Objects.requireNonNull(elements, "elements");
        int partitionCount = activePartitionCount(elements.length, parallelism);
        ArrayList<ArrayList<E>> partials = new ArrayList<>(partitionCount);
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            int start = partitionStart(elements.length, partitionCount, partitionIndex);
            int size = expectedPartitionSize(elements.length, partitionCount, partitionIndex);
            partials.add(fillArrayList(elements, start, size));
        }
        return partials;
    }

    static <E> List<SpliceList<E>> prepareSpliceListPartials(E[] elements, int parallelism) {
        Objects.requireNonNull(elements, "elements");
        int partitionCount = activePartitionCount(elements.length, parallelism);
        ArrayList<SpliceList<E>> partials = new ArrayList<>(partitionCount);
        for (int partitionIndex = 0; partitionIndex < partitionCount; partitionIndex++) {
            int start = partitionStart(elements.length, partitionCount, partitionIndex);
            int size = expectedPartitionSize(elements.length, partitionCount, partitionIndex);
            partials.add(fillSpliceList(elements, start, size));
        }
        return partials;
    }

    static int activePartitionCount(int elementCount, int parallelism) {
        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be positive: " + parallelism);
        }
        return Math.min(elementCount, parallelism);
    }

    static int expectedPartitionSize(
            int elementCount,
            int partitionCount,
            int partitionIndex
    ) {
        int baseSize = elementCount / partitionCount;
        int remainder = elementCount % partitionCount;
        return baseSize + (partitionIndex < remainder ? 1 : 0);
    }

    static int partitionStart(
            int elementCount,
            int partitionCount,
            int partitionIndex
    ) {
        int baseSize = elementCount / partitionCount;
        int remainder = elementCount % partitionCount;
        return partitionIndex * baseSize + Math.min(partitionIndex, remainder);
    }

    private static <E> ArrayList<E> fillArrayList(E[] elements, int start, int size) {
        ArrayList<E> local = new ArrayList<>(size);
        int end = start + size;
        for (int index = start; index < end; index++) {
            local.add(elements[index]);
        }
        return local;
    }

    private static <E> SpliceList<E> fillSpliceList(E[] elements, int start, int size) {
        SpliceList<E> local = new SpliceList<>(size);
        int end = start + size;
        for (int index = start; index < end; index++) {
            local.add(elements[index]);
        }
        return local;
    }

    private static <T> List<T> await(
            List<? extends Future<? extends T>> futures
    ) throws InterruptedException, ExecutionException {
        ArrayList<T> results = new ArrayList<>(futures.size());
        for (Future<? extends T> future : futures) {
            results.add(future.get());
        }
        return results;
    }
}
