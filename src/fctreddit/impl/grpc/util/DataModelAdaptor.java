package fctreddit.impl.grpc.util;

import fctreddit.api.User;
import fctreddit.api.java.Image;
import fctreddit.impl.grpc.generated_java.ImageGrpc;
import fctreddit.impl.grpc.generated_java.ImageProtoBuf;
import fctreddit.impl.grpc.generated_java.UsersProtoBuf;

public class DataModelAdaptor {

    public static User GrpcUser_to_User(UsersProtoBuf.GrpcUser from) {
        return new User(
                from.getUserId(),
                from.getFullName(),
                from.getEmail(),
                from.getPassword());
    }

    public static UsersProtoBuf.GrpcUser User_to_GrpcUser(User from) {
        UsersProtoBuf.GrpcUser.Builder b = UsersProtoBuf.GrpcUser.newBuilder()
                .setUserId(from.getUserId())
                .setPassword(from.getPassword())
                .setEmail(from.getEmail())
                .setFullName(from.getFullName());

        return b.build();
    }

}
