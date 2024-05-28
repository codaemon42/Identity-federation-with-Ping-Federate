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
package org.apache.commons.lang;

public class HtmlCharEscape extends Entities
{
    private static final String[][] CHAR_ENTITY_ARRAY =
    {
        {"quot", "34"}, // " - double-quote
        {"amp", "38"}, // & - ampersand
        {"lt", "60"}, // < - less-than
        {"gt", "62"}, // > - greater-than
        {"apos", "39"}, // ' - apostrophe aka single quote
    };

    public static final HtmlCharEscape HTML_CHAR_ESCAPE;
    static
    {
        HTML_CHAR_ESCAPE = new HtmlCharEscape();
        HTML_CHAR_ESCAPE.addEntities(CHAR_ENTITY_ARRAY);
    }
}
