/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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

    static {
        final Map<CharSequence, CharSequence> escapePowershellMap = new HashMap<>();
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
        ESCAPE_POWERSHELL = new LookupTranslator(
            Collections.unmodifiableMap(escapePowershellMap)
        );
    }

    public static String escapePowerShell(final String input) {
        return ESCAPE_POWERSHELL.translate(input);
    }
}
