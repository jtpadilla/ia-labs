package io.helidon.example.grpc.client;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.jspecify.annotations.NonNull;

public class ApiKeyClientInterceptor implements ClientInterceptor {

    private final String apiKey;

    public ApiKeyClientInterceptor(String apiKey) {
        this.apiKey = apiKey;
    }

    public static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, ResT> ClientCall<ReqT, ResT> interceptCall(
            @NonNull MethodDescriptor<ReqT, ResT> method,
            @NonNull CallOptions callOptions,
            @NonNull Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, ResT>(next.newCall(method, callOptions)) {
            @Override
            public void start(@NonNull Listener<ResT> responseListener, @NonNull Metadata headers) {
                headers.put(API_KEY_HEADER, apiKey);
                super.start(responseListener, headers);
            }
        };
    }
}
