package ca.mlaflamme.clocktime.notification;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

public class NotificationInfo {
    Drawable mDrawable;
    String mId;

    public NotificationInfo(Context ctx, String pkg, Notification notification) throws PackageManager.NameNotFoundException, IconNotFoundException {

        Context remoteCtx = ctx.createPackageContext(pkg, 0);
        try {
            mDrawable = ContextCompat.getDrawable(remoteCtx, notification.icon);
        }catch (Resources.NotFoundException ignored){

        }

        if(mDrawable==null){
            throw new IconNotFoundException();
        }
        mId=pkg;
    }
    public NotificationInfo(Context ctx, int iconResId){
        mId="notificationInternal"+iconResId;
        mDrawable=ContextCompat.getDrawable(ctx, iconResId);
    }

    public Drawable getDrawable() {
        return mDrawable;
    }

    public String getId() {
        return mId;
    }

    public View generateView(Context context){
        ImageView img = new ImageView(context);
        img.setImageDrawable(getDrawable());
        return img;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationInfo that = (NotificationInfo) o;

        return mId.equals(that.mId);

    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
