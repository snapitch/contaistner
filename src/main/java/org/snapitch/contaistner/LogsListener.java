package org.snapitch.contaistner;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogMessage;
import com.spotify.docker.client.LogStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static com.spotify.docker.client.DockerClient.AttachParameter.*;
import static java.util.Arrays.asList;

@Slf4j
@RequiredArgsConstructor
public class LogsListener {

    private final DockerClient client;
    private final String containerId;
    private final Consumer<String> logConsumer;

    public void listenLogs() {
        try {
            LogStream logStream = client.attachContainer(containerId, LOGS, STREAM, STDOUT, STDERR);
            StringBuilder logs = new StringBuilder();
            while (true) {
                LogMessage next = logStream.next();
                ByteBuffer content = next.content();
                byte[] buffer = new byte[content.remaining()];
                content.get(buffer);
                logs.append(new String(buffer));

                String[] logLines = logs.toString().split("\n");

                if (logLines.length > 0) {
                    List<String> finishedLogsLines = asList(Arrays.copyOfRange(logLines, 0, logLines.length));
                    logs = new StringBuilder();

                    if (!finishedLogsLines.isEmpty()) {
                        for (String finishedLogsLine : finishedLogsLines) {
                            logConsumer.accept(finishedLogsLine);
                        }
                    }
                }
            }

        } catch (Exception ignored) {
            LOGGER.debug("End of logs listener for container {}", containerId);
        }
    }
}
