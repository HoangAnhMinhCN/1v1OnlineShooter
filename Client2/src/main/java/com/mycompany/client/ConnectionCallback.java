package com.mycompany.client;

public interface ConnectionCallback {
    void onConnectSuccess();
    void onConnectFailure(String errorMessage);
}
