package mrapple100.utils;

import java.awt.*;
import java.util.ArrayDeque;

//------------------------------------
//Synchronize frames
//------------------------------------
public class FrameSynchronizer {

    private ArrayDeque<Image> queue;
    private int bufSize;
    private int curSeqNb;
    private Image lastImage;

    public FrameSynchronizer(int bsize) {
        curSeqNb = 1;
        bufSize = bsize;
        queue = new ArrayDeque<Image>(bufSize);
    }

    //synchronize frames based on their sequence number
    public void addFrame(Image image, int seqNum) {
        if (seqNum < curSeqNb) {
            queue.add(lastImage);
        }
        else if (seqNum > curSeqNb) {
            for (int i = curSeqNb; i < seqNum; i++) {
                queue.add(lastImage);
            }
            queue.add(image);
        }
        else {
            queue.add(image);
        }
    }

    //get the next synchronized frame
    public Image nextFrame() {
        curSeqNb++;
        lastImage = queue.peekLast();
        return queue.remove();
    }
}