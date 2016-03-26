/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.oceanic.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.TypeParser;
import org.openhab.binding.oceanic.OceanicBindingConstants.OceanicChannelSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link OceanicHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public class OceanicThingHandler extends SerialThingHandler {

    // List of Configuration constants
    public static final String INTERVAL = "interval";

    private Logger logger = LoggerFactory.getLogger(OceanicThingHandler.class);

    private ScheduledFuture<?> pollingJob;
    private String lastLineReceived = "";
    private long GRACE_PERIOD = 1000;
    private long REQUEST_TIMEOUT = 15000;

    public OceanicThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Oceanic handler.");
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        super.dispose();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Oceanic handler.");

        if (getConfig().get(BAUD_RATE) == null) {
            baud = 19200;
        } else {
            baud = (int) getConfig().get(BAUD_RATE);
        }

        if (getConfig().get(BUFFER_SIZE) == null) {
            bufferSize = 1024;
        } else {
            bufferSize = (int) getConfig().get(BUFFER_SIZE);
        }

        port = (String) getConfig().get(PORT);

        super.initialize();

        onUpdate();
    }

    private synchronized void onUpdate() {
        if (pollingJob == null || pollingJob.isCancelled()) {
            int polling_interval = ((BigDecimal) getConfig().get(INTERVAL)).intValue();
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 1, polling_interval, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onDataReceived(String line) {

        line = StringUtils.chomp(line);

        // little hack to overcome Locale limits of the Oceanic device
        line = line.replace(",", ".");
        line = line.trim();

        lastLineReceived = line;
    }

    private Runnable pollingRunnable = new Runnable() {

        @SuppressWarnings("unchecked")
        private State createStateForType(OceanicChannelSelector selector, String value) {

            Class<? extends Type> typeClass = selector.getTypeClass();
            List<Class<? extends State>> stateTypeList = new ArrayList<Class<? extends State>>();

            stateTypeList.add((Class<? extends State>) typeClass);

            State state = TypeParser.parseState(stateTypeList, selector.convertValue(value));

            return state;
        }

        @Override
        public void run() {
            try {

                if (getThing().getStatus() == ThingStatus.ONLINE) {
                    for (Channel aChannel : getThing().getChannels()) {
                        for (OceanicChannelSelector selector : OceanicChannelSelector.values()) {
                            ChannelUID theChannelUID = new ChannelUID(getThing().getUID(), selector.toString());
                            if (aChannel.getUID().equals(theChannelUID)
                                    && selector.getTypeValue() == OceanicChannelSelector.ValueSelectorType.GET) {
                                try {
                                    Thread.sleep(GRACE_PERIOD);
                                } catch (InterruptedException e) {
                                    logger.error("An exception occurred while putting the thread to sleep: {}",
                                            e.getMessage());
                                    e.printStackTrace();
                                }
                                String response = requestResponse(selector.name());

                                if (response != "") {
                                    State value = createStateForType(selector, response);
                                    updateState(theChannelUID, value);
                                } else {
                                    logger.warn("Received an empty answer for '{}'", selector.name());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("An exception occurred while polling the Oceanic Water Softener: '{}'", e.getMessage());
            }
        }
    };

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        String commandAsString = command.toString();
        String channelID = channelUID.getId();

        for (Channel aChannel : getThing().getChannels()) {
            if (aChannel.getUID().equals(channelUID)) {
                try {
                    OceanicChannelSelector selector = OceanicChannelSelector.getValueSelector(channelID,
                            OceanicChannelSelector.ValueSelectorType.SET);

                    switch (selector) {
                        case setSV1:
                            commandAsString = selector.name() + commandAsString;
                        default:
                            commandAsString = selector.name();
                            break;
                    }
                    String response = requestResponse(commandAsString);
                    if (response.equals("ERR")) {
                        logger.error("An error occurred while setting '{}' to {}", selector.toString(),
                                commandAsString);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warn(
                            "An error occurred while trying to set the read-only variable associated with channel '{}' to '{}'",
                            channelID, command.toString());
                }

                break;
            }
        }
    }

    private String requestResponse(String commandAsString) {
        synchronized (this) {
            lastLineReceived = "";
            String response = null;
            writeString(commandAsString + "\r");
            long timeStamp = System.currentTimeMillis();
            while (lastLineReceived.equals("")) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    logger.error("An exception occurred while putting the thread to sleep: {}", e.getMessage());
                    e.printStackTrace();
                }
                if (System.currentTimeMillis() - timeStamp > REQUEST_TIMEOUT) {
                    logger.warn("A timeout occurred while requesting data from the water softener");
                    break;
                }
            }
            response = lastLineReceived;
            return response;
        }
    }
}