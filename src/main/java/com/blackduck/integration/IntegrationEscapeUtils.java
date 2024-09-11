/*
 * blackduck-detect
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class IntegrationEscapeUtils extends StringEscapeUtils {
    public static final CharSequenceTranslator ESCAPE_POWERSHELL;

    static {
        Map<CharSequence, CharSequence> escapePowershellMap = new HashMap<>();
        escapePowershellMap.put("|", "`|");
        escapePowershellMap.put("&", "`&");
        escapePowershellMap.put(";", "`;");
        escapePowershellMap.put("<", "`<");
        escapePowershellMap.put(">", "`>");
        escapePowershellMap.put("(", "`(");
        escapePowershellMap.put(")", "`)");
        escapePowershellMap.put("$", "`$");
        escapePowershellMap.put("`", "``");
        escapePowershellMap.put("\\", "`\\");
        escapePowershellMap.put("\"", "`\"");
        escapePowershellMap.put("'", "`'");
        escapePowershellMap.put(" ", "` ");
        escapePowershellMap.put("\t", "`\t");
        escapePowershellMap.put("\r\n", "");
        escapePowershellMap.put("\n", "");
        escapePowershellMap.put("*", "`*");
        escapePowershellMap.put("?", "`?");
        escapePowershellMap.put("[", "`[");
        escapePowershellMap.put("#", "`#");
        escapePowershellMap.put("~", "`~");
        escapePowershellMap.put("=", "`=");
        escapePowershellMap.put("%", "`%");
        escapePowershellMap.put(",", "`,");
        ESCAPE_POWERSHELL = new LookupTranslator(
            Collections.unmodifiableMap(escapePowershellMap)
        );
    }

    public static String escapePowerShell(String input) {
        return ESCAPE_POWERSHELL.translate(input);
    }
}
