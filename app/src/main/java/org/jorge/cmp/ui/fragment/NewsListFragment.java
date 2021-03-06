/*
 * This file is part of LoLin1.
 *
 * LoLin1 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LoLin1 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LoLin1. If not, see <http://www.gnu.org/licenses/>.
 *
 * Created by Jorge Antonio Diaz-Benito Soriano.
 */

package org.jorge.cmp.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;

import org.jorge.cmp.R;
import org.jorge.cmp.datamodel.Realm;
import org.jorge.cmp.io.database.SQLiteDAO;
import org.jorge.cmp.io.prefs.PreferenceAssistant;

import java.util.Arrays;

public class NewsListFragment extends FeedListFragment {

    public static Fragment newInstance(Context context, Realm accRealm) {
        Bundle args = new Bundle();
        args.putString(FeedListFragment.TAG_KEY, NewsListFragment.class.getName());
        args.putInt(FeedListFragment.ERROR_RES_ID_KEY, R.drawable.feed_article_image_placeholder);
        String accLocale = PreferenceAssistant.readSharedString(context,
                PreferenceAssistant.PREF_LANG, null);
        if (accLocale == null || !Arrays.asList(accRealm.getLocales()).contains(accLocale)) {
            accLocale = accRealm.getLocales()[0];
            PreferenceAssistant.writeSharedString(context, PreferenceAssistant.PREF_LANG,
                    accLocale);
        }
        args.putString(FeedListFragment.TABLE_NAME_KEY, SQLiteDAO.getNewsTableName
                (accRealm, accLocale));
        args.putSerializable(FeedListFragment.LM_KEY, LayoutManagerEnum.GRID);

        return FeedListFragment.instantiate(context, NewsListFragment.class.getName(), args);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        final ActionBar actionBar = mActivity.getSupportActionBar();
        actionBar.setTitle(mActivity.getString(R.string.title_section0));
    }
}
