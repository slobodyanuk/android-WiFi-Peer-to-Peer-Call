package com.android.wificall.util;

import android.app.Activity;
import android.view.View;

import com.android.wificall.R;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.Target;

/**
 * Created by Serhii Slobodyanuk on 03.10.2016.
 */
public class ShowCaseUtils {

    private ShowcaseView mShowcaseView;

    public void showCaseView(int id, int title, int text, Target target, Activity activity){
        mShowcaseView = new ShowcaseView.Builder(activity)
                .setTarget(target)
                .withMaterialShowcase()
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(R.style.CustomShowcaseTheme2)
                .blockAllTouches()
                  .singleShot(id)
                .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                    @Override
                    public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        super.onShowcaseViewDidHide(showcaseView);
                        ((CaseViewListener) activity).onCaseViewDidHide(id);
                    }
                })
                .build();
        mShowcaseView.hideButton();
        mShowcaseView.setOnTouchListener((View.OnTouchListener) activity);
    }

    public interface CaseViewListener{
        void onCaseViewDidHide(int id);
    }

}
