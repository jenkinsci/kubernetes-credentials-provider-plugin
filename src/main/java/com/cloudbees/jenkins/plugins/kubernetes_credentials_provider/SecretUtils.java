/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Optional;

import com.cloudbees.plugins.credentials.CredentialsScope;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Secret;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Collection of utilities for working with {@link Secret}s.
 * <em>Note</em>: API may be subject to change.
 */
public abstract class SecretUtils {

    private static final Logger LOG = Logger.getLogger(SecretUtils.class.getName());

    /** Optional Kubernetes annotation for the credential description */
    private static final String JENKINS_IO_CREDENTIALS_DESCRIPTION_ANNOTATION = "jenkins.io/credentials-description";

    /** Annotation prefix for the optional custom mapping of data */
    private static final String JENKINS_IO_CREDENTIALS_KEYBINDING_ANNOTATION_PREFIX = "jenkins.io/credentials-keybinding-";

    static final String JENKINS_IO_CREDENTIALS_ID_LABEL = "jenkins.io/credentials-id";

    static final String JENKINS_IO_CREDENTIALS_TYPE_LABEL = "jenkins.io/credentials-type";

    static final String JENKINS_IO_CREDENTIALS_SCOPE_LABEL = "jenkins.io/credentials-scope";


    /**
     * Convert a String representation of the base64 encoded bytes of a UTF-8 String back to a String. 
     * @param s the base64 encoded String representation of the bytes.
     * @return the String or {@code null} if the string could not be converted.
     */
    @CheckForNull
    @Restricted(NoExternalUse.class) // API is not yet concrete
    public static String base64DecodeToString(String s) {
        byte[] bytes = base64Decode(s);
        if (bytes != null) {
            try {
                CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPORT);
                decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer decode = decoder.decode(ByteBuffer.wrap(bytes));
                return decode.toString();
            } catch (CharacterCodingException ex) {
                LOG.log(Level.WARNING, "failed to covert Secret, is this a valid UTF-8 string?  {0}", ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Convert a String representation of the base64 encoded bytes back to a byte[]. 
     * @param s the base64 encoded representation of the bytes.
     * @return the byte[] or {@code null} if the string could not be converted.
     */
    @CheckForNull
    @Restricted(NoExternalUse.class) // API is not yet concrete
    public static byte[] base64Decode(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.WARNING, "failed to base64decode Secret, is the format valid?  {0}", ex.getMessage());
        }
        return null;
    }

    /**
     * Get the scope from a given {@code Secret}.
     * If the label is empty, then it defaults to global scope.
     * @param s the secret whose scope we want to obtain.
     * @return the scope for a given secret.
     * @throws CredentialsConvertionException if scope is invalid.
     */
    public static CredentialsScope getCredentialScope(Secret s) throws CredentialsConvertionException {
        CredentialsScope scope = CredentialsScope.GLOBAL;
        String label = s.getMetadata().getLabels().get(JENKINS_IO_CREDENTIALS_SCOPE_LABEL);
        if (label != null) {
            try {
                scope = CredentialsScope.valueOf(label.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new CredentialsConvertionException(JENKINS_IO_CREDENTIALS_SCOPE_LABEL + " is set to an invalid scope: " + label, exception);
            }
        }
        return scope;
    }

    /**
     * Obtain the credential ID from a given {@code Secret}.
     * @param s the secret whose id we want to obtain.
     * @return the credential ID for a given secret.
     */
    public static String getCredentialId(Secret s) {
        Map<String, String> labels = s.getMetadata().getLabels();
        if (labels != null) {
            String overrideId = labels.get(JENKINS_IO_CREDENTIALS_ID_LABEL);
            if (overrideId != null) {
                return overrideId;
            }
        }
        // we must have a metadata as the label that identifies this as a Jenkins credential needs to be present
        return s.getMetadata().getName();
    }

    /**
     * Obtain the credential description from a given {@code Secret}.
     * @param s the secret whose description we want to obtain.
     * @return the credential description for a given secret.
     */
    @CheckForNull
    public static String getCredentialDescription(Secret s) {
        // we must have a metadata as the label that identifies this as a Jenkins credential needs to be present
        Map<String, String> annotations = s.getMetadata().getAnnotations();
        if (annotations != null) {
            return annotations.get(JENKINS_IO_CREDENTIALS_DESCRIPTION_ANNOTATION);
        }
        return null;
    }

    /**
     * Checks that {@code obj} is not {@code null}.
     * @param obj the Object to check for {@code null}.
     * @param exceptionMessage detail message to be used in the event that a CredentialsConvertionException is thrown.
     * @param <T> the type of the obj.
     * @return {@code obj} if not {@code null}.
     * @throws CredentialsConvertionException iff {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(@Nullable T obj, String exceptionMessage) throws CredentialsConvertionException {
        if (obj == null) {
            throw new CredentialsConvertionException(exceptionMessage);
        }
        return obj;
    }

    /**
     * Checks that {@code obj} is not {@code null}.
     * @param obj the Object to check for {@code null}.
     * @param exceptionMessage detail message to be used in the event that a CredentialsConvertionException is thrown.
     * @param mapped an optional mapping (adds a {@code "mapped to " + mapped} to the exception message if this is non null.
     * @param <T> the type of the obj.
     * @return {@code obj} if not {@code null}.
     * @throws CredentialsConvertionException iff {@code obj} is {@code null}.
     */
    public static <T> T requireNonNull(@Nullable T obj, String exceptionMessage, @Nullable String mapped) throws CredentialsConvertionException {
        if (obj == null) {
            if (mapped != null) {
                throw new CredentialsConvertionException(exceptionMessage.concat(" (mapped to " + mapped + ")"));
            }
            throw new CredentialsConvertionException(exceptionMessage);
        }
        return obj;
    }


    /**
     * Get the data for the specified key (or the mapped key if key is mapped), or throw a
     * CredentialsConvertionException if the data for the given key was not present..
     *
     * @param s the Secret
     * @param key the key to get the data for (which may be mapped to another key).
     * @param exceptionMessage the detailMessage of the exception if the data for the key (or mapped key) was not
     *            present.
     * @return The data for the given key.
     * @throws CredentialsConvertionException if the data was not present.
     */
    @SuppressFBWarnings(value= {"ES_COMPARING_PARAMETER_STRING_WITH_EQ"}, justification="the string will be the same string if not mapped")
    public static String getNonNullSecretData(Secret s, String key, String exceptionMessage) throws CredentialsConvertionException {
        String mappedKey = getKeyName(s, key);
        Map<String, String> data = requireNonNull(s.getData(), exceptionMessage, mappedKey == key ? null : mappedKey);
        return requireNonNull(data.get(mappedKey), exceptionMessage, mappedKey);
    }

    /**
     * Get optional data for the specified key (or the mapped key if key is mapped)
     *
     * @param s the Secret
     * @param key the key to get the data for (which may be mapped to another key).
     * @param exceptionMessage the detailMessage of the exception if the data for the key (or mapped key) was not
     *            present.
     * @return Optional data for specified key
     * @throws CredentialsConvertionException if the data was not present.
     */
    public static Optional<String> getOptionalSecretData(Secret s, String key, String exceptionMessage) throws CredentialsConvertionException {
        String mappedKey = getKeyName(s, key);
        if (s.getData().containsKey(key) || s.getData().containsKey(mappedKey)) {
            return Optional.of(getNonNullSecretData(s, key, exceptionMessage));
        }
        return Optional.empty();
    }

    /**
     * Get the mapping for the specified key name. Secrets can override the defaults used by the plugin by specifying an
     * attribute of the type {@code jenkins.io/credentials-keybinding-name} containing the custom name - for example
     * {@code jenkins.io/credentials-keybinding-foo=wibble}.
     *
     * @param s the secret to inspect for a custom name.
     * @param key the name of the key we are looking for.
     * @return the custom mapping for the key or {@code key} (identical object) if there is no custom mapping.
     */
    public static String getKeyName(Secret s, String key) {
        Map<String, String> annotations = s.getMetadata().getAnnotations();
        if (annotations != null){
            final String annotationName = JENKINS_IO_CREDENTIALS_KEYBINDING_ANNOTATION_PREFIX + key;
            String customMapping = annotations.get(annotationName);
            if (customMapping == null) {
                // no entry
                return key;
            }
            if (customMapping.isEmpty()){
                LOG.log(Level.WARNING, "Secret {0} contains a mapping annotation {1} but has no entry - mapping will "
                                       + "not be performed",
                        new Object[] {s.getMetadata().getName(), annotationName});
                return key;
            }
            return customMapping;
        }
        return key;
    }
}
