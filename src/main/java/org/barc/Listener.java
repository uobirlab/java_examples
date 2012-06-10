package org.barc;

import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;


/**
 * A simple listener class taken from the documentation.
 */
public class Listener extends AbstractNodeMain {

    /**
     * Create the name for the node.
     *
     * @return the node name
     */
    @Override
    public GraphName getDefaultNodeName() {
        return new GraphName("barc/listener");
    }

    /**
     * Sets up a subscription to the 'chatter' topic and simply prints
     * out any messages it receives on this topic.
     *
     * @param node the node
     */
    @Override
    public void onStart(ConnectedNode node) {
        final Log log = node.getLog();
        Subscriber<std_msgs.String> subscriber = node.newSubscriber("chatter", std_msgs.String._TYPE);
        subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
            @Override
            public void onNewMessage(std_msgs.String msg) {
                log.info(String.format("I heard \"%s\"", msg.getData()));
            }
        });
    }
}
