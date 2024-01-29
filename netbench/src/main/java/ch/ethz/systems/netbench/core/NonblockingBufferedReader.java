package ch.ethz.systems.netbench.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class NonblockingBufferedReader {
	private int command;
    private volatile boolean closed = false;
    private Thread backgroundReaderThread = null;
    BufferedReader bufferedReader;
    final Object lock = new Object();

    public NonblockingBufferedReader(InputStream input) {
        this(new InputStreamReader(input));
      }
    public NonblockingBufferedReader(final InputStreamReader reader) {
    	bufferedReader = new BufferedReader(reader);
        backgroundReaderThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        int c = bufferedReader.read();
                        if(c!='\n') {
                        	command = c;
                        }
                        if(command == 'p') {
                        	System.out.println("Pausing");
                        	stopReading();
                        }
                    
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    closed = true;
                }
            }
        });
        backgroundReaderThread.setDaemon(true);
        backgroundReaderThread.start();
    }

    public int readChar()  {
    	int tmp = command;
    	command = 0;
        return tmp;
    }

    public void close() {
        if (backgroundReaderThread != null) {
            backgroundReaderThread.interrupt();
            backgroundReaderThread = null;
        }
    }
	public void reset() {
		command = 0;
		synchronized (lock) {
			lock.notify();
		}
		
		
	}
	
	private void stopReading(){
		try {
			synchronized (lock) {
				lock.wait();
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			throw new RuntimeException(e);
		}
	}
	public boolean isPaused() {
		
		return command=='p';
	}
	
}
