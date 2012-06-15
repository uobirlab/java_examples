package org.barc;

import barc.subsumption.Behavior;
import barc.subsumption.Subsumption;
import com.google.common.base.Preconditions;
import geometry_msgs.Twist;
import org.apache.commons.logging.Log;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.parameter.ParameterTree;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import sensor_msgs.LaserScan;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * This class shows the usage of the subsumption architecture. Currently, the
 * subsumption architecture creates all behaviors at a single level. Future work
 * will hopefully allow for tiered reactive machines.
 *
 * @author jxv911@cs.bham.ac.uk (Jeremiah Via)
 * @see Subsumption
 */
public class ReactiveMover extends AbstractNodeMain {

    private enum Direction {LEFT, RIGHT}

    private Direction direction;
    private double maxScanHalf, left, right;

    @Override
    public GraphName getDefaultNodeName() {
        return new GraphName("reactive_mover");
    }

    @Override
    public void onStart(ConnectedNode node) {
        final Log log = node.getLog();

        /*
         Get settings from parameter server:

         This allows for more flexible nodes because settings can be changed
         in configuration files without the need to recompile the code.
         */
        ParameterTree params = node.getParameterTree();
        final String laserTopic = params.getString("laser_topic", "base_scan");
        final String twistTopic = params.getString("twist_topic", "cmd_vel");
        final double forwardSpeed = params.getDouble("forward_speed", 0.1);
        final double turningSpeed = params.getDouble("turning_speed", 0.5);

        /*
        Set up subscriber:

        The subscriber will do some simple processing for us. It will determine
        which direction has more free space. Notice that with the lasers we use,
        the value will be 0.0 if the distance being measured is beyond its maximum
        distance, hence we simply make it the max distance because we want to head
        towards open space.
        */
        final Subscriber<LaserScan> laserSubscriber = node.newSubscriber(laserTopic, LaserScan._TYPE);
        laserSubscriber.addMessageListener(new MessageListener<LaserScan>() {
            @Override
            public void onNewMessage(LaserScan scan) {
                for (int i = 0; i < scan.getRanges().length; i++) {
                    double val = (scan.getRanges()[i] == 0.0) ? scan.getRangeMax() : scan.getRanges()[i];
                    if (i < scan.getRanges().length / 2)
                        left += val;
                    else
                        right += val;
                }

                direction = (left > right) ? Direction.LEFT : Direction.RIGHT;
                maxScanHalf = scan.getRangeMax() * (scan.getRanges().length / 2);
            }
        });

        /*
        Setup publisher:

        Our publisher will publish movement commands to the robot, known as
        Twist messages.
        */
        final Publisher<Twist> twistPublisher = node.newPublisher(twistTopic, Twist._TYPE);

        /*
        Setup behaviors:

        We will do this in place since the behaviors are so simple. There are only
        two behaviors which we need to create, a left arc and a right arc. The arbitrator
        will run through these two behaviors and execute whichever one has its criterion
        for execution met.
        */
        List<Behavior> behaviors = new ArrayList<>();

        // Create & add left turning behavior
        behaviors.add(new Behavior() {
            @Override
            public boolean canRun() {
                return direction == Direction.LEFT;
            }

            @Override
            public void run() {
                Preconditions.checkState(left <= maxScanHalf, "Left scan must be <= to the max value");
                double percent = left / maxScanHalf;

                Twist twistLeft = twistPublisher.newMessage();
                twistLeft.getLinear().setX(0.1);
                twistLeft.getAngular().setZ(-(percent * 0.5));

                log.debug(format("Turning left @  %s", twistLeft));
                twistPublisher.publish(twistLeft);
            }

            // Nothing to do
            @Override
            public void stop() { }
        });

        // Create & add right turning behavior
        behaviors.add(new Behavior() {
            @Override
            public boolean canRun() {
                return direction == Direction.RIGHT;
            }

            @Override
            public void run() {
                Preconditions.checkState(left <= maxScanHalf, "Right scan must be <= to the max value");
                double percent = right / maxScanHalf;

                Twist twistRight = twistPublisher.newMessage();
                twistRight.getLinear().setX(0.1);
                twistRight.getAngular().setZ(percent * 0.5);

                log.debug(format("Turning right @  %s", twistRight));
                twistPublisher.publish(twistRight);
            }

            // Nothing to do
            @Override
            public void stop() { }
        });

        // Start arbitrator in another thread
        new Thread(new Subsumption(behaviors)).start();
    }
}
