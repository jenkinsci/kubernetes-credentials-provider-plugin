package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import static org.junit.Assert.*;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import org.junit.Test;

public class LabelSelectorExpressionsTest {

    @Test
    public void parse() {
        LabelSelector selector = LabelSelectorExpressions.parse("partition  in  (customerA, customerB),environment!=qa,!foo,bar,color=blue,owner in ( john , mary )");

        LabelSelector expected = new LabelSelectorBuilder()
                .addToMatchLabels("color", "blue")
                .addNewMatchExpression()
                    .withKey("partition")
                    .withOperator("In")
                    .withValues("customerA", "customerB")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("environment")
                    .withOperator("NotIn")
                    .withValues("qa")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("foo")
                    .withOperator("DoesNotExist")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("bar")
                    .withOperator("Exists")
                    .endMatchExpression()
                .addNewMatchExpression()
                    .withKey("owner")
                    .withOperator("In")
                    .withValues("john", "mary")
                    .endMatchExpression()
                .build();

        assertEquals(expected, selector);
    }
}