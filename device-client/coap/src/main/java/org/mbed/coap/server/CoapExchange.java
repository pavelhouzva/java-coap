/**
 * Copyright (C) 2011-2014 ARM Limited. All rights reserved.
 */
package org.mbed.coap.server;

import org.mbed.coap.CoapPacket;
import org.mbed.coap.CoapUtils;
import org.mbed.coap.Code;
import org.mbed.coap.ExHeaderOptions;
import org.mbed.coap.MessageType;
import org.mbed.coap.Method;
import org.mbed.coap.exception.CoapException;
import org.mbed.coap.transport.TransportContext;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author szymon
 */
public abstract class CoapExchange {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoapExchange.class);
    protected CoapPacket request;
    protected CoapPacket response;
    private boolean isDelayedResponse;

    public CoapExchange(CoapPacket request, CoapPacket response) {
        this.request = request;
        this.response = response;
    }

    public CoapPacket getRequest() {
        return request;
    }

    protected CoapPacket getResponse() {
        return response;
    }

    /**
     * Returns request method (GET, PUT, POST, DELETE)
     *
     * @return method
     */
    public Method getRequestMethod() {
        return request.getMethod();
    }

    /**
     * Returns requested URI path
     *
     * @return uri path
     */
    public String getRequestUri() {
        return request.headers().getUriPath();
    }

    /**
     * Returns request coap headers
     */
    public ExHeaderOptions getRequestHeaders() {
        return request.headers();
    }

    public byte[] getRequestBody() {
        return request.getPayload();
    }

    /**
     * Returns request body
     */
    public String getRequestBodyString() {
        return request.getPayloadString();
    }

    /**
     * Returns source address of incoming message
     */
    public abstract InetSocketAddress getRemoteAddress();

    /**
     * Returns response headers that will be sent to requester
     */
    public ExHeaderOptions getResponseHeaders() {
        return response.headers();
    }

    public abstract void setResponseBody(byte[] payload);

    /**
     * Sets response content type of a body
     */
    public abstract void setResponseContentType(short contentType);

    public abstract void setResponseToken(byte[] token);

    public abstract byte[] getResponseToken();

    /**
     * Sets response body
     *
     * @param body response body
     */
    public void setResponseBody(String body) {
        setResponseBody(CoapUtils.encodeString(body));
    }

    /**
     * Sets response CoAP code
     */
    public abstract void setResponseCode(Code code);

    public abstract void setResponse(CoapPacket message);

    /**
     * Sends CoAP reset response
     */
    public void sendResetResponse() {
        response = request.createResponse();
        response.setMessageType(MessageType.Reset);
        response.setCode(null);
        this.getCoapServer().sendResponse(this);
        response = null;
        //request = null;
    }
    //private CoapPacket oldResp = null;

    /**
     * Sends response, this method must be called only once at the end of
     * request handling. No operations are allowed on this object after.
     */
    public void sendResponse() {
        if (!isDelayedResponse) {
            if (request.getMessageType() == MessageType.NonConfirmable && request.getMethod() == null) {
                LOGGER.trace("Send response ignored for NON response");
            } else {
                send();
            }
            response = null;
        } else {
            try {
                this.getCoapServer().makeRequest(response, CoapUtils.getCallbackNull());
            } catch (CoapException ex) {
                LOGGER.warn("Error while sending delayed response: " + ex.getMessage());
            }
        }
    }

    public abstract CoapServer getCoapServer();

    /**
     * Sends empty ACK to server telling that response will come later on. If
     * request wan NON, then will not send anything
     */
    public void sendDelayedAck() {
        if (request.getMessageType() == MessageType.NonConfirmable) {
            return;
        }
        CoapPacket emptyAck = new CoapPacket();
        emptyAck.setCode(null);
        emptyAck.setMethod(null);
        emptyAck.setMessageType(MessageType.Acknowledgement);
        emptyAck.setMessageId(getRequest().getMessageId());

        CoapPacket tmpResp = this.getResponse();
        this.setResponse(emptyAck);

        //this.send();
        this.getCoapServer().sendResponse(this);

        this.setResponse(tmpResp);
        tmpResp.setMessageType(MessageType.Confirmable);
        isDelayedResponse = true;
    }

    protected abstract void send();

    /**
     * Returns request transport context.
     *
     * @return transport context or null if does not exist.
     */
    public abstract TransportContext getRequestTransportContext();

    public abstract TransportContext getResponseTransportContext();

    public abstract void setResponseTransportContext(TransportContext responseTransportContext);

    @Override
    public String toString() {
        return "CoapExchange [" + "request=" + request + ", response=" + response + ']';
    }
}