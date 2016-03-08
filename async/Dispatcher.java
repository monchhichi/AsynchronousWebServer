package async;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue; // for Set and Iterator
import java.util.Set;

public class Dispatcher implements Runnable {

  private Selector selector;
  private Queue<Runnable> pendingInvocations;

  private TimeoutThread timeoutThread;

  public Dispatcher() {
    pendingInvocations = new LinkedList<Runnable>();
    // create selector
    try {
      selector = Selector.open();
    } catch (IOException ex) {
      System.out.println("Cannot create selector.");
      ex.printStackTrace();
      System.exit(1);
    } // end of catch
  } // end of Dispatcher

  public Selector selector() {
    return selector;
  }

  public void invokeLater(Runnable run) {
    synchronized (pendingInvocations) {
      pendingInvocations.add(run);
    }
    // select() will return so that
    // new added method may be invoked?
    selector.wakeup();
  }

  public void run() {

    while (true) {
      Debug.DEBUG("Enter selection");
      // command queue
      Runnable run;
      if ((run = pendingInvocations.poll()) != null)
        new Thread(run).start();

      // ready event
      try {
        // check to see if any events
        selector.select();
      } catch (IOException ex) {
        ex.printStackTrace();
        break;
      }

      // readKeys is a set of ready events
      Set<SelectionKey> readyKeys = selector.selectedKeys();

      // create an iterator for the set
      Iterator<SelectionKey> iterator = readyKeys.iterator();

      // iterate over all events
      while (iterator.hasNext()) {

        SelectionKey key = (SelectionKey) iterator.next();
        iterator.remove();

        try {
          if (key.isAcceptable()) { // a new connection is ready to be
                                    // accepted

            final IAcceptHandler aH = (IAcceptHandler) key.attachment();
            aH.handleAccept(key);

            // register timeout event
            timeoutThread = new TimeoutThread(1000, new ITimeout() {
              @Override
              public void close() {
                invokeLater(new Runnable() {
                  public void run() {
                    try {
                      aH.closeConnection();
                    } catch (IOException e) {
                      // TODO Auto-generated catch block
                      e.printStackTrace();
                    }
                  }
                });
              }
            });
            // timeoutThread.start();

          } // end of isAcceptable

          if (key.isReadable() || key.isWritable()) {
            IReadWriteHandler rwH = (IReadWriteHandler) key.attachment();

            if (key.isReadable()) {
              rwH.handleRead(key);
            } // end of if isReadable

            if (key.isWritable()) {
              timeoutThread.interrupt();
              rwH.handleWrite(key);
              key.channel().close();
            } // end of if isWritable
          } // end of readwrite
        } catch (IOException ex) {
          Debug.DEBUG("Exception when handling key " + key);
          key.cancel();
          try {
            key.channel().close();
            // in a more general design, call have a handleException
          } catch (IOException cex) {
          }
        } // end of catch

      } // end of while (iterator.hasNext()) {

    } // end of while (true)
  } // end of run

  class TimeoutThread extends Thread {
    private long time;
    private ITimeout timeout;

    public TimeoutThread(long time, ITimeout callback) {
      Debug.DEBUG("Timeout event registered.");
      this.time = time;
      timeout = callback;
    }

    @Override
    public void run() {
      try {
        Thread.sleep(time);
        timeout.close();
      } catch (InterruptedException e) {
      }
    }
  }

  interface ITimeout {

    public void close();
  }

}
