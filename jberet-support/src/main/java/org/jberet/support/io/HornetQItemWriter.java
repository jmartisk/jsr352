/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

import java.io.Serializable;
import java.util.List;
import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.ItemWriter;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.hornetq.api.core.HornetQException;
import org.hornetq.api.core.client.ClientMessage;
import org.hornetq.api.core.client.ClientProducer;
import org.jberet.support._private.SupportLogger;

/**
 * An implementation of {@code javax.batch.api.chunk.ItemWriter} that sends data items to a HornetQ address.
 *
 * @see HornetQItemReader
 * @since 1.1.0
 */
@Named
@Dependent
public class HornetQItemWriter extends HornetQItemReaderWriterBase implements ItemWriter {
    @Inject
    @BatchProperty
    protected boolean durableMessage;

    protected ClientProducer producer;

    @Override
    public void open(final Serializable checkpoint) throws Exception {
        super.open(checkpoint);
        producer = session.createProducer(queueAddress);
    }

    @Override
    public void writeItems(final List<Object> items) throws Exception {
        for (final Object item : items) {
            final ClientMessage msg;
            if (item instanceof ClientMessage) {
                msg = (ClientMessage) item;
            } else if (item instanceof String) {
                msg = session.createMessage(ClientMessage.TEXT_TYPE, durableMessage);
                msg.getBodyBuffer().writeString((String) item);
            } else {
                msg = session.createMessage(ClientMessage.OBJECT_TYPE, durableMessage);
                msg.getBodyBuffer().writeBytes(objectToBytes(item));
            }
            producer.send(msg);
        }
    }

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() {
        super.close();
        if (producer != null) {
            try {
                producer.close();
            } catch (final HornetQException e) {
                SupportLogger.LOGGER.tracef(e, "Failed to close HornetQ consumer %s%n", producer);
            }
            producer = null;
        }
    }
}