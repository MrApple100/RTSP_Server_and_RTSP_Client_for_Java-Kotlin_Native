package mrapple100.Server;

import mrapple100.Server.rtsp.utils.ConnectCheckerRtsp;
import org.jetbrains.annotations.NotNull;

public class CustomConnectCheckerRTSP implements ConnectCheckerRtsp {
    @Override
    public void onConnectionStartedRtsp(@NotNull String rtspUrl) {
        System.out.println(rtspUrl);
    }

    @Override
    public void onConnectionSuccessRtsp() {
        System.out.println("success connect");
    }

    @Override
    public void onConnectionFailedRtsp(@NotNull String reason) {
        System.out.println(reason);
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
