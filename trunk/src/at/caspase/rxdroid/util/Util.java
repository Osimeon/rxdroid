/**
 * Copyright (C) 2011 Joseph Lehner <joseph.c.lehner@gmail.com>
 *
 * This file is part of RxDroid.
 *
 * RxDroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RxDroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RxDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package at.caspase.rxdroid.util;

import android.content.Context;
import android.util.AttributeSet;
import at.caspase.rxdroid.Database;
import at.caspase.rxdroid.R;

public final class Util
{
	public static int getDoseTimeDrawableFromDoseViewId(int doseViewId)
	{
		switch(doseViewId)
		{
				case R.id.morning:
						return R.drawable.ic_morning;
				case R.id.noon:
						return R.drawable.ic_noon;
				case R.id.evening:
						return R.drawable.ic_evening;
				case R.id.night:
						return R.drawable.ic_night;
		}

		throw new IllegalArgumentException();
	}

	public static int getDoseTimeFromDoseViewId(int doseViewId)
	{
		switch(doseViewId)
		{
				case R.id.morning:
						return Database.Drug.TIME_MORNING;
				case R.id.noon:
						return Database.Drug.TIME_NOON;
				case R.id.evening:
						return Database.Drug.TIME_EVENING;
				case R.id.night:
						return Database.Drug.TIME_NIGHT;
		}

		throw new IllegalArgumentException();
	}
	
	/**
	 * Obtains a string attribute from an AttributeSet.
	 * 
	 * Note that this function automatically resolves string references.
	 * 
	 * @param context The context.
	 * @param attrs An AttributeSet to query.
	 * @param namespace The attribute's namespace (in the form of <code>http://schemas.android.com/apk/res/&lt;package&gt;</code>
	 * @param attribute The name of the attribute to query.
	 * @param defaultValue A default value, in case there's no such attribute.
	 * @return The attribute's value, or <code>null</code> if it does not exist.
	 */	
	public static String getStringAttribute(Context context, AttributeSet attrs, String namespace, String attribute, String defaultValue)
	{
		int resId = attrs.getAttributeResourceValue(namespace, attribute, -1);
		String value;
		
		if(resId == -1)
			value = attrs.getAttributeValue(namespace, attribute);
		else
			value = context.getString(resId);
		
		return value == null ? defaultValue : value;		
	}
}
