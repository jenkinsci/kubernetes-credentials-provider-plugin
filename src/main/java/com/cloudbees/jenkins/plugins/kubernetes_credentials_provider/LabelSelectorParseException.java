package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

/**
 * Exception thrown for label selector parsing errors.
 */
class LabelSelectorParseException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Create new label parse exception with no root cause.
     * @param message exception message
     */
    LabelSelectorParseException(String message) {
        super(message);
    }
}
