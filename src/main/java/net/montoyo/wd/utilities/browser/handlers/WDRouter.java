package net.montoyo.wd.utilities.browser.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.browser.handlers.js.JSQueryHandler;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WDRouter extends CefMessageRouterHandlerAdapter {
    public static final WDRouter INSTANCE = new WDRouter();

    private static boolean exists = false;

    public WDRouter() {
        if (exists) throw new RuntimeException("Can only have one WD message router.");
        exists = true;
    }

    class QueryData {
        CefBrowser browser;
        String type;
        BiConsumer<String, CefQueryCallback> consumer;

        public QueryData(CefBrowser browser, String type, BiConsumer<String, CefQueryCallback> consumer) {
            this.browser = browser;
            this.type = type;
            this.consumer = consumer;
        }
    }

    ArrayList<QueryData> awaitingQueries = new ArrayList<>();

    @Override
    public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {
        if (request.startsWith("WebDisplays_")) {
            request = request.substring("Webdisplays_".length());

            QueryData target = null;
            for (QueryData awaitingQuery : awaitingQueries) {
                if (browser != awaitingQuery.browser) continue;

                if (request.startsWith(awaitingQuery.type)) {
                    String requestData = request.substring(awaitingQuery.type.length());
                    target = awaitingQuery;
                    awaitingQuery.consumer.accept(requestData, callback);
                    break;
                }
            }

            if (target != null) {
                awaitingQueries.remove(target);
                callback.success("");
            } else {
                if (browser instanceof WDBrowser wdBrowser) {
                    Map<String, JSQueryHandler> handlerMap = wdBrowser.queryHandlers();

                    int i0 = request.indexOf('('); // legacy, TODO: support
                    int i1 = request.indexOf('{');
                    if (i0 == -1) i0 = i1;
                    if (i1 == -1) i1 = i0;

                    if (i1 == -1) {
                        if (handlerMap.containsKey(request)) {
                            if (!handlerMap.get(request).handle(browser, frame, null, persistent, callback)) {
                                callback.failure(-1, "Query " + queryId + " with data " + request + " completed, but wasn't marked as successful.");
                            }
                        }
                    } else {
                        int min = Math.min(i0, i1);
                        String text = request.substring(0, min);
                        if (handlerMap.containsKey(text)) {
                            JsonObject obj = null;
                            if (request.charAt(min) == '{')
                                obj = gson.fromJson(request.substring(min), JsonObject.class);

                            if (!handlerMap.get(text).handle(browser, frame, obj, persistent, callback)) {
                                callback.failure(-1, "Query " + queryId + " with data " + request + " completed, but wasn't marked as successful.");
                            }
                        }
                        callback.failure(-1, "Query " + queryId + " with data " + request + " completed, but there was no active request waiting for the result.");
                    }
                }
            }

            return true;
        }
        return false;
    }

    private static final Gson gson = new Gson();

    public class Task<T> {
        QueryData qd;
        CompletableFuture<T> wrapped;

        public Task(QueryData qd, CompletableFuture<T> wrapped) {
            this.qd = qd;
            this.wrapped = wrapped;
        }

        public void cancel() {
            wrapped.cancel(true);
            awaitingQueries.remove(qd);
        }

        public Task<T> thenAccept(Consumer<T> consumer) {
            wrapped.thenAccept(consumer);
            return this;
        }
    }

    public Task<JsonObject> requestJson(CefBrowser screen, String queryType, String script) {
        JsonObject[] obj = new JsonObject[1];

        QueryData qd = new QueryData(
                screen, queryType,
                (data, context) -> {
                    obj[0] = gson.fromJson(data, JsonObject.class);
                }
        );
        awaitingQueries.add(qd);

        screen.executeJavaScript(script, "", 0);

        return new Task<>(
                qd,
                CompletableFuture.supplyAsync(() -> {
                    while (obj[0] == null) {
                        try {
                            Thread.sleep(1);
                        } catch (Throwable ignored) {
                        }
                    }
                    return obj[0];
                })
        );
    }
}
