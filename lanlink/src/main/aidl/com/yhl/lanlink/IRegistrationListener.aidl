package com.yhl.lanlink;

interface IRegistrationListener {
    /**
     * 服务注册事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    void onServiceRegistered(int resultCode);

    /**
     * 服务注销事件回调
     * @param resultCode {@link RESULT_SUCCESS} ：注册成功，
     *                  <br>{@link RESULT_FAILED} ：注册失败
     */
    void onServiceUnregistered(int resultCode);
}
