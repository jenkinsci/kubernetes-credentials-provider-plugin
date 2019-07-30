package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;

/**
 * Parser for <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">Kubernetes Label Selectors</a>.
 */
class LabelSelectorExpressions {

    /**
     * Parse Kubernetes label selector expression.
     * Example:
     * <pre>
     * app in (jenkins-dev, jenkins-all)
     * </pre>
     * @param selector label selector expression or null
     * @return parsed label selector
     * @throws LabelSelectorParseException when invalid selector expression
     */
    static LabelSelector parse(@Nullable String selector) throws LabelSelectorParseException {
        LabelSelectorBuilder lsb = new LabelSelectorBuilder();
        if (selector != null && !selector.trim().isEmpty()) {
            Matcher matcher = Pattern.compile("[^,]+?\\([^()]+\\)|[^,]+").matcher(selector);
            while (matcher.find()) {
                String expression = matcher.group();
                expression = expression.replaceFirst("!=|=", " $0 ");
                String[] tokens = expression.trim().split("\\s+", 3);
                switch (tokens.length) {
                    case 1:
                        if (tokens[0].startsWith("!")) {
                            lsb.addNewMatchExpression()
                                    .withKey(tokens[0].substring(1))
                                    .withOperator("DoesNotExist")
                                    .endMatchExpression();
                        } else {
                            lsb.addNewMatchExpression()
                                    .withKey(tokens[0])
                                    .withOperator("Exists")
                                    .endMatchExpression();
                        }
                        break;
                    case 3:
                        String operator = tokens[1];
                        if ("=".equals(operator)) {
                            lsb.addToMatchLabels(tokens[0], tokens[2]);
                        } else if ("!=".equals(operator)) {
                            lsb.addNewMatchExpression()
                                    .withKey(tokens[0])
                                    .withOperator("NotIn")
                                    .withValues(tokens[2])
                                    .endMatchExpression();
                        } else if ("notin".equalsIgnoreCase(operator)) {
                            lsb.addNewMatchExpression()
                                    .withKey(tokens[0])
                                    .withOperator("NotIn")
                                    .withValues(values(tokens[2]))
                                    .endMatchExpression();
                        } else if ("in".equalsIgnoreCase(operator)) {
                            lsb.addNewMatchExpression()
                                    .withKey(tokens[0])
                                    .withOperator("In")
                                    .withValues(values(tokens[2]))
                                    .endMatchExpression();
                        } else {
                            throw new LabelSelectorParseException("Unrecognized selector operator '" + operator + "' in expression '" + expression + "'. Expected one of: =, !=, in, notin");
                        }
                        break;
                    default:
                        throw new LabelSelectorParseException("Invalid selector expression '" + expression + "'. Expected 1 or 3 tokens, got " + tokens.length + ": " + Arrays.toString(tokens));
                }
            }
        }
        return lsb.build();
    }

    private static String[] values(String list) {
        return list.replaceAll("\\(|\\)", "").trim().split("\\s*,\\s*");
    }
}
