package fctreddit.clients.ImagesClients;

import com.google.protobuf.ByteString;
import fctreddit.api.java.Result;

public abstract class ImagesClient {

    public abstract Result<Void> deleteImage(String userId, String imageId, String password);

}
