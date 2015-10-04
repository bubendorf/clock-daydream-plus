/*package ca.mlaflamme.clocktime.preference;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class AdmobPreference extends Preference
{
    private static final String ADSNS = "http://schemas.android.com/apk/res-auto";
    private static final String CLOCKNS = "http://schemas.android.com/apk/lib/ca.mlaflamme.clocktime";

    private String mAdUnitId;

    public AdmobPreference(Context context) {
        super(context, null);
    }

    public AdmobPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setValuesFromXml(attrs);
    }

    private void setValuesFromXml(AttributeSet attrs) {
        mAdUnitId = getAttributeStringValue(attrs, CLOCKNS, "adUnitIdString", "");
    }

    private String getAttributeStringValue(AttributeSet attrs, String namespace, String name, String defaultValue) {
        String value = attrs.getAttributeValue(namespace, name);
        if (value == null)
            value = defaultValue;

        return value;
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        // this will create the linear layout defined in ads_layout.xml
        View view = super.onCreateView(parent);

        // Create the adView
        AdView adView = new AdView(getContext());
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(mAdUnitId);

        ((LinearLayout)view).addView(adView);

        // Initiate a generic request to load it with an ad
        AdRequest request = new AdRequest.Builder().build();
        adView.loadAd(request);

        return view;
    }

}
*/