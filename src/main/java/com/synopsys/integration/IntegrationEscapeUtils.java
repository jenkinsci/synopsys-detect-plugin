/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;

public class IntegrationEscapeUtils extends StringEscapeUtils {
    public static final CharSequenceTranslator ESCAPE_POWERSHELL;

    // same as ESCAPE_XSI without escaping Asterisk '*' so wildcards for excluding directories work again like v7
    public static final CharSequenceTranslator ESCAPE_XSI_MINUS_A;

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

    static {
        Map<CharSequence, CharSequence> unescapeJavaMap = new HashMap<>();
        unescapeJavaMap.put("|", "\\|");
        unescapeJavaMap.put("&", "\\&");
        unescapeJavaMap.put(";", "\\;");
        unescapeJavaMap.put("<", "\\<");
        unescapeJavaMap.put(">", "\\>");
        unescapeJavaMap.put("(", "\\(");
        unescapeJavaMap.put(")", "\\)");
        unescapeJavaMap.put("$", "\\$");
        unescapeJavaMap.put("`", "\\`");
        unescapeJavaMap.put("\\", "\\\\");
        unescapeJavaMap.put("\"", "\\\"");
        unescapeJavaMap.put("'", "\\'");
        unescapeJavaMap.put(" ", "\\ ");
        unescapeJavaMap.put("\t", "\\\t");
        unescapeJavaMap.put("\r\n", "");
        unescapeJavaMap.put("\n", "");
        unescapeJavaMap.put("?", "\\?");
        unescapeJavaMap.put("[", "\\[");
        unescapeJavaMap.put("#", "\\#");
        unescapeJavaMap.put("~", "\\~");
        unescapeJavaMap.put("=", "\\=");
        unescapeJavaMap.put("%", "\\%");
        ESCAPE_XSI_MINUS_A = new LookupTranslator(Collections.unmodifiableMap(unescapeJavaMap));
    }

    public static String escapePowerShell(String input) {
        return ESCAPE_POWERSHELL.translate(input);
    }

    public static String escapeXSIMinusAsterisk(String input) {
        return ESCAPE_XSI_MINUS_A.translate(input);
    }
}
