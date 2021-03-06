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

package org.jorge.cmp.util;

import org.jorge.cmp.datamodel.FeedArticle;

/**
 * This is bad practice and should never be done.
 * The reason it's being done here is a bug that causes
 * Android Studio to mistakenly find a cyclic hierarchy.
 */
public final class Interface {

    private Interface() {
        throw new UnsupportedOperationException("DO NOT CALL THIS METHOD!");
    }

    public interface IOnFeedArticleClickedListener {
        public void onFeedArticleClicked(FeedArticle item, Class c);
    }

    public interface IOnBackPressed {

        public Boolean onBackPressed();
    }

    public interface IOnItemInteractionListener {

        void onItemClick(FeedArticle item);
    }
}