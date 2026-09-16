import io.github.jutil.performancelab.*;
import java.util.*;
import java.util.concurrent.*;
public class ValidateDestinations {
  public static void main(String[] args) throws Exception {
    for (int n : new int[]{100000,10000000}) {
      var source=HardwoodDestinationMaterializationCases.createSourceArrays(n);
      var pool=Executors.newFixedThreadPool(8);
      try {
        for(int batch : new int[]{8192,1000000}) {
          for(int path=0;path<4;path++) {
            Object result=switch(path) {
              case 0 -> HardwoodDestinationMaterializationCases.sequentialRangedBatches(source,batch);
              case 1 -> HardwoodDestinationMaterializationCases.singleThreadedColumnAppender(source,batch);
              case 2 -> HardwoodDestinationMaterializationCases.executorPipelinedColumnAppender(source,batch,pool);
              default -> HardwoodDestinationMaterializationCases.arrayListRows(source,batch);
            };
            var store=result instanceof HardwoodMarketDataProjectionStore ? (HardwoodMarketDataProjectionStore)result:null;
            var list=store==null ? (List<?>)result:null;
            if ((store!=null?store.size():list.size())!=n) throw new AssertionError("size");
            for(int i=0;i<n;i++) {
              var row=store!=null?store.viewAt(i):(HardwoodMarketDataProjection)list.get(i);
              if(row.timestamp()!=source.timestamps()[i] || row.sequenceNumber()!=source.sequenceNumbers()[i]
                || !Objects.equals(row.symbol(),source.symbols()[i]) || !Objects.equals(row.venue(),source.venues()[i])
                || !Objects.equals(row.side(),source.sides()[i]) || row.bidPrice()!=source.bidPrices()[i]
                || row.askPrice()!=source.askPrices()[i] || row.lastTradePrice()!=source.lastTradePrices()[i]) throw new AssertionError("row "+i);
            }
            System.out.println("validated rows="+n+" batch="+batch+" path="+path+" all eight columns");
          }
        }
      } finally {pool.shutdown(); if(!pool.awaitTermination(30,TimeUnit.SECONDS)) throw new AssertionError("executor");}
    }
  }
}
