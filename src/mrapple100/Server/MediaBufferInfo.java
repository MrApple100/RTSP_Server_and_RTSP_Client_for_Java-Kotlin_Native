package mrapple100.Server;

public final class MediaBufferInfo {
        public int flags;
        public int offset;
        public long presentationTimeUs;
        public int size;

        public void BufferInfo() {
            throw new RuntimeException("Stub!");
        }

        public void set(int newOffset, int newSize, long newTimeUs, int newFlags) {
            throw new RuntimeException("Stub!");
        }

}
