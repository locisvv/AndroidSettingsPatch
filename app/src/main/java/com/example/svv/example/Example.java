package com.example.svv.example;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static de.robv.android.xposed.XposedHelpers.*;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Example implements IXposedHookLoadPackage {
    private static final String SHOULD_NOT_RESOLVE = "SHOULDN'T RESOLVE!";
    private static final String KEY_CALLER_IDENTITY = "pendingIntent";

    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.settings"))
            return;

        final Class<?> clazz = findClass("com.android.settings.accounts.AddAccountSettings", lpparam.classLoader);

        final Method method2 = findMethodBestMatch(clazz, "addAccount", String.class);

        XposedBridge.hookMethod(method2, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod (MethodHookParam param) throws Throwable {
                XposedBridge.log("----- Started hooking addAccount------");

                Context context = (Context) param.thisObject;
                PendingIntent mPendingIntent1 = PendingIntent.getBroadcast(context, 0, new Intent(), 0);

                Intent identityIntent = new Intent();
                identityIntent.setComponent(new ComponentName(SHOULD_NOT_RESOLVE, SHOULD_NOT_RESOLVE));
                identityIntent.setAction(SHOULD_NOT_RESOLVE);
                identityIntent.addCategory(SHOULD_NOT_RESOLVE);

                String accountType = (String) param.args[0];
                XposedBridge.log("----- account type: " + accountType);

                AccountManagerCallback<Bundle> mCallback = (AccountManagerCallback<Bundle>) XposedHelpers
                        .findField(param.thisObject.getClass(), "mCallback").get(context);

                PendingIntent mPendingIntent = PendingIntent.getBroadcast(context, 0, identityIntent, 0);

                if (mPendingIntent.equals(mPendingIntent1)) {
                    XposedBridge.log("----- equels ------");
                }

                Bundle addAccountOptions = new Bundle();
                addAccountOptions.putParcelable(KEY_CALLER_IDENTITY, mPendingIntent);
                AccountManager.get(context).addAccount(
                        accountType,
                        null, /* authTokenType */
                        null, /* requiredFeatures */
                        addAccountOptions,
                        null,
                        mCallback,
                        null /* handler */);

                XposedHelpers.findField(param.thisObject.getClass(), "mAddAccountCalled").setBoolean(context, true);
                param.setResult(null);
            }
        });
    }
}