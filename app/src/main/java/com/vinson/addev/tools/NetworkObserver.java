package com.vinson.addev.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.util.Log;


import com.vinson.addev.App;

import static android.content.Context.CONNECTIVITY_SERVICE;

public class NetworkObserver {
    private static final String TAG = NetworkObserver.class.getSimpleName();
    private Listener mListener;
    private boolean mNetworkAvailable;
    private ConnectivityManager mConnectivityManager;

    public NetworkObserver(Context context) {
        mConnectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        assert mConnectivityManager != null;
    }

    public void register(Listener l) {
        mListener = l;
        NetworkInfo networkInfo = mConnectivityManager.getActiveNetworkInfo();
        mNetworkAvailable = (networkInfo != null && networkInfo.isConnected());
        mConnectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), mNetworkCallback);
    }

    public void unregister() {
        if (mListener == null) return;
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        mListener = null;
    }

    public boolean disconnected() {
        return !mNetworkAvailable;
    }

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            Log.w(TAG, "network available, prev available:" + mNetworkAvailable);
            boolean prev = mNetworkAvailable;
            mNetworkAvailable = true;
            App.getInstance().showToast("网络已连接");
            if (mListener != null)
                mListener.changed(true);
        }

        @Override
        public void onLost(Network network) {
            Log.w(TAG, "network lost, prev available:" + mNetworkAvailable);
            boolean prev = mNetworkAvailable;
            mNetworkAvailable = false;
            App.getInstance().showToast("网络已断开");
            if (mListener != null)
                mListener.changed(false);
        }
    };

    public interface Listener {
        void changed(boolean connected);
    }
}
