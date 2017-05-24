package rocks.stalin.android.app.network;

import java.net.SocketAddress;

import rocks.stalin.android.app.framework.concurrent.observable.ObservableFuture;

public interface OffsetSourceFactory {
    ObservableFuture<LocalNetworkSntpOffsetSource> create(String localHost, int localPort, String remoteHost, int remotePort);
}
