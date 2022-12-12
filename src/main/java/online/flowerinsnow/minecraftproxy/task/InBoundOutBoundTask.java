package online.flowerinsnow.minecraftproxy.task;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;

public class InBoundOutBoundTask implements Runnable {
    public final SocketChannel inBound;
    public final SocketChannel outBound;

    public InBoundOutBoundTask(SocketChannel inBound, SocketChannel outBound) {
        this.inBound = inBound;
        this.outBound = outBound;
    }

    @Override
    public void run() {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        try {
            while (inBound.read(bb) != -1) {
                bb.flip();
                outBound.write(bb);
                bb.clear();
            }
            onClose(null);
        } catch (ClosedByInterruptException ignored) {
        } catch (IOException e) {
            onClose(e);
        }
    }

    /**
     * 当连接断开时抛出
     *
     * @param e 如果有异常
     */
    protected void onClose(IOException e) {
    }
}
