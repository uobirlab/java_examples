package org.barc;

import org.ros.concurrent.CancellableLoop;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

/**
 * A simple class to show you how to publish a message. Yanked from the documentation.
 */
public class Talker extends AbstractNodeMain {

    /**
     * Set the name of the 'node'.
     *
     * @return node name
     */
    @Override
    public GraphName getDefaultNodeName() {
        return new GraphName("barc/talker");
    }

    /**
     * Simply creates a publisher which publishes 'Hello world: num'.
     *
     * @param connectedNode the node to create the topic on
     */
    @Override
    public void onStart(ConnectedNode connectedNode) {
        final Publisher<std_msgs.String> publisher = connectedNode.newPublisher("chatter", std_msgs.String._TYPE);

        connectedNode.executeCancellableLoop(new CancellableLoop() {
            private int sequenceNumber;

            @Override
            protected void setup() {
                sequenceNumber = 0;
            }

            @Override
            protected void loop() throws InterruptedException {
                std_msgs.String msg = publisher.newMessage();
                msg.setData("Hello world:  " + sequenceNumber);
                publisher.publish(msg);
                sequenceNumber++;
                Thread.sleep(1000);
            }
        });
    }
}
