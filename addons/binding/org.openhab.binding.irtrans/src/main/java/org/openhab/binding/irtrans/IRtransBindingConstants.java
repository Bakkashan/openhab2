/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.irtrans;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IRtransBindingConstants} contains constants used by the IRtrans
 * handler classes
 *
 * @author Karel Goderis - Initial contribution
 * @since 2.0.0
 *
 **/
public class IRtransBindingConstants {

    public static final String BINDING_ID = "irtrans";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_ETHERNET_BRIDGE = new ThingTypeUID(BINDING_ID, "ethernet");
    public final static ThingTypeUID THING_TYPE_BLASTER = new ThingTypeUID(BINDING_ID, "blaster");

    // List of all Channel ids
    public final static String CHANNEL_IO = "io";

    // List of all Channel types
    public static final String BLASTER_CHANNEL_TYPE = "Blaster";
    public static final String RECEIVER_CHANNEL_TYPE = "Receiver";

    // List of possible leds on a IRtrans transceiver
    public enum Led {
        DEFAULT("D"),
        INTERNAL("I"),
        EXTERNAL("E"),
        ALL("B"),
        ONE("1"),
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6"),
        SEVEN("7"),
        EIGHT("8");

        private final String text;

        private Led(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static Led get(String valueSelectorText) throws IllegalArgumentException {

            for (Led c : Led.values()) {
                if (c.text.equals(valueSelectorText)) {
                    return c;
                }
            }

            throw new IllegalArgumentException("Not valid value selector");
        }

    }

}