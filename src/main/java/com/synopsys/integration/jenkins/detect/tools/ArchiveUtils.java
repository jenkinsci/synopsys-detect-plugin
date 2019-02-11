/**
 * blackduck-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.jenkins.detect.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;

public class ArchiveUtils {
    public static void unzip(final File zipFile) throws IOException {
        final File unzipDirectory = new File(zipFile.getParentFile(), getUnzippedDirectoryName(zipFile.getName()));
        unzip(unzipDirectory, zipFile);
    }

    public static void unzip(File dir, final File zipFile) throws IOException {
        // without getAbsoluteFile, getParentFile below seems to fail
        dir = dir.getAbsoluteFile();
        final ZipFile zip = new ZipFile(zipFile);
        final Enumeration<? extends ZipEntry> entries = zip.entries();
        try {
            while (entries.hasMoreElements()) {
                final ZipEntry e = entries.nextElement();
                final File f = new File(dir, e.getName());
                if (e.isDirectory()) {
                    f.mkdirs();
                } else {
                    final File p = f.getParentFile();
                    if (p != null) {
                        p.mkdirs();
                    }
                    try (final InputStream input = zip.getInputStream(e)) {
                        copyInputStreamToFile(input, f);
                    }
                    f.setLastModified(e.getTime());
                }
            }
        } finally {
            zip.close();
        }
    }

    public static String getUnzippedDirectoryName(final String zipFileName) {
        return zipFileName.substring(zipFileName.lastIndexOf('/') + 1, zipFileName.lastIndexOf('.'));
    }

    private static void copyInputStreamToFile(final InputStream in, final File f) throws IOException {
        final FileOutputStream fos = new FileOutputStream(f);
        try {
            IOUtils.copy(in, fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

}
