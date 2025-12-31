/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.miniserv.client.ClientTaskGetFile;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.serialization.Util;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandler;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class WDScheme implements CefResourceHandler {

    private static final String ERROR_PAGE = "<!DOCTYPE html><html><head></head><body><h1>%d %s</h1><hr /><i>Miniserv powered by WebDisplays</i></body></html>";
    private ClientTaskGetFile task;
    private boolean isErrorPage;

    String url;
    boolean onlyError = false;

    public WDScheme(String url) {
        this.url = url;
    }

    @Override
    public boolean processRequest(CefRequest cefRequest, CefCallback cefCallback) {
        url = cefRequest.getURL();

        url = url.substring("webdisplays://".length());

        int pos = url.indexOf('/');
        if (pos < 0)
            return false;

        String uuidStr = url.substring(0, pos);
        String fileStr = url.substring(pos + 1);

        fileStr = URLDecoder.decode(fileStr, StandardCharsets.UTF_8);

        if (uuidStr.isEmpty() || Util.isFileNameInvalid(fileStr)) {
            // invalid URL or no UUID
            onlyError = true;
            cefCallback.Continue();
            return true;
        }

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException ex) {
            // invalid UUID
            onlyError = true;
            cefCallback.Continue();
            return true;
        }

        task = new ClientTaskGetFile(uuid, fileStr);
        boolean doContinue = Client.getInstance().addTask(task);
        if (doContinue) cefCallback.Continue();
        return doContinue;
    }

    @Override
    public void getResponseHeaders(CefResponse cefResponse, IntRef contentLength, StringRef redir) {
        int status;
        if (onlyError) {
            status = Constants.GETF_STATUS_BAD_NAME;
        } else {
            Log.info("Waiting for response...");
            status = task.waitForResponse();
            Log.info("Got response %d", status);

            if (status == 0) {
                //OK
                int extPos = task.getFileName().lastIndexOf('.');
                if (extPos >= 0) {
                    String mime = mapMime(task.getFileName().substring(extPos + 1));

                    if (mime != null)
                        cefResponse.setMimeType(mime);
                }

                cefResponse.setStatus(200);
                cefResponse.setStatusText("OK");
                contentLength.set(0);
                return;
            }
        }

        int errCode;
        String errStr;

        if (status == Constants.GETF_STATUS_NOT_FOUND) {
            errCode = 404;
            errStr = "Not Found";
        } else if (status == Constants.GETF_STATUS_TIMED_OUT) {
            errCode = 408;
            errStr = "Timed Out";
        } else if (status == Constants.GETF_STATUS_BAD_NAME) {
            errCode = 418;
            errStr = "I'm a teapot";
        } else {
            errCode = 500;
            errStr = "Internal Server Error";
        }

        // reporting the actual status and text makes CEF not display the page
        cefResponse.setStatus(200);
        cefResponse.setStatusText("OK");
        cefResponse.setMimeType("text/html");

        dataToWrite = String.format(ERROR_PAGE, errCode, errStr).getBytes(StandardCharsets.UTF_8);
        dataOffset = 0;
        amountToWrite = dataToWrite.length;
        isErrorPage = true;
        contentLength.set(0);
    }

    private byte[] dataToWrite;
    private int dataOffset;
    private int amountToWrite;

    @Override
    public boolean readResponse(byte[] output, int bytesToRead, IntRef bytesRead, CefCallback cefCallback) {
        if (dataToWrite == null) {
            if (isErrorPage) {
                bytesRead.set(0);
                return false;
            }

            dataToWrite = task.waitForData();
            dataOffset = 3; //packet ID + size
            amountToWrite = task.getDataLength();

            if (amountToWrite <= 0) {
                dataToWrite = null;
                bytesRead.set(0);
                return false;
            }
        }

        int toWrite = bytesToRead;
        if (toWrite > amountToWrite)
            toWrite = amountToWrite;

        System.arraycopy(dataToWrite, dataOffset, output, 0, toWrite);
        bytesRead.set(toWrite);

        dataOffset += toWrite;
        amountToWrite -= toWrite;

        if (amountToWrite <= 0) {
            if (!isErrorPage)
                task.nextData();

            dataToWrite = null;
        }

        return true;
    }

    @Override
    public void cancel() {
        Log.info("Scheme query canceled or finished.");
        if (!onlyError)
            task.cancel();
    }

    public static String mapMime(String ext) {
        switch (ext) {
            case "htm":
            case "html":
                return "text/html";

            case "css":
                return "text/css";

            case "js":
                return "text/javascript";

            case "png":
                return "image/png";

            case "jpg":
            case "jpeg":
                return "image/jpeg";

            case "gif":
                return "image/gif";

            case "svg":
                return "image/svg+xml";

            case "xml":
                return "text/xml";

            case "txt":
                return "text/plain";

            default:
                return null;
        }
    }
}
