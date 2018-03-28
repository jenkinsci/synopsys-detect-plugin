// CHECKSTYLE:OFF

package com.blackducksoftware.integration.detect.jenkins;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

@SuppressWarnings({
    "",
    "PMD"
})
public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    /**
     * Black Duck Hub Detect
     * 
     */
    public static String DetectPipelineStep_getDisplayName() {
        return holder.format("DetectPipelineStep_getDisplayName");
    }

    /**
     * Black Duck Hub Detect
     * 
     */
    public static Localizable _DetectPipelineStep_getDisplayName() {
        return new Localizable(holder, "DetectPipelineStep_getDisplayName");
    }

    /**
     * Please set a value for the connection timeout.
     * 
     */
    public static String DetectPostBuildStep_getPleaseSetTimeout() {
        return holder.format("DetectPostBuildStep_getPleaseSetTimeout");
    }

    /**
     * Please set a value for the connection timeout.
     * 
     */
    public static Localizable _DetectPostBuildStep_getPleaseSetTimeout() {
        return new Localizable(holder, "DetectPostBuildStep_getPleaseSetTimeout");
    }

    /**
     * Please specify the credentials or the Api token.
     * 
     */
    public static String DetectPostBuildStep_getPleaseSetHubCredentials() {
        return holder.format("DetectPostBuildStep_getPleaseSetHubCredentials");
    }

    /**
     * Please specify the credentials or the Api token.
     * 
     */
    public static Localizable _DetectPostBuildStep_getPleaseSetHubCredentials() {
        return new Localizable(holder, "DetectPostBuildStep_getPleaseSetHubCredentials");
    }

    /**
     * Please set a Server URL.
     * 
     */
    public static String DetectPostBuildStep_getPleaseSetServerUrl() {
        return holder.format("DetectPostBuildStep_getPleaseSetServerUrl");
    }

    /**
     * Please set a Server URL.
     * 
     */
    public static Localizable _DetectPostBuildStep_getPleaseSetServerUrl() {
        return new Localizable(holder, "DetectPostBuildStep_getPleaseSetServerUrl");
    }

    /**
     * Black Duck Hub Detect
     * 
     */
    public static String DetectPostBuildStep_getDisplayName() {
        return holder.format("DetectPostBuildStep_getDisplayName");
    }

    /**
     * Black Duck Hub Detect
     * 
     */
    public static Localizable _DetectPostBuildStep_getDisplayName() {
        return new Localizable(holder, "DetectPostBuildStep_getDisplayName");
    }

    /**
     * Can not reach this server : {0}
     * 
     */
    public static String DetectPostBuildStep_getCanNotReachThisServer_0_(Object arg1) {
        return holder.format("DetectPostBuildStep_getCanNotReachThisServer_0_", arg1);
    }

    /**
     * Can not reach this server : {0}
     * 
     */
    public static Localizable _DetectPostBuildStep_getCanNotReachThisServer_0_(Object arg1) {
        return new Localizable(holder, "DetectPostBuildStep_getCanNotReachThisServer_0_", arg1);
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static String DetectPostBuildStep_getCredentialsValidFor_0_(Object arg1) {
        return holder.format("DetectPostBuildStep_getCredentialsValidFor_0_", arg1);
    }

    /**
     * Credentials valid for: {0}
     * 
     */
    public static Localizable _DetectPostBuildStep_getCredentialsValidFor_0_(Object arg1) {
        return new Localizable(holder, "DetectPostBuildStep_getCredentialsValidFor_0_", arg1);
    }

}
