package com.yhl.cast.client.data;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yhl.cast.client.proto.UserProto;
import com.yhl.lanlink.interfaces.MessageCodec;

import org.jetbrains.annotations.NotNull;

public class UserCodec extends MessageCodec {

    @NotNull
    @Override
    public String getMessageType() {
        return "user-info";
    }

    @NotNull
    @Override
    public byte[] encode(@NotNull Object msg) {
        if (msg instanceof User) {
            User user = (User) msg;
            UserProto.User.Builder builder = UserProto.User.newBuilder();
            builder.setAge(user.getAge()).setEmail(user.getEmail()).setName(user.getName()).setPhone(user.getPhone()).setSex(user.getSex());
            UserProto.User userProto = builder.build();
            return userProto.toByteArray();
        }
        return new byte[0];
    }

    @NotNull
    @Override
    public User decode(@NotNull byte[] data) {
        try {
            UserProto.User userProto = UserProto.User.getDefaultInstance().getParserForType().parseFrom(data);
            User user = new User();
            user.setName(userProto.getName());
            user.setEmail(userProto.getEmail());
            user.setPhone(userProto.getPhone());
            user.setAge(userProto.getAge());
            user.setSex(userProto.getSex());
            return user;
        } catch (InvalidProtocolBufferException e) {
            Log.i("UserCodec", "decode: error = " + e.getMessage());
        }
        return null;
    }
}
