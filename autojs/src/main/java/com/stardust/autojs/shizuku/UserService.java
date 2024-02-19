package com.stardust.autojs.shizuku;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.stardust.autojs.core.util.ProcessShell;
import com.stardust.autojs.runtime.api.AbstractShell;
import com.tony.shizuku.shell.plugin.IUserService;

import androidx.annotation.Keep;

public class UserService extends IUserService.Stub {

    /**
     * Constructor is required.
     */
    public UserService() {
        Log.i("UserService", "constructor");
    }

    /**
     * Constructor with Context. This is only available from Shizuku API v13.
     * <p>
     * This method need to be annotated with {@link Keep} to prevent ProGuard from removing it.
     *
     * @param context Context created with createPackageContextAsUser
     * @see <a href="https://github.com/RikkaApps/Shizuku-API/blob/672f5efd4b33c2441dbf609772627e63417587ac/server-shared/src/main/java/rikka/shizuku/server/UserService.java#L66">code used to create the instance of this class</a>
     */
    @Keep
    public UserService(Context context) {
        Log.i("UserService", "constructor with Context: context=" + context.toString());
    }

    /**
     * Reserved destroy method
     */
    @Override
    public void destroy() {
        Log.i("UserService", "destroy");
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public String execCommand(String command) throws RemoteException {
        Log.d("UserService", "execCommand: " + command);
        try {
            return ProcessShell
                    .execCommand(command.split("\n"), ProcessShell.getShellProcess())
                    .toJson();
        } catch (Exception e) {
            AbstractShell.Result result = new AbstractShell.Result();
            result.code = 1;
            result.error = e.getMessage();
            return result.toJson();
        }
    }

}
