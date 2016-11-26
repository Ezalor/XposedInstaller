package de.robv.android.xposed.installer.util;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipFile;

import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.installation.FlashCallback;

public final class InstallZipUtil {
    private InstallZipUtil() {}

    public static ZipCheckResult checkZip(String zipPath) {
        ZipFile zip;
        try {
            zip = new ZipFile(zipPath);
        } catch (IOException e) {
            return new ZipCheckResult();
        }

        ZipCheckResult result = checkZip(zip);
        closeSilently(zip);
        return result;
    }

    public static ZipCheckResult checkZip(ZipFile zip) {
        ZipCheckResult result = new ZipCheckResult();

        // Check for update-binary.
        if (zip.getEntry("META-INF/com/google/android/update-binary") == null) {
            return result;
        }

        result.mValidZip = true;

        // Check whether the file can be flashed directly in the app.
        if (zip.getEntry("META-INF/com/google/android/flash-script.sh") != null) {
            result.mFlashableInApp = true;
        }

        return result;
    }

    public static XposedProp parseXposedProp(InputStream is) throws IOException {
        XposedProp prop = new XposedProp();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                continue;
            }

            String key = parts[0].trim();
            if (key.charAt(0) == '#') {
                continue;
            }

            String value = parts[1].trim();

            if (key.equals("version")) {
                prop.mVersion = value;
                prop.mVersionInt = ModuleUtil.extractIntPart(value);
            } else if (key.equals("arch")) {
                prop.mArch = value;
            } else if (key.equals("minsdk")) {
                prop.mMinSdk = Integer.parseInt(value);
            } else if (key.equals("maxsdk")) {
                prop.mMaxSdk = Integer.parseInt(value);
            }
        }
        reader.close();
        return prop.isComplete() ? prop : null;
    }

    public static String messageForError(int code, Object... args) {
        Context context = XposedApp.getInstance();
        switch (code) {
            case FlashCallback.ERROR_TIMEOUT:
                return context.getString(R.string.flash_error_timeout);

            case FlashCallback.ERROR_SHELL_DIED:
                return context.getString(R.string.flash_error_shell_died);

            case FlashCallback.ERROR_NO_ROOT_ACCESS:
                return context.getString(R.string.root_failed);

            case FlashCallback.ERROR_INVALID_ZIP:
                String message = context.getString(R.string.flash_error_invalid_zip);
                if (args.length > 0) {
                    message += "\n" + args[0];
                }
                return message;

            case FlashCallback.ERROR_NOT_FLASHABLE_IN_APP:
                return context.getString(R.string.flash_error_not_flashable_in_app);

            default:
                return context.getString(R.string.flash_error_default, code);
        }
    }

    public static void triggerError(FlashCallback callback, int code, Object... args) {
        callback.onError(code, messageForError(code, args));
    }

    public static void closeSilently(ZipFile z) {
        try {
            z.close();
        } catch (IOException ignored) {}
    }

    public static class ZipCheckResult {
        private boolean mValidZip = false;
        private boolean mFlashableInApp = false;

        public boolean isValidZip() {
            return mValidZip;
        }

        public boolean isFlashableInApp() {
            return mFlashableInApp;
        }
    }

    public static class XposedProp {
        private String mVersion = null;
        private int mVersionInt = 0;
        private String mArch = null;
        private int mMinSdk = 0;
        private int mMaxSdk = 0;

        private boolean isComplete() {
            return mVersion != null
                    && mVersionInt > 0
                    && mArch != null
                    && mMinSdk > 0
                    && mMaxSdk > 0;
        }

        public String getVersion() {
            return mVersion;
        }

    }
}
