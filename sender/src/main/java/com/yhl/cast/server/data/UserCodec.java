package com.yhl.cast.server.data;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.yhl.cast.server.proto.UserProto;
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
        if (msg instanceof UserInfo) {
            UserInfo userInfo = (UserInfo) msg;
            UserProto.User.Builder builder = UserProto.User.newBuilder();
            builder.setAge(userInfo.getAge()).setEmail(userInfo.getEmail()).setName(userInfo.getName()).setPhone(userInfo.getPhone()).setSex(userInfo.getSex());
            UserProto.User userProto = builder.build();
            return userProto.toByteArray();
        }
        return new byte[0];
    }

    @NotNull
    @Override
    public UserInfo decode(@NotNull byte[] data) {
        try {
            UserProto.User userProto = UserProto.User.getDefaultInstance().getParserForType().parseFrom(data);
            UserInfo userInfo = new UserInfo();
            userInfo.setName(userProto.getName());
            userInfo.setEmail(userProto.getEmail());
            userInfo.setPhone(userProto.getPhone());
            userInfo.setAge(userProto.getAge());
            userInfo.setSex(userProto.getSex());
            return userInfo;
        } catch (InvalidProtocolBufferException e) {
            Log.i("UserCodec1", "decode: error = " + e.getMessage());
        }
        return null;
    }
}
