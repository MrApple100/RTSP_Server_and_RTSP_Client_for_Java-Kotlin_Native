package mrapple100.Server;

import com.pedro.rtsp.utils.ConnectCheckerRtsp;
import org.jetbrains.annotations.NotNull;

public class CustomConnectCheckerRTSP implements ConnectCheckerRtsp {
    @Override
    public void onConnectionStartedRtsp(@NotNull String rtspUrl) {

    }

    @Override
    public void onConnectionSuccessRtsp() {

    }

    @Override
    public void onConnectionFailedRtsp(@NotNull String reason) {

    }

    @Override
    public void onNewBitrateRtsp(long bitrate) {

    }

    @Override
    public void onDisconnectRtsp() {

    }

    @Override
    public void onAuthErrorRtsp() {

    }

    @Override
    public void onAuthSuccessRtsp() {

    }
}
