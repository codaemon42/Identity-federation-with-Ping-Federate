/*
 * **************************************************
 *  Copyright (C) 2019 Ping Identity Corporation
 *  All rights reserved.
 *
 *  The contents of this file are the property of Ping Identity Corporation.
 *  You may not copy or use this file, in either source code or executable
 *  form, except in compliance with terms set by Ping Identity Corporation.
 *  For further information please contact:
 *
 *  Ping Identity Corporation
 *  1001 17th St Suite 100
 *  Denver, CO 80202
 *  303.468.2900
 *  http://www.pingidentity.com
 * ****************************************************
 */

package com.pingidentity.oob;


import org.apache.commons.lang.HtmlCharEscape;

public final class Utils
{
    private Utils()
    {
        // hidden constructor
    }

    static String escapeForHtml(String s)
    {
        if (s == null)
        {
            return null;
        }
        return HtmlCharEscape.HTML_CHAR_ESCAPE.escape(s);
    }
}
