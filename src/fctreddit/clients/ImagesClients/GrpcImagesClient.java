package fctreddit.clients.ImagesClients;

import fctreddit.api.java.Result;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import io.grpc.LoadBalancerRegistry;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import java.util.logging.Logger;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Channel;
import io.grpc.StatusRuntimeException;
import io.grpc.Status;

import java.net.URI;

public class GrpcImagesClient extends ImagesClient{

    static {
        LoadBalancerRegistry.getDefaultRegistry().register(new PickFirstLoadBalancerProvider());
    }

    final ImageGrpc.ImageBlockingStub stub;
    private static final Logger Log = Logger.getLogger(GrpcImagesClient.class.getName());

    public GrpcImagesClient(URI  serverURI) {
        Channel channel = ManagedChannelBuilder.forAddress(serverURI.getHost(), serverURI.getPort())
                .enableRetry().usePlaintext().build();
        stub = ImageGrpc.newBlockingStub(channel);
    }

    @Override
    public Result<Void> deleteImage(String userId, String imageId, String password) {
        try {
            Log.info("Delete image request: " + userId + " " + imageId + " " + password);
            ImageProtoBuf.EmptyMessage res = stub.deleteImage(ImageProtoBuf.DeleteImageArgs.newBuilder()
                    .setUserId(userId)
                    .setImageId(imageId)
                    .setPassword(password)
                    .build());
            Log.info("Delete image worked");
            return Result.ok(null);
        }
        catch (StatusRuntimeException sre) {
            Log.info("Delete image failed: " + statusToErrorCode(sre.getStatus()));
            return Result.error( statusToErrorCode(sre.getStatus()) );
        }
    }

    static Result.ErrorCode statusToErrorCode(Status status) {
        return switch(status.getCode()) {
            case OK -> Result.ErrorCode.OK;
            case NOT_FOUND -> Result.ErrorCode.NOT_FOUND;
            case ALREADY_EXISTS -> Result.ErrorCode.CONFLICT;
            case PERMISSION_DENIED -> Result.ErrorCode.FORBIDDEN;
            case INVALID_ARGUMENT -> Result.ErrorCode.BAD_REQUEST;
            case UNIMPLEMENTED -> Result.ErrorCode.NOT_IMPLEMENTED;
            default -> Result.ErrorCode.INTERNAL_ERROR;

        };
    }



}
