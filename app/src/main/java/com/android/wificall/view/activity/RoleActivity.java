package com.android.wificall.view.activity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.android.wificall.R;
import com.android.wificall.util.PrefsKeys;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.pixplicity.easyprefs.library.Prefs;

import butterknife.OnClick;

/**
 * Created by matviy on 09.09.16.
 */
public class RoleActivity extends BaseActivity implements View.OnTouchListener {

    private ShowcaseView showcaseView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.role_title);
    }

    @OnClick(R.id.speaker)
    public void selectSpeaker() {
        Prefs.putBoolean(PrefsKeys.IS_SPEAKER, true);
        WifiDirectActivity.invoke(this);
    }

    @OnClick(R.id.listener)
    public void selectListener() {
        Prefs.putBoolean(PrefsKeys.IS_SPEAKER, false);
        WifiDirectActivity.invoke(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showFirstTeacher();
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_select_role;
    }

    private void showSecondTeacher() {
        Target viewTarget = new ViewTarget(R.id.listener, this);
        showcaseView = new ShowcaseView.Builder(this)
                .setTarget(viewTarget)
                .withMaterialShowcase()
                .setContentTitle(R.string.showcase_listener_title)
                .setContentText(R.string.showcase_listener_text)
                .singleShot(2)
                .setStyle(R.style.CustomShowcaseTheme2)
                .blockAllTouches()
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        super.onShowcaseViewDidHide(showcaseView);
                    }
                })
                .build();
        showcaseView.hideButton();
        showcaseView.setOnTouchListener(this);
    }

    private void showFirstTeacher() {
        Target viewTarget = new ViewTarget(R.id.speaker, this);
        showcaseView = new ShowcaseView.Builder(this)
                .setTarget(viewTarget)
                .withMaterialShowcase()
                .setContentTitle(R.string.showcase_speaker_title)
                .setContentText(R.string.showcase_speaker_text)
                .setStyle(R.style.CustomShowcaseTheme2)
                .blockAllTouches()
                .singleShot(1)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        super.onShowcaseViewDidHide(showcaseView);
                        showSecondTeacher();
                    }
                })
                .build();
        showcaseView.hideButton();
        showcaseView.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (view instanceof ShowcaseView) {
                ((ShowcaseView) view).hide();
            }
        }
        return true;
    }


}
