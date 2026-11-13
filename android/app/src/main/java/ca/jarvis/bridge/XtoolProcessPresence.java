package ca.jarvis.bridge;

import android.app.ActivityManager;
import android.content.Context;
import java.util.List;

/**
 * Best-effort *read-only* view of xTool's process presence.
 * No new permissions, no continuous recording, no interaction with xTool.
 *
 * Some Android builds restrict visibility of other applications' processes.
 * A negative result is never trusted unless this Jarvis session first saw
 * xTool's process alive. Null means the OS did not provide usable data.
 */
final class XtoolProcessPresence {
    private static final String XTOOL_PACKAGE = "com.makeblock.xcs";

    private XtoolProcessPresence() { }

    static Boolean running(Context context) {
        try {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null) return null;
            List<ActivityManager.RunningAppProcessInfo> processes =
                    manager.getRunningAppProcesses();
            if (processes == null) return null;
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process == null) continue;
                if (process.processName != null
                        && (XTOOL_PACKAGE.equals(process.processName)
                            || process.processName.startsWith(XTOOL_PACKAGE + ":")))
                    return true;
                if (process.pkgList != null) {
                    for (String pkg : process.pkgList) {
                        if (XTOOL_PACKAGE.equals(pkg)) return true;
                    }
                }
            }
            return false;
        } catch (SecurityException | IllegalArgumentException unavailable) {
            return null;
        }
    }
}
