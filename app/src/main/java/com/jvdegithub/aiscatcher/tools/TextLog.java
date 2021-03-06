/*
 *     AIS-catcher for Android
 *     Copyright (C)  2022 jvde.github@gmail.com.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.jvdegithub.aiscatcher.tools;

public class TextLog {

    private String[] text = new String[]{"", "",};
    private int active = 0;
    private final int MaxCharacters = 100000;

    public void Update(String str) {
        synchronized (text)
        {
            text[active] = text[active] + str;
            if (text[active].length() > MaxCharacters) {
                active = 1 - active;
                text[active] = "";
            }
        }
    }

    public String getText() {
        synchronized(text)
        {
            return text[1 - active] + text[active];
        }
    }

    public void Clear() {
        synchronized(text)
        {
            text[0] = text[1] = "";
            active = 0;
        }
    }

}