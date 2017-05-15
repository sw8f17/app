package rocks.stalin.android.app.network;

import java.io.IOException;

import rocks.stalin.android.app.proto.SntpRequest;
import rocks.stalin.android.app.proto.SntpResponse;
import rocks.stalin.android.app.proto.Timestamp;
import rocks.stalin.android.app.utils.LogHelper;
import rocks.stalin.android.app.utils.time.Clock;


public class SntpServer {
    public final String TAG = LogHelper.makeLogTag(SntpServer.class);

    public void register(final MessageConnection connection) {
        connection.addHandler(SntpRequest.class, new MessageConnection.MessageListener<SntpRequest, SntpRequest.Builder>() {
            @Override
            public void packetReceived(SntpRequest message) {
                LogHelper.i(TAG, "SntpRequest recieved");

                Clock.Instant receivedTime = Clock.getTime();
                Timestamp requestReceivedAt = new Timestamp.Builder()
                        .millis(receivedTime.getMillis())
                        .nanos(receivedTime.getNanos())
                        .build();

                Clock.Instant sentTime = Clock.getTime();
                Timestamp responseSentAt = new Timestamp.Builder()
                        .millis(sentTime.getMillis())
                        .nanos(sentTime.getNanos())
                        .build();

                SntpResponse response = new SntpResponse.Builder()
                        .requestReceived(requestReceivedAt)
                        .responseSent(responseSentAt)
                        .requestSent(message.requestSent)
                        .build();

                try {
                    connection.send(response, SntpResponse.class);
                } catch (IOException e) {
                    LogHelper.e(TAG, "Failed to send response to Sntp request!!!");
                    e.printStackTrace();
                }
            }
        });
    }
}
