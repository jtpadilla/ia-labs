package io.helidon.example.grpc.server;

import io.grpc.*;
import org.jspecify.annotations.NonNull;

public class ApiKeyInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> API_KEY_HEADER =
            Metadata.Key.of("x-api-key", Metadata.ASCII_STRING_MARSHALLER);

    public static final String VALID_API_KEY = "my-secret-api-key";

    @Override
    public <ReqT, ResT> ServerCall.Listener<ReqT> interceptCall(
            @NonNull ServerCall<ReqT, ResT> call,
            @NonNull Metadata headers,
            @NonNull ServerCallHandler<ReqT, ResT> next) {

        String apiKey = headers.get(API_KEY_HEADER);

        if (VALID_API_KEY.equals(apiKey)) {
            return next.startCall(call, headers);
        } else {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing API KEY"), new Metadata());
            return new ServerCall.Listener<ReqT>() {};
        }
    }
}
